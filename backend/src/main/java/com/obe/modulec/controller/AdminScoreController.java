package com.obe.modulec.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.Result;
import com.obe.modulea.entity.TeachingClass;
import com.obe.modulea.entity.Student;
import com.obe.modulea.entity.Course;
import com.obe.modulea.entity.SysUser;
import com.obe.modulea.mapper.StudentMapper;
import com.obe.modulea.mapper.CourseMapper;
import com.obe.modulea.mapper.SysUserMapper;
import com.obe.modulea.mapper.TeachingClassMapper;
import com.obe.moduleb.entity.AssessmentPoint;
import com.obe.moduleb.entity.CourseOutline;
import com.obe.moduleb.mapper.AssessmentPointMapper;
import com.obe.moduleb.mapper.CourseOutlineMapper;
import com.obe.modulec.entity.ScoreSheet;
import com.obe.modulec.entity.StudentScore;
import com.obe.modulec.mapper.StudentScoreMapper;
import com.obe.modulec.entity.ScoreUnlockRequest;
import com.obe.modulec.mapper.ScoreSheetMapper;
import com.obe.modulec.service.CourseCalcService;
import com.obe.modulec.service.UnlockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminScoreController {

    private final ScoreSheetMapper scoreSheetMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final UnlockRequestService unlockRequestService;
    private final CourseCalcService courseCalcService;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final CourseOutlineMapper courseOutlineMapper;
    private final AssessmentPointMapper assessmentPointMapper;

    private String currentRoleCode() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(GrantedAuthority::getAuthority)
                .orElse("").replace("ROLE_", "");
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Emergency direct unlock — bypasses work order flow (ADMIN only) */
    @PostMapping("/scores/{sheetId}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Void> directUnlock(@PathVariable Long sheetId) {
        courseCalcService.unlockSheet(sheetId);
        return Result.ok();
    }

    @GetMapping("/scores")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<List<Map<String, Object>>> listSheets() {
        List<ScoreSheet> sheets = scoreSheetMapper.selectList(
                new LambdaQueryWrapper<ScoreSheet>().orderByDesc(ScoreSheet::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScoreSheet s : sheets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("classId", s.getClassId());
            item.put("status", s.getStatus());
            item.put("lockedAt", s.getLockedAt());
            item.put("lockedBy", s.getLockedBy());
            TeachingClass tc = teachingClassMapper.selectById(s.getClassId());
            if (tc != null) {
                item.put("className", tc.getClassName());
                Course course = courseMapper.selectById(tc.getCourseId());
                if (course != null) item.put("courseName", course.getName());
                SysUser teacher = userMapper.selectById(tc.getTeacherId());
                if (teacher != null) item.put("teacherName", teacher.getRealName());
                item.put("semesterId", tc.getSemesterId());
            } else {
                item.put("className", String.valueOf(s.getClassId()));
            }
            result.add(item);
        }
        return Result.ok(result);
    }

    @GetMapping("/unlock-requests")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<List<ScoreUnlockRequest>> listRequests() {
        return Result.ok(unlockRequestService.listRequestsForRole(currentRoleCode()));
    }

    /** Academic: first-level review — agree correction is needed */
    @PostMapping("/unlock-requests/{id}/approve")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> approveRequest(@PathVariable Long id) {
        unlockRequestService.approveRequest(id, currentUserId());
        return Result.ok();
    }

    /** Academic: final review — unlock the sheet */
    @PostMapping("/unlock-requests/{id}/unlock")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> unlockApprovedRequest(@PathVariable Long id) {
        unlockRequestService.unlockApprovedRequest(id, currentUserId());
        return Result.ok();
    }

    /** Academic: reject request */
    @PostMapping("/unlock-requests/{id}/reject")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> rejectRequest(@PathVariable Long id) {
        unlockRequestService.rejectRequest(id, currentUserId(), currentRoleCode());
        return Result.ok();
    }

    /** Get detailed student scores for a teaching class */
    @GetMapping("/scores/{classId}/details")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<Map<String, Object>> getScoreDetails(@PathVariable Long classId) {
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null) {
            return Result.ok(Map.of("headers", List.of(), "rows", List.of()));
        }

        // Get assessment points via outline
        CourseOutline outline = courseOutlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        List<AssessmentPoint> assessments = outline != null
                ? assessmentPointMapper.selectList(
                    new LambdaQueryWrapper<AssessmentPoint>()
                            .eq(AssessmentPoint::getOutlineId, outline.getId())
                            .orderByAsc(AssessmentPoint::getSortOrder))
                : List.of();

        // Build assessment headers
        List<Map<String, Object>> headers = new ArrayList<>();
        for (AssessmentPoint ap : assessments) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", ap.getId());
            h.put("name", ap.getName());
            h.put("maxScore", ap.getMaxScore());
            headers.add(h);
        }

        // Get student scores
        List<StudentScore> scores = studentScoreMapper.selectList(
                new LambdaQueryWrapper<StudentScore>().eq(StudentScore::getSheetId, sheet.getId()));

        // Group by student
        Map<Long, List<StudentScore>> byStudent = scores.stream()
                .collect(java.util.stream.Collectors.groupingBy(StudentScore::getStudentId));

        // Build rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<StudentScore>> entry : byStudent.entrySet()) {
            Student student = studentMapper.selectById(entry.getKey());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", entry.getKey());
            row.put("studentNo", student != null ? student.getStudentNo() : "");
            row.put("studentName", student != null ? student.getName() : "");

            // Score map: assessmentId -> score
            Map<Long, BigDecimal> scoreMap = new LinkedHashMap<>();
            for (StudentScore ss : entry.getValue()) {
                scoreMap.merge(ss.getAssessmentId(), ss.getScore(), BigDecimal::add);
            }
            row.put("scores", scoreMap);
            rows.add(row);
        }

        // Sort by studentNo
        rows.sort(Comparator.comparing(r -> (String) r.get("studentNo")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("headers", headers);
        result.put("rows", rows);
        return Result.ok(result);
    }

}