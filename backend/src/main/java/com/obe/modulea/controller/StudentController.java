package com.obe.modulea.controller;

import com.obe.common.PageResult;
import com.obe.common.Result;
import com.obe.modulea.entity.Student;
import com.obe.modulea.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    public Result<PageResult<Student>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long adminClassId,
            @RequestParam(required = false) Integer enrollmentYear,
            @RequestParam(required = false) String enrollmentStatus) {
        return Result.ok(studentService.listStudents(page, size, keyword, collegeId, majorId, adminClassId, enrollmentYear, enrollmentStatus));
    }

    @GetMapping("/class/{classId}")
    public Result<List<Student>> listByClass(@PathVariable Long classId) {
        return Result.ok(studentService.getStudentsByClass(classId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Student> create(@RequestBody Student student) {
        return Result.ok(studentService.createStudent(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.updateStudent(student);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.ok();
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Integer> importStudents(@RequestBody List<Student> students) {
        int imported = studentService.importStudents(students);
        return Result.ok(imported);
    }

    @PutMapping("/batch-status")
    @PreAuthorize("hasRole('ACADEMIC')")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusRequest request) {
        studentService.batchUpdateStatus(request.getIds(), request.getStatus());
        return Result.ok();
    }

    public static class BatchStatusRequest {
        private List<Long> ids;
        private String status;
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
