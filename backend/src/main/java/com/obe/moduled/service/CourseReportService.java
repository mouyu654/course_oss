package com.obe.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.BizException;
import com.obe.modulea.entity.Course;
import com.obe.modulea.entity.TeachingClass;
import com.obe.modulea.mapper.CourseMapper;
import com.obe.modulea.mapper.TeachingClassMapper;
import com.obe.moduleb.entity.CourseObjective;
import com.obe.moduleb.entity.CourseOutline;
import com.obe.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.moduleb.mapper.CourseObjectiveMapper;
import com.obe.moduleb.mapper.CourseOutlineMapper;
import com.obe.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import com.obe.modulec.entity.CourseAchievement;
import com.obe.modulec.entity.ObjAchievement;
import com.obe.modulec.mapper.CourseAchievementMapper;
import com.obe.modulec.mapper.ObjAchievementMapper;
import com.obe.modulec.service.ScoreService;
import com.obe.moduled.exporter.PdfExporter;
import com.obe.moduled.exporter.TraceExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseReportService {

    private final ObjAchievementMapper objAchievementMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final ScoreService scoreService;
    private final ObjectiveIndicatorWeightMapper weightMapper;

    /**
     * Build course-level report data.
     */
    public Map<String, Object> getReportData(Long classId) {
        Map<String, Object> data = new LinkedHashMap<>();

        TeachingClass tc = teachingClassMapper.selectById(classId);
        if (tc == null) throw new BizException("教学班级不存在");

        Course course = courseMapper.selectById(tc.getCourseId());
        data.put("courseName", course != null ? course.getName() : "");
        data.put("className", tc.getClassName());

        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));

        // Objective achievements
        List<ObjAchievement> objList = objAchievementMapper.selectList(
                new LambdaQueryWrapper<ObjAchievement>().eq(ObjAchievement::getClassId, classId));

        Map<String, BigDecimal> objectiveMap = new LinkedHashMap<>();
        if (outline != null) {
            List<CourseObjective> objectives = objectiveMapper.selectList(
                    new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
            for (CourseObjective obj : objectives) {
                BigDecimal val = objList.stream()
                        .filter(o -> o.getObjectiveId().equals(obj.getId()))
                        .findFirst()
                        .map(ObjAchievement::getAchievement)
                        .orElse(null);
                objectiveMap.put(obj.getObjNo(), val);
            }
        }
        data.put("objectiveResults", objectiveMap);

        // Course indicator achievements
        List<CourseAchievement> courseList = courseAchievementMapper.selectList(
                new LambdaQueryWrapper<CourseAchievement>().eq(CourseAchievement::getClassId, classId));
        Map<Long, BigDecimal> indicatorMap = new LinkedHashMap<>();
        LocalDateTime calcTime = null;
        for (CourseAchievement ca : courseList) {
            indicatorMap.put(ca.getIndicatorId(), ca.getAchievement());
            if (calcTime == null) calcTime = ca.getCalcTime();
        }
        data.put("indicatorResults", indicatorMap);
        data.put("calcTime", calcTime);

        // Score preview
        data.put("scorePreview", scoreService.getScorePreview(classId));

        return data;
    }

    /** Generate course-level PDF report */
    public byte[] generatePdf(Long classId) {
        Map<String, Object> data = getReportData(classId);

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> objResults = (Map<String, BigDecimal>) data.get("objectiveResults");
        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> indResults = (Map<Long, BigDecimal>) data.get("indicatorResults");

        Map<String, BigDecimal> indStrMap = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : indResults.entrySet()) {
            indStrMap.put(String.valueOf(e.getKey()), e.getValue());
        }

        return PdfExporter.generateCourseReport(
                (String) data.get("courseName"),
                (String) data.get("className"),
                objResults,
                indStrMap,
                (LocalDateTime) data.get("calcTime"));
    }

    /** Generate course-level Excel workbook with comprehensive detail */
    public byte[] generateExcel(Long classId) {
        Map<String, Object> data = getReportData(classId);

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> objResults = (Map<String, BigDecimal>) data.get("objectiveResults");
        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> indResults = (Map<Long, BigDecimal>) data.get("indicatorResults");

        // Build assessment info
        ScoreService.ScorePreview preview = scoreService.getScorePreview(classId);
        java.util.List<TraceExcelExporter.AssessmentInfo> assessments = new java.util.ArrayList<>();
        if (preview != null && preview.headers() != null) {
            for (ScoreService.AssessmentHeader h : preview.headers()) {
                assessments.add(new TraceExcelExporter.AssessmentInfo(
                        h.id(), h.name(), h.maxScore().doubleValue()));
            }
        }

        // Build student scores: skip assessment-level score, sum question-level scores
        java.util.List<TraceExcelExporter.StudentScoreRow> studentRows = new java.util.ArrayList<>();
        if (preview != null && preview.rows() != null) {
            for (ScoreService.ScoreRow row : preview.rows()) {
                java.util.Map<Long, Double> scores = new java.util.LinkedHashMap<>();
                for (ScoreService.ScoreCell cell : row.cells()) {
                    double val = 0;
                    if (cell.score() != null) {
                        val = cell.score().doubleValue();
                    } else if (cell.questionScores() != null && !cell.questionScores().isEmpty()) {
                        val = cell.questionScores().values().stream()
                                .mapToDouble(java.math.BigDecimal::doubleValue).sum();
                    } else {
                        scores.put(cell.assessmentId(), null);
                        continue;
                    }
                    scores.put(cell.assessmentId(), Math.round(val * 100.0) / 100.0);
                }
                studentRows.add(new TraceExcelExporter.StudentScoreRow(
                        row.studentNo(), row.studentName(), scores));
            }
        }

        // Build objective results with descriptions
        java.util.List<TraceExcelExporter.ObjectiveResult> objList = new java.util.ArrayList<>();
        if (objResults != null) {
            TeachingClass tc = teachingClassMapper.selectById(classId);
            CourseOutline outline = null;
            if (tc != null) {
                outline = outlineMapper.selectOne(
                        new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
            }
            java.util.Map<String, String> objDescMap = new java.util.LinkedHashMap<>();
            if (outline != null) {
                java.util.List<CourseObjective> objectives = objectiveMapper.selectList(
                        new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
                for (CourseObjective obj : objectives) {
                    objDescMap.put(obj.getObjNo(), obj.getDescription());
                }
            }
            for (Map.Entry<String, BigDecimal> e : objResults.entrySet()) {
                objList.add(new TraceExcelExporter.ObjectiveResult(
                        e.getKey(),
                        objDescMap.getOrDefault(e.getKey(), ""),
                        e.getValue()));
            }
        }

        // Build indicator results with related objectives
        java.util.List<TraceExcelExporter.IndicatorResult> indList = new java.util.ArrayList<>();
        if (indResults != null) {
            // Find related objectives for each indicator
            java.util.Map<Long, java.util.Set<String>> indicatorObjectives = new java.util.LinkedHashMap<>();
            TeachingClass tc = teachingClassMapper.selectById(classId);
            if (tc != null) {
                CourseOutline outline = outlineMapper.selectOne(
                        new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
                if (outline != null) {
                    java.util.List<CourseObjective> objectives = objectiveMapper.selectList(
                            new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
                    for (CourseObjective obj : objectives) {
                        java.util.List<ObjectiveIndicatorWeight> weights = weightMapper.selectList(
                                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                                        .eq(ObjectiveIndicatorWeight::getObjectiveId, obj.getId()));
                        for (ObjectiveIndicatorWeight w : weights) {
                            indicatorObjectives
                                    .computeIfAbsent(w.getIndicatorId(), k -> new java.util.LinkedHashSet<>())
                                    .add(obj.getObjNo());
                        }
                    }
                }
            }

            for (Map.Entry<Long, BigDecimal> e : indResults.entrySet()) {
                java.util.Set<String> objs = indicatorObjectives.get(e.getKey());
                String related = objs != null ? String.join(", ", objs) : "";
                indList.add(new TraceExcelExporter.IndicatorResult(
                        "指标点" + e.getKey(), e.getValue(), related));
            }
        }

        return TraceExcelExporter.generateCourseDetailReport(
                new TraceExcelExporter.CourseReportData(
                        (String) data.get("courseName"),
                        (String) data.get("className"),
                        assessments, studentRows, objList, indList));
    }
}
