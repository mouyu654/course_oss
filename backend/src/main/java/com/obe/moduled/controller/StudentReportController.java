package com.obe.moduled.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.BizException;
import com.obe.common.Result;
import com.obe.modulea.entity.Student;
import com.obe.modulea.mapper.StudentMapper;
import com.obe.moduled.service.StudentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports/student")
@RequiredArgsConstructor
public class StudentReportController {

    private final StudentReportService studentReportService;
    private final StudentMapper studentMapper;

    @GetMapping("/by-no/{studentNo}")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR','ADMIN')")
    public Result<StudentReportService.StudentAchievementData> getReportByStudentNo(@PathVariable String studentNo) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo));
        if (student == null) {
            throw new BizException(404, "未找到学号为 " + studentNo + " 的学生");
        }
        return Result.ok(studentReportService.getStudentAchievement(student.getId()));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ACADEMIC','DIRECTOR','ADMIN')")
    public Result<StudentReportService.StudentAchievementData> getStudentReport(@PathVariable Long studentId) {
        return Result.ok(studentReportService.getStudentAchievement(studentId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudentReportService.StudentAchievementData> getMyReport() {
        Long studentId = getCurrentStudentId();
        return Result.ok(studentReportService.getStudentAchievement(studentId));
    }

    @GetMapping("/{studentId}/trace/{indicatorId}")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR','ADMIN')")
    public Result<StudentReportService.IndicatorTraceData> getTrace(
            @PathVariable Long studentId, @PathVariable Long indicatorId) {
        return Result.ok(studentReportService.getIndicatorTrace(studentId, indicatorId));
    }

    @GetMapping("/{studentId}/excel")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ACADEMIC','DIRECTOR','ADMIN')")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long studentId) {
        byte[] excel = studentReportService.generateExcel(studentId);
        String filename = URLEncoder.encode("学生个人达成度报告.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/{studentId}/word")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ACADEMIC','DIRECTOR','ADMIN')")
    public ResponseEntity<byte[]> downloadWord(@PathVariable Long studentId) {
        byte[] word = studentReportService.generateWord(studentId);
        String filename = URLEncoder.encode("学生个人达成度报告.docx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(word);
    }

    private Long getCurrentStudentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Student student = studentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, username));
        if (student == null) {
            throw new com.obe.common.BizException(404, "未找到学生记录");
        }
        return student.getId();
    }
}
