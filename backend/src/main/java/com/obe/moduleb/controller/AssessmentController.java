package com.obe.moduleb.controller;

import com.obe.common.Result;
import com.obe.moduleb.entity.AssessmentPoint;
import com.obe.moduleb.service.AssessmentService;
import com.obe.moduleb.service.TeacherConfigImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final TeacherConfigImportService importService;

    @GetMapping
    public Result<List<AssessmentPoint>> list(@PathVariable Long classId) {
        return Result.ok(assessmentService.listAssessments(classId));
    }

    @PostMapping
    public Result<AssessmentPoint> create(@PathVariable Long classId,
                                          @RequestBody AssessmentPoint point) {
        return Result.ok(assessmentService.createAssessment(classId, point));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long classId,
                               @PathVariable Long id,
                               @RequestBody AssessmentPoint point) {
        point.setId(id);
        assessmentService.updateAssessment(point);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long classId,
                               @PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return Result.ok();
    }

    /** 下载考核点批量导入模板 */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate(@PathVariable Long classId) {
        byte[] data = importService.generateAssessmentTemplate(classId);
        String filename = URLEncoder.encode("考核点导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 批量导入考核点 */
    @PostMapping("/import")
    public Result<Integer> importAssessments(@PathVariable Long classId,
                                             @RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importAssessments(classId, file));
    }
}
