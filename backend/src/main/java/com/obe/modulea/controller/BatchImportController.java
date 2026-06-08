package com.obe.modulea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.Result;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/batch-import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ACADEMIC')")
public class BatchImportController {

    private final StudentMapper studentMapper;
    private final SysCollegeMapper collegeMapper;
    private final SysMajorMapper majorMapper;
    private final CourseMapper courseMapper;
    private final CourseMajorMapper courseMajorMapper;
    private final AdminClassStudentMapper adminClassStudentMapper;
    private final ClassStudentMapper classStudentMapper;

    // ===== 模板下载 =====

    @GetMapping("/templates/students")
    public void downloadStudentTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=students_template.xlsx");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("学生信息");
        String[] headers = {"学号", "姓名", "学院名称", "专业名称", "入学年份", "学籍状态"};
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
        // Example row
        Row ex = sheet.createRow(1);
        ex.createCell(0).setCellValue("2024010101");
        ex.createCell(1).setCellValue("张三");
        ex.createCell(2).setCellValue("计算机学院");
        ex.createCell(3).setCellValue("计算机科学与技术");
        ex.createCell(4).setCellValue(2024);
        ex.createCell(5).setCellValue("在读");
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/templates/courses")
    public void downloadCourseTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=courses_template.xlsx");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("课程信息");
        String[] headers = {"课程代码", "课程名称", "学分", "理论学时", "实验学时", "所属专业名称"};
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
        Row ex = sheet.createRow(1);
        ex.createCell(0).setCellValue("CS1001");
        ex.createCell(1).setCellValue("数据结构");
        ex.createCell(2).setCellValue(3.0);
        ex.createCell(3).setCellValue(48);
        ex.createCell(4).setCellValue(16);
        ex.createCell(5).setCellValue("计算机科学与技术");
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/templates/class-students")
    public void downloadClassStudentTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=class_students_template.xlsx");
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("班级学生");
        String[] headers = {"学号", "姓名"};
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
        Row ex = sheet.createRow(1);
        ex.createCell(0).setCellValue("2024010101");
        ex.createCell(1).setCellValue("张三");
        wb.write(response.getOutputStream());
        wb.close();
    }

    // ===== 批量导入 =====

    @PostMapping("/students")
    @Transactional
    public Result<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) throws IOException {
        List<Student> students = parseStudentFile(file);
        int imported = 0, skipped = 0;
        for (Student s : students) {
            Long count = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, s.getStudentNo()));
            if (count == 0) {
                studentMapper.insert(s);
                imported++;
            } else {
                skipped++;
            }
        }
        return Result.ok(Map.of("imported", imported, "skipped", skipped, "total", students.size()));
    }

    @PostMapping("/courses")
    @Transactional
    public Result<Map<String, Object>> importCourses(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = parseCourseFile(file);
        int imported = 0, skipped = 0;
        for (Map<String, Object> r : rows) {
            String code = (String) r.get("code");
            Long count = courseMapper.selectCount(
                    new LambdaQueryWrapper<Course>().eq(Course::getCode, code));
            if (count > 0) { skipped++; continue; }
            Course c = new Course();
            c.setCode(code);
            c.setName((String) r.get("name"));
            c.setCredit((BigDecimal) r.get("credit"));
            courseMapper.insert(c);
            imported++;
        }
        return Result.ok(Map.of("imported", imported, "skipped", skipped, "total", rows.size()));
    }

    @PostMapping("/admin-classes/{classId}/students")
    @Transactional
    public Result<Map<String, Object>> importAdminClassStudents(
            @PathVariable Long classId, @RequestParam("file") MultipartFile file) throws IOException {
        List<String> studentNos = parseStudentNoFile(file);
        int added = 0, notFound = 0, exists = 0;
        for (String sno : studentNos) {
            Student s = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, sno));
            if (s == null) { notFound++; continue; }
            // Check if already assigned to this admin class
            if (classId.equals(s.getAdminClassId())) { exists++; continue; }
            s.setAdminClassId(classId);
            studentMapper.updateById(s);
            added++;
        }
        return Result.ok(Map.of("added", added, "notFound", notFound, "exists", exists, "total", studentNos.size()));
    }

    @PostMapping("/teaching-classes/{classId}/students")
    @Transactional
    public Result<Map<String, Object>> importTeachingClassStudents(
            @PathVariable Long classId, @RequestParam("file") MultipartFile file) throws IOException {
        List<String> studentNos = parseStudentNoFile(file);
        int added = 0, notFound = 0, exists = 0;
        for (String sno : studentNos) {
            Student s = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, sno));
            if (s == null) { notFound++; continue; }
            Long cnt = classStudentMapper.selectCount(
                    new LambdaQueryWrapper<ClassStudent>()
                            .eq(ClassStudent::getClassId, classId)
                            .eq(ClassStudent::getStudentId, s.getId()));
            if (cnt > 0) { exists++; continue; }
            ClassStudent cs = new ClassStudent();
            cs.setClassId(classId);
            cs.setStudentId(s.getId());
            classStudentMapper.insert(cs);
            added++;
        }
        return Result.ok(Map.of("added", added, "notFound", notFound, "exists", exists, "total", studentNos.size()));
    }

    // ===== Private helpers =====

    private List<Student> parseStudentFile(MultipartFile file) throws IOException {
        List<Student> list = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Long> collegeMap = buildCollegeNameMap();
            Map<String, Long> majorMap = buildMajorNameMap();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || getCellStr(row, 0).isBlank()) continue;
                Student s = new Student();
                s.setStudentNo(getCellStr(row, 0));
                s.setName(getCellStr(row, 1));
                String collegeName = getCellStr(row, 2);
                if (!collegeName.isBlank()) s.setCollegeId(collegeMap.get(collegeName));
                String majorName = getCellStr(row, 3);
                if (!majorName.isBlank()) s.setMajorId(majorMap.get(majorName));
                String yearStr = getCellStr(row, 4);
                if (!yearStr.isBlank()) {
                    try { s.setEnrollmentYear((int) Double.parseDouble(yearStr)); } catch (NumberFormatException ignored) {}
                }
                String status = getCellStr(row, 5);
                s.setEnrollmentStatus(status.isBlank() ? "在读" : status);
                list.add(s);
            }
        }
        return list;
    }

    private List<Map<String, Object>> parseCourseFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || getCellStr(row, 0).isBlank()) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("code", getCellStr(row, 0));
                r.put("name", getCellStr(row, 1));
                String creditStr = getCellStr(row, 2);
                if (!creditStr.isBlank()) r.put("credit", new BigDecimal(creditStr));
                list.add(r);
            }
        }
        return list;
    }

    private List<String> parseStudentNoFile(MultipartFile file) throws IOException {
        List<String> list = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String sno = getCellStr(row, 0).trim();
                if (!sno.isBlank()) list.add(sno);
            }
        }
        return list;
    }

    private String getCellStr(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private Map<String, Long> buildCollegeNameMap() {
        Map<String, Long> map = new HashMap<>();
        for (SysCollege c : collegeMapper.selectList(null)) map.put(c.getName(), c.getId());
        return map;
    }

    private Map<String, Long> buildMajorNameMap() {
        Map<String, Long> map = new HashMap<>();
        for (SysMajor m : majorMapper.selectList(null)) map.put(m.getName(), m.getId());
        return map;
    }
}
