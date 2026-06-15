package com.obe.moduleb.controller;

import com.obe.common.Result;
import com.obe.moduleb.entity.CourseObjective;
import com.obe.moduleb.service.ObjectiveService;
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
@RequestMapping("/api/classes/{classId}/objectives")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class ObjectiveController {

    private final ObjectiveService objectiveService;
    private final TeacherConfigImportService importService;

    @GetMapping
    public Result<List<CourseObjective>> list(@PathVariable Long classId) {
        return Result.ok(objectiveService.listObjectives(classId));
    }

    @PostMapping
    public Result<CourseObjective> create(@PathVariable Long classId,
                                          @RequestBody CourseObjective objective) {
        return Result.ok(objectiveService.createObjective(classId, objective));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long classId,
                               @PathVariable Long id,
                               @RequestBody CourseObjective objective) {
        objective.setId(id);
        objectiveService.updateObjective(objective);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long classId,
                               @PathVariable Long id) {
        objectiveService.deleteObjective(id);
        return Result.ok();
    }

    /** 下载课程目标批量导入模板 */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] data = importService.generateObjectiveTemplate();
        String filename = URLEncoder.encode("课程目标导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 批量导入课程目标 */
    @PostMapping("/import")
    public Result<Integer> importObjectives(@PathVariable Long classId,
                                            @RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importObjectives(classId, file));
    }
}
