package com.obe.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.common.BizException;
import com.obe.common.PageResult;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseMajorMapper courseMajorMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final SysUserMapper userMapper;
    private final MacroSupportMatrixMapper macroSupportMatrixMapper;

    public PageResult<Course> listCourses(long page, long size, Long majorId, String keyword) {
        var wrapper = new LambdaQueryWrapper<Course>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Course::getName, keyword)
                    .or().like(Course::getCode, keyword));
        }
        wrapper.orderByAsc(Course::getCode);

        Page<Course> result = courseMapper.selectPage(new Page<>(page, size), wrapper);
        List<Course> courses = result.getRecords();

        // Build course → majorId map
        List<CourseMajor> allLinks = courseMajorMapper.selectList(null);
        Map<Long, Long> courseMajorMap = allLinks.stream()
                .collect(Collectors.toMap(CourseMajor::getCourseId, CourseMajor::getMajorId, (a, b) -> a));
        for (Course c : courses) {
            c.setMajorId(courseMajorMap.get(c.getId()));
        }

        // 如果按专业筛选，只返回该专业的课程
        if (majorId != null) {
            Set<Long> courseIds = allLinks.stream()
                    .filter(cm -> cm.getMajorId().equals(majorId))
                    .map(CourseMajor::getCourseId).collect(Collectors.toSet());
            courses = courses.stream()
                    .filter(c -> courseIds.contains(c.getId()))
                    .collect(Collectors.toList());
            for (Course c : courses) {
                c.setMajorId(majorId);
            }
            return new PageResult<>(courses, courses.size(), 1, courses.size());
        }

        return new PageResult<>(courses, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void createCourse(Course course) {
        courseMapper.insert(course);
        if (course.getMajorId() != null) {
            CourseMajor cm = new CourseMajor();
            cm.setCourseId(course.getId());
            cm.setMajorId(course.getMajorId());
            courseMajorMapper.insert(cm);
        }
    }

    @Transactional
    public void updateCourse(Long id, Course course) {
        if (courseMapper.selectById(id) == null) throw new BizException("课程不存在");
        course.setId(id);
        courseMapper.updateById(course);
        if (course.getMajorId() != null) {
            courseMajorMapper.delete(new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getCourseId, id));
            CourseMajor cm = new CourseMajor();
            cm.setCourseId(id);
            cm.setMajorId(course.getMajorId());
            courseMajorMapper.insert(cm);
        }
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (courseMapper.selectById(id) == null) throw new BizException("课程不存在");
        // 检查是否有关联的教学班级
        Long classCount = teachingClassMapper.selectCount(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getCourseId, id));
        if (classCount > 0) throw new BizException("该课程下有 " + classCount + " 个教学班级，请先删除教学班级");
        // 检查是否在宏观支撑矩阵中
        Long matrixCount = macroSupportMatrixMapper.selectCount(
                new LambdaQueryWrapper<MacroSupportMatrix>().eq(MacroSupportMatrix::getCourseId, id));
        if (matrixCount > 0) throw new BizException("该课程在宏观支撑矩阵中有 " + matrixCount + " 条配置，请先移除矩阵配置");
        courseMajorMapper.delete(new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getCourseId, id));
        courseMapper.deleteById(id);
    }

    public List<TeachingClass> listClasses(Long courseId) {
        List<TeachingClass> classes = teachingClassMapper.selectList(
                new LambdaQueryWrapper<TeachingClass>()
                        .eq(TeachingClass::getCourseId, courseId)
                        .orderByDesc(TeachingClass::getSemesterId));

        for (TeachingClass tc : classes) {
            Course course = courseMapper.selectById(tc.getCourseId());
            if (course != null) tc.setCourseName(course.getName());
            SysUser teacher = userMapper.selectById(tc.getTeacherId());
            if (teacher != null) tc.setTeacherName(teacher.getRealName());
        }
        return classes;
    }

    @Transactional
    public void createClass(TeachingClass teachingClass) {
        teachingClassMapper.insert(teachingClass);
    }

    /** 获取所有课程，标记哪些属于指定专业 */
    public Map<String, Object> getMajorCourses(Long majorId) {
        List<Course> allCourses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().orderByAsc(Course::getCode));
        Set<Long> linkedCourseIds = courseMajorMapper.selectList(
                new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getMajorId, majorId))
                .stream().map(CourseMajor::getCourseId).collect(Collectors.toSet());

        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Course c : allCourses) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("code", c.getCode());
            item.put("name", c.getName());
            item.put("credit", c.getCredit());
            item.put("linked", linkedCourseIds.contains(c.getId()));
            list.add(item);
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("courses", list);
        result.put("linkedCount", linkedCourseIds.size());
        return result;
    }

    /** 批量替换专业的课程关联 */
    @Transactional
    public void updateMajorCourses(Long majorId, List<Long> courseIds) {
        courseMajorMapper.delete(
                new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getMajorId, majorId));
        for (Long courseId : courseIds) {
            CourseMajor cm = new CourseMajor();
            cm.setCourseId(courseId);
            cm.setMajorId(majorId);
            courseMajorMapper.insert(cm);
        }
    }
}
