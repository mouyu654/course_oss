package com.obe.moduled.controller;

import com.obe.common.Result;
import com.obe.moduled.service.CourseReportService;
import com.obe.moduled.service.MajorReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CourseReportService courseReportService;
    private final MajorReportService majorReportService;

    // ===== 课程级 =====

    @GetMapping("/course/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public Result<Map<String, Object>> getCourseReport(@PathVariable Long classId) {
        return Result.ok(courseReportService.getReportData(classId));
    }

    @GetMapping("/course/{classId}/pdf")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public ResponseEntity<byte[]> downloadCoursePdf(@PathVariable Long classId) {
        byte[] pdf = courseReportService.generatePdf(classId);
        String filename = URLEncoder.encode("课程达成度报告.pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/course/{classId}/excel")
    @PreAuthorize("hasAnyRole('TEACHER','ACADEMIC','DIRECTOR')")
    public ResponseEntity<byte[]> downloadCourseExcel(@PathVariable Long classId) {
        byte[] excel = courseReportService.generateExcel(classId);
        String filename = URLEncoder.encode("课程达成度报告.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    // ===== 专业级计算数据管理 =====

    /** 列出所有计算批次 */
    @GetMapping("/major/batches")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<List<Map<String, Object>>> listBatches(
            @RequestParam(required = false) Integer enrollmentYear,
            @RequestParam(required = false) Long majorId) {
        return Result.ok(majorReportService.listCalcBatches(enrollmentYear, majorId));
    }

    /** 获取某批次的雷达图数据 */
    @GetMapping("/major/data")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Map<String, Object>> getMajorData(
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer enrollmentYear) {
        return Result.ok(majorReportService.getReportData(majorId, enrollmentYear));
    }

    /** 删除某批次的计算结果 */
    @DeleteMapping("/major/data")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public Result<Void> deleteBatch(
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer enrollmentYear) {
        majorReportService.deleteBatch(majorId, enrollmentYear);
        return Result.ok();
    }

    /** 下载穿透式 Excel 台账 */
    @GetMapping("/major/excel")
    @PreAuthorize("hasAnyRole('DIRECTOR','ACADEMIC')")
    public ResponseEntity<byte[]> downloadMajorExcel(
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer enrollmentYear) {
        byte[] excel = majorReportService.generateTraceExcel(majorId, enrollmentYear);
        String suffix = (enrollmentYear != null ? enrollmentYear + "级" : "") + "专业达成度台账.xlsx";
        String filename = URLEncoder.encode(suffix, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
