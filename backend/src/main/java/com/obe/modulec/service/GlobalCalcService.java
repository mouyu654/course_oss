package com.obe.modulec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.BizException;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulec.entity.CourseAchievement;
import com.obe.modulec.entity.MajorAchievement;
import com.obe.modulec.entity.ScoreSheet;
import com.obe.modulec.mapper.CourseAchievementMapper;
import com.obe.modulec.mapper.MajorAchievementMapper;
import com.obe.modulec.mapper.ScoreSheetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalCalcService {

    private final MacroSupportMatrixMapper macroSupportMatrixMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final MajorAchievementMapper majorAchievementMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final SysDictSemesterMapper semesterMapper;
    private final CourseMajorMapper courseMajorMapper;
    private final StudentMapper studentMapper;
    private final ClassStudentMapper classStudentMapper;

    /**
     * 宏观看板：按年级、专业查询支撑课程的计算状态 + 权重配置状态。
     */
    public DashboardData getDashboard(Integer enrollmentYear, Long majorId) {
        // 1. 确定专业集合：选了专业就用该专业，没选就取该年级所有专业
        Set<Long> majorIds = new HashSet<>();
        if (majorId != null) {
            majorIds.add(majorId);
        } else if (enrollmentYear != null) {
            studentMapper.selectList(
                    new LambdaQueryWrapper<Student>()
                            .select(Student::getMajorId)
                            .eq(Student::getEnrollmentYear, enrollmentYear)
                            .isNotNull(Student::getMajorId)
                            .groupBy(Student::getMajorId))
                    .forEach(s -> majorIds.add(s.getMajorId()));
        } else {
            courseMajorMapper.selectList(null)
                    .forEach(cm -> majorIds.add(cm.getMajorId()));
        }

        if (majorIds.isEmpty()) {
            return DashboardData.empty();
        }

        // 2. 宏观矩阵中的支撑课程 ∩ 专业的课程
        List<MacroSupportMatrix> matrix = macroSupportMatrixMapper.selectList(null);
        Set<Long> matrixCourseIds = matrix.stream()
                .map(MacroSupportMatrix::getCourseId)
                .collect(Collectors.toSet());

        Set<Long> relevantCourseIds = new HashSet<>();
        for (Long mid : majorIds) {
            courseMajorMapper.selectList(
                    new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getMajorId, mid))
                    .stream().map(CourseMajor::getCourseId)
                    .filter(matrixCourseIds::contains)
                    .forEach(relevantCourseIds::add);
        }

        if (relevantCourseIds.isEmpty()) {
            return DashboardData.empty();
        }

        // 3. 查询教学班级：按课程筛选，按年级+专业过滤
        List<TeachingClass> allClasses;
        if (enrollmentYear != null || majorId != null) {
            var studentQuery = new LambdaQueryWrapper<Student>()
                    .select(Student::getId);
            if (enrollmentYear != null) {
                studentQuery.eq(Student::getEnrollmentYear, enrollmentYear);
            }
            if (majorId != null) {
                studentQuery.eq(Student::getMajorId, majorId);
            }
            Set<Long> filteredStudentIds = studentMapper.selectList(studentQuery)
                    .stream().map(Student::getId).collect(Collectors.toSet());
            Set<Long> filteredClassIds = filteredStudentIds.isEmpty() ? Set.of()
                    : classStudentMapper.selectList(
                            new LambdaQueryWrapper<ClassStudent>()
                                    .in(ClassStudent::getStudentId, filteredStudentIds))
                    .stream().map(ClassStudent::getClassId).collect(Collectors.toSet());
            allClasses = teachingClassMapper.selectList(
                    new LambdaQueryWrapper<TeachingClass>()
                            .in(TeachingClass::getCourseId, relevantCourseIds))
                    .stream().filter(tc -> filteredClassIds.contains(tc.getId())).toList();
        } else {
            allClasses = teachingClassMapper.selectList(
                    new LambdaQueryWrapper<TeachingClass>()
                            .in(TeachingClass::getCourseId, relevantCourseIds));
        }

        // 4. 按课程聚合看板数据
        Map<Long, List<TeachingClass>> courseClassMap = new LinkedHashMap<>();
        for (TeachingClass tc : allClasses) {
            courseClassMap.computeIfAbsent(tc.getCourseId(), k -> new ArrayList<>()).add(tc);
        }

        List<DashboardData.CourseStatus> courseStatuses = new ArrayList<>();
        int lockedCourseCount = 0;

        for (var entry : courseClassMap.entrySet()) {
            Long courseId = entry.getKey();
            List<TeachingClass> classes = entry.getValue();
            Course course = courseMapper.selectById(courseId);
            String courseName = course != null ? course.getName() : "";

            boolean allLocked = true;
            List<Long> classIds = new ArrayList<>();
            List<DashboardData.ClassDetail> classDetails = new ArrayList<>();
            for (TeachingClass tc : classes) {
                classIds.add(tc.getId());
                ScoreSheet sheet = scoreSheetMapper.selectOne(
                        new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, tc.getId()));
                String classStatus = sheet == null ? "EMPTY" : sheet.getStatus();
                if (!"LOCKED".equals(classStatus)) allLocked = false;

                String teacherName = "";
                if (tc.getTeacherId() != null) {
                    SysUser teacher = userMapper.selectById(tc.getTeacherId());
                    teacherName = teacher != null ? teacher.getRealName() : "";
                }
                String semesterName = "";
                if (tc.getSemesterId() != null) {
                    SysDictSemester sem = semesterMapper.selectById(tc.getSemesterId());
                    semesterName = sem != null ? sem.getSemesterCode() : "";
                }
                classDetails.add(new DashboardData.ClassDetail(
                        tc.getId(), tc.getClassName(), teacherName, semesterName, classStatus));
            }

            if (allLocked) lockedCourseCount++;
            courseStatuses.add(new DashboardData.CourseStatus(
                    courseId, courseName, classIds, allLocked ? "LOCKED" : "UNLOCKED", classDetails));
        }

        boolean allReady = !courseStatuses.isEmpty() && lockedCourseCount == courseStatuses.size();
        List<Long> incompleteCourseIds = courseStatuses.stream()
                .filter(cs -> !"LOCKED".equals(cs.status()))
                .map(DashboardData.CourseStatus::courseId)
                .toList();

        // 6. 权重配置校验
        List<DashboardData.WeightStatus> weightStatuses = buildWeightStatuses(matrix, relevantCourseIds);

        return new DashboardData(allReady, lockedCourseCount, courseStatuses.size(),
                incompleteCourseIds, courseStatuses, weightStatuses);
    }

    /**
     * 构建每个指标点的权重校验状态。
     */
    private List<DashboardData.WeightStatus> buildWeightStatuses(
            List<MacroSupportMatrix> matrix, Set<Long> relevantCourseIds) {
        Map<Long, BigDecimal> weightSums = new LinkedHashMap<>();
        for (MacroSupportMatrix m : matrix) {
            if (!relevantCourseIds.contains(m.getCourseId())) continue;
            if (m.getWeight() == null) continue;
            weightSums.merge(m.getIndicatorId(), m.getWeight(), BigDecimal::add);
        }

        List<DashboardData.WeightStatus> statuses = new ArrayList<>();
        for (var entry : weightSums.entrySet()) {
            BigDecimal sum = entry.getValue().setScale(4, RoundingMode.HALF_UP);
            boolean valid = sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) <= 0;
            statuses.add(new DashboardData.WeightStatus(entry.getKey(), sum, valid));
        }
        return statuses;
    }

    /**
     * 执行专业级全局达成度计算。
     * 维度：(majorId, enrollmentYear)，不含学期编码。
     */
    @Transactional(rollbackFor = Exception.class)
    public MajorCalcResult compute(Integer enrollmentYear, Long majorId, Long operator) {
        DashboardData dashboard = getDashboard(enrollmentYear, majorId);

        // 前置校验1：所有支撑课程必须已锁定
        if (!dashboard.allReady()) {
            throw new BizException("存在未完成课程级计算的课程: " + dashboard.incompleteCourseIds());
        }

        // 前置校验2：权重配置必须正确（每个指标点权重和 = 1.0）
        List<String> badIndicators = dashboard.weightStatuses().stream()
                .filter(ws -> !ws.valid())
                .map(ws -> "指标点" + ws.indicatorId() + "权重和=" + ws.weightSum())
                .toList();
        if (!badIndicators.isEmpty()) {
            throw new BizException("权重配置不正确，无法计算: " + String.join("; ", badIndicators));
        }

        // 获取相关矩阵
        List<MacroSupportMatrix> matrix = macroSupportMatrixMapper.selectList(null);
        Set<Long> courseIds = dashboard.courseStatuses().stream()
                .map(DashboardData.CourseStatus::courseId).collect(Collectors.toSet());
        List<MacroSupportMatrix> relevantMatrix = matrix.stream()
                .filter(m -> courseIds.contains(m.getCourseId())).toList();

        // ---- 课程级达成度聚合：同一课程多个教学班级取平均 ----
        // courseId -> (indicatorId -> 平均E_k)
        Map<Long, Map<Long, BigDecimal>> courseAchievements = new HashMap<>();
        for (DashboardData.CourseStatus cs : dashboard.courseStatuses()) {
            Map<Long, List<BigDecimal>> indicatorValues = new HashMap<>();
            for (Long classId : cs.classIds()) {
                List<CourseAchievement> achievements = courseAchievementMapper.selectList(
                        new LambdaQueryWrapper<CourseAchievement>()
                                .eq(CourseAchievement::getClassId, classId));
                for (CourseAchievement ca : achievements) {
                    indicatorValues.computeIfAbsent(ca.getIndicatorId(), k -> new ArrayList<>())
                            .add(ca.getAchievement());
                }
            }
            Map<Long, BigDecimal> avgMap = new HashMap<>();
            for (var entry : indicatorValues.entrySet()) {
                List<BigDecimal> vals = entry.getValue();
                BigDecimal avg = vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(vals.size()), 6, RoundingMode.HALF_UP);
                avgMap.put(entry.getKey(), avg);
            }
            courseAchievements.put(cs.courseId(), avgMap);
        }

        // ---- G_k = Σ(E_k × W_c)，W_c 是课程级权重 ----
        // 按指标点分组
        Map<Long, List<MacroSupportMatrix>> indicatorMatrixMap = relevantMatrix.stream()
                .filter(m -> m.getWeight() != null)
                .collect(Collectors.groupingBy(MacroSupportMatrix::getIndicatorId));

        Map<Long, BigDecimal> majorAchievements = new LinkedHashMap<>();
        for (var entry : indicatorMatrixMap.entrySet()) {
            Long indicatorId = entry.getKey();
            BigDecimal gk = BigDecimal.ZERO;
            for (MacroSupportMatrix m : entry.getValue()) {
                Map<Long, BigDecimal> courseEK = courseAchievements.get(m.getCourseId());
                if (courseEK == null) continue;
                BigDecimal ek = courseEK.get(indicatorId);
                if (ek == null) continue;
                gk = gk.add(ek.multiply(m.getWeight()));
            }
            majorAchievements.put(indicatorId, gk.setScale(4, RoundingMode.HALF_UP));
        }

        // ---- 持久化：维度 (majorId, enrollmentYear) ----
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MajorAchievement> deleteWrapper = new LambdaQueryWrapper<MajorAchievement>()
                .eq(MajorAchievement::getMajorId, majorId);
        if (enrollmentYear != null) {
            deleteWrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        } else {
            deleteWrapper.isNull(MajorAchievement::getEnrollmentYear);
        }
        majorAchievementMapper.delete(deleteWrapper);

        for (var e : majorAchievements.entrySet()) {
            MajorAchievement ma = new MajorAchievement();
            ma.setMajorId(majorId);
            ma.setIndicatorId(e.getKey());
            ma.setEnrollmentYear(enrollmentYear);
            ma.setAchievement(e.getValue());
            ma.setCalcTime(now);
            ma.setTriggeredBy(operator);
            majorAchievementMapper.insert(ma);
        }

        return new MajorCalcResult(majorAchievements, now);
    }

    public Map<Long, BigDecimal> getResults(Long majorId, Integer enrollmentYear) {
        var wrapper = new LambdaQueryWrapper<MajorAchievement>()
                .eq(MajorAchievement::getMajorId, majorId);
        if (enrollmentYear != null) {
            wrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        } else {
            wrapper.isNull(MajorAchievement::getEnrollmentYear);
        }
        List<MajorAchievement> list = majorAchievementMapper.selectList(wrapper);
        return list.stream().collect(Collectors.toMap(MajorAchievement::getIndicatorId, MajorAchievement::getAchievement));
    }

    public List<Integer> getEnrollmentYears() {
        List<Student> all = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .select(Student::getEnrollmentYear)
                        .isNotNull(Student::getEnrollmentYear)
                        .groupBy(Student::getEnrollmentYear)
                        .orderByDesc(Student::getEnrollmentYear));
        return all.stream().map(Student::getEnrollmentYear).toList();
    }

    // ---- DTOs ----

    public record DashboardData(
            boolean allReady,
            int lockedCount,
            int totalCount,
            List<Long> incompleteCourseIds,
            List<CourseStatus> courseStatuses,
            List<WeightStatus> weightStatuses) {

        public static DashboardData empty() {
            return new DashboardData(false, 0, 0, List.of(), List.of(), List.of());
        }

        /** 课程级状态：一门课聚合所有教学班级 */
        public record CourseStatus(Long courseId, String courseName, List<Long> classIds, String status,
                                   List<ClassDetail> classDetails) {}

        /** 教学班级详情 */
        public record ClassDetail(Long classId, String className, String teacherName, String semesterName, String status) {}

        /** 指标点权重校验状态 */
        public record WeightStatus(Long indicatorId, BigDecimal weightSum, boolean valid) {}
    }

    public record MajorCalcResult(Map<Long, BigDecimal> achievements, LocalDateTime calcTime) {}
}
