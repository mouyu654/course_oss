package com.obe.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.common.BizException;
import com.obe.common.PageResult;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.moduleb.entity.CourseOutline;
import com.obe.moduleb.mapper.CourseOutlineMapper;
import com.obe.modulec.entity.ScoreSheet;
import com.obe.modulec.mapper.ScoreSheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysCollegeMapper collegeMapper;
    private final SysMajorMapper majorMapper;
    private final SysDictSemesterMapper semesterMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;
    private final SysAdminClassMapper adminClassMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final CourseOutlineMapper courseOutlineMapper;

    // ========== 学院 ==========

    public List<SysCollege> listColleges() {
        return collegeMapper.selectList(new LambdaQueryWrapper<SysCollege>().orderByAsc(SysCollege::getId));
    }

    public void createCollege(String name) {
        SysCollege college = new SysCollege();
        college.setName(name);
        collegeMapper.insert(college);
    }

    public void updateCollege(Long id, String name) {
        SysCollege college = collegeMapper.selectById(id);
        if (college == null) throw new BizException("学院不存在");
        college.setName(name);
        collegeMapper.updateById(college);
    }

    @Transactional
    public void deleteCollege(Long id) {
        Long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getCollegeId, id));
        if (studentCount > 0) throw new BizException("该学院下有 " + studentCount + " 名学生，无法删除");
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getCollegeId, id));
        if (userCount > 0) throw new BizException("该学院下有 " + userCount + " 名用户，无法删除");
        Long majorCount = majorMapper.selectCount(
                new LambdaQueryWrapper<SysMajor>().eq(SysMajor::getCollegeId, id));
        if (majorCount > 0) throw new BizException("该学院下有 " + majorCount + " 个专业，无法删除");
        collegeMapper.deleteById(id);
    }

    // ========== 专业 ==========

    public PageResult<SysMajor> listMajors(long page, long size, Long collegeId) {
        var wrapper = new LambdaQueryWrapper<SysMajor>();
        if (collegeId != null) {
            wrapper.eq(SysMajor::getCollegeId, collegeId);
        }
        wrapper.orderByAsc(SysMajor::getId);
        Page<SysMajor> result = majorMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public void createMajor(SysMajor major) {
        majorMapper.insert(major);
    }

    public void updateMajor(SysMajor major) {
        SysMajor existing = majorMapper.selectById(major.getId());
        if (existing == null) throw new BizException("专业不存在");
        majorMapper.updateById(major);
    }

    @Transactional
    public void deleteMajor(Long id) {
        Long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getMajorId, id));
        if (studentCount > 0) throw new BizException("该专业下有 " + studentCount + " 名学生，无法删除");
        Long classCount = adminClassMapper.selectCount(
                new LambdaQueryWrapper<SysAdminClass>().eq(SysAdminClass::getMajorId, id));
        if (classCount > 0) throw new BizException("该专业下有 " + classCount + " 个行政班级，无法删除");
        majorMapper.deleteById(id);
    }

    // ========== 学年学期 ==========

    public List<SysDictSemester> listSemesters() {
        return semesterMapper.selectList(
                new LambdaQueryWrapper<SysDictSemester>().orderByDesc(SysDictSemester::getAcademicYear));
    }

    public void createSemester(SysDictSemester semester) {
        semesterMapper.insert(semester);
    }

    public void updateSemester(Long id, SysDictSemester semester) {
        semester.setId(id);
        semesterMapper.updateById(semester);
    }

    @Transactional
    public void deleteSemester(Long id) {
        Long classCount = teachingClassMapper.selectCount(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getSemesterId, id));
        if (classCount > 0) throw new BizException("该学期下有 " + classCount + " 个教学班级，无法删除");
        semesterMapper.deleteById(id);
    }

    // ========== 教学班级 ==========

    public PageResult<TeachingClass> listClasses(long page, long size, String keyword, Long courseId, Long teacherId, Long semesterId) {
        var wrapper = new LambdaQueryWrapper<TeachingClass>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(TeachingClass::getClassName, keyword);
        }
        if (courseId != null) {
            wrapper.eq(TeachingClass::getCourseId, courseId);
        }
        if (teacherId != null) {
            wrapper.eq(TeachingClass::getTeacherId, teacherId);
        }
        if (semesterId != null) {
            wrapper.eq(TeachingClass::getSemesterId, semesterId);
        }
        wrapper.orderByAsc(TeachingClass::getId);
        Page<TeachingClass> result = teachingClassMapper.selectPage(new Page<>(page, size), wrapper);
        // Fill course, teacher and semester names
        for (TeachingClass tc : result.getRecords()) {
            Course course = courseMapper.selectById(tc.getCourseId());
            if (course != null) tc.setCourseName(course.getName());
            SysUser teacher = userMapper.selectById(tc.getTeacherId());
            if (teacher != null) tc.setTeacherName(teacher.getRealName());
            if (tc.getSemesterId() != null) {
                SysDictSemester sem = semesterMapper.selectById(tc.getSemesterId());
                if (sem != null) tc.setSemesterCode(sem.getSemesterCode());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public TeachingClass createClass(TeachingClass tc) {
        teachingClassMapper.insert(tc);
        return tc;
    }

    @Transactional
    public void updateClass(Long id, TeachingClass tc) {
        TeachingClass existing = teachingClassMapper.selectById(id);
        if (existing == null) throw new BizException("教学班级不存在");
        tc.setId(id);
        teachingClassMapper.updateById(tc);
    }

    @Transactional
    public void deleteClass(Long id) {
        if (teachingClassMapper.selectById(id) == null) throw new BizException("教学班级不存在");
        // 检查是否有成绩单
        Long sheetCount = scoreSheetMapper.selectCount(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, id));
        if (sheetCount > 0) throw new BizException("该教学班级已有成绩单，无法删除");
        // 检查是否有课程大纲
        Long outlineCount = courseOutlineMapper.selectCount(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, id));
        if (outlineCount > 0) throw new BizException("该教学班级已有课程大纲，无法删除");
        classStudentMapper.delete(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, id));
        teachingClassMapper.deleteById(id);
    }

    /** Get students assigned to a class */
    public List<Student> getClassStudents(Long classId) {
        List<ClassStudent> cs = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId));
        if (cs.isEmpty()) return List.of();
        List<Long> studentIds = cs.stream().map(ClassStudent::getStudentId).toList();
        return studentMapper.selectBatchIds(studentIds);
    }

    /** Assign a student to a class */
    @Transactional
    public void addClassStudent(Long classId, Long studentId) {
        if (classStudentMapper.selectOne(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .eq(ClassStudent::getStudentId, studentId)) != null) {
            return; // already assigned
        }
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        classStudentMapper.insert(cs);
    }

    /** Remove a student from a class */
    @Transactional
    public void removeClassStudent(Long classId, Long studentId) {
        classStudentMapper.delete(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .eq(ClassStudent::getStudentId, studentId));
    }

    // ========== 行政班级 ==========

    public PageResult<SysAdminClass> listAdminClasses(long page, long size, Long majorId) {
        var wrapper = new LambdaQueryWrapper<SysAdminClass>();
        if (majorId != null) {
            wrapper.eq(SysAdminClass::getMajorId, majorId);
        }
        wrapper.orderByAsc(SysAdminClass::getId);
        Page<SysAdminClass> result = adminClassMapper.selectPage(new Page<>(page, size), wrapper);
        for (SysAdminClass ac : result.getRecords()) {
            SysMajor major = majorMapper.selectById(ac.getMajorId());
            if (major != null) ac.setMajorName(major.getName());
            Long count = studentMapper.selectCount(
                    new LambdaQueryWrapper<Student>()
                            .eq(Student::getAdminClassId, ac.getId()));
            ac.setStudentCount(count.intValue());
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public SysAdminClass createAdminClass(SysAdminClass ac) {
        adminClassMapper.insert(ac);
        return ac;
    }

    @Transactional
    public void updateAdminClass(Long id, SysAdminClass ac) {
        if (adminClassMapper.selectById(id) == null) throw new BizException("行政班级不存在");
        ac.setId(id);
        adminClassMapper.updateById(ac);
    }

    @Transactional
    public void deleteAdminClass(Long id) {
        // Unlink students first
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getAdminClassId, id));
        for (Student s : students) {
            s.setAdminClassId(null);
            studentMapper.updateById(s);
        }
        adminClassMapper.deleteById(id);
    }

    public List<Student> getAdminClassStudents(Long classId) {
        return studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getAdminClassId, classId));
    }

    @Transactional
    public void addAdminClassStudent(Long classId, Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BizException("学生不存在");
        student.setAdminClassId(classId);
        studentMapper.updateById(student);
    }

    @Transactional
    public void removeAdminClassStudent(Long classId, Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student != null && classId.equals(student.getAdminClassId())) {
            student.setAdminClassId(null);
            studentMapper.updateById(student);
        }
    }
}
