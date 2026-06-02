package com.obe.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.modulec.entity.*;
import com.obe.modulec.mapper.*;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.moduleb.entity.AssessmentPoint;
import com.obe.moduleb.entity.CourseObjective;
import com.obe.moduleb.entity.CourseOutline;
import com.obe.moduleb.entity.ObjectiveIndicatorWeight;
import com.obe.moduleb.mapper.AssessmentPointMapper;
import com.obe.moduleb.mapper.CourseObjectiveMapper;
import com.obe.moduleb.mapper.CourseOutlineMapper;
import com.obe.moduleb.mapper.ObjectiveIndicatorWeightMapper;
import com.obe.modulec.entity.*;
import com.obe.modulec.mapper.*;
import com.obe.moduled.exporter.TraceExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MajorReportService {

    private final MajorAchievementMapper majorAchievementMapper;
    private final IndicatorMapper indicatorMapper;
    private final CourseMapper courseMapper;
    private final CourseMajorMapper courseMajorMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final MacroSupportMatrixMapper macroMatrixMapper;
    private final CourseAchievementMapper courseAchievementMapper;
    private final ObjAchievementMapper objAchievementMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final SysMajorMapper majorMapper;

    /**
     * 列出所有计算批次（按 enrollmentYear + majorId 分组）。
     */
    public List<Map<String, Object>> listCalcBatches(Integer enrollmentYear, Long majorId) {
        var wrapper = new LambdaQueryWrapper<MajorAchievement>()
                .orderByDesc(MajorAchievement::getCalcTime);
        if (enrollmentYear != null) {
            wrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        }
        if (majorId != null) {
            wrapper.eq(MajorAchievement::getMajorId, majorId);
        }
        List<MajorAchievement> all = majorAchievementMapper.selectList(wrapper);

        // Group by (majorId, enrollmentYear) to get unique batches
        Map<String, List<MajorAchievement>> grouped = all.stream()
                .collect(Collectors.groupingBy(a -> a.getMajorId() + "_" + a.getEnrollmentYear(),
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> batches = new ArrayList<>();
        int id = 1;
        for (var entry : grouped.entrySet()) {
            List<MajorAchievement> items = entry.getValue();
            MajorAchievement first = items.get(0);
            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("id", id++);
            batch.put("majorId", first.getMajorId());
            batch.put("enrollmentYear", first.getEnrollmentYear());

            SysMajor major = majorMapper.selectById(first.getMajorId());
            batch.put("majorName", major != null ? major.getName() : "");
            batch.put("calcTime", first.getCalcTime());
            batches.add(batch);
        }
        return batches;
    }

    /**
     * 获取某批次的报告数据（雷达图数据）。
     */
    public Map<String, Object> getReportData(Long majorId, Integer enrollmentYear) {
        Map<String, Object> data = new LinkedHashMap<>();

        LambdaQueryWrapper<MajorAchievement> wrapper = new LambdaQueryWrapper<MajorAchievement>()
                .orderByDesc(MajorAchievement::getCalcTime);
        if (majorId != null) {
            wrapper.eq(MajorAchievement::getMajorId, majorId);
        }
        if (enrollmentYear != null) {
            wrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        }
        List<MajorAchievement> achievements = majorAchievementMapper.selectList(wrapper);

        List<Indicator> indicators = indicatorMapper.selectList(null);
        Map<Long, Indicator> indicatorMap = new HashMap<>();
        for (Indicator ind : indicators) indicatorMap.put(ind.getId(), ind);

        Map<String, BigDecimal> radarData = new LinkedHashMap<>();
        for (MajorAchievement ma : achievements) {
            Indicator ind = indicatorMap.get(ma.getIndicatorId());
            radarData.put(ind != null ? ind.getIndicatorNo() : String.valueOf(ma.getIndicatorId()),
                    ma.getAchievement());
        }
        data.put("radarData", radarData);
        data.put("achievements", achievements);
        data.put("calcTime", achievements.isEmpty() ? null : achievements.get(0).getCalcTime());

        return data;
    }

    /**
     * 删除某批次的计算结果。
     */
    @Transactional
    public void deleteBatch(Long majorId, Integer enrollmentYear) {
        LambdaQueryWrapper<MajorAchievement> wrapper = new LambdaQueryWrapper<MajorAchievement>();
        if (majorId != null) {
            wrapper.eq(MajorAchievement::getMajorId, majorId);
        }
        if (enrollmentYear != null) {
            wrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        } else {
            wrapper.isNull(MajorAchievement::getEnrollmentYear);
        }
        majorAchievementMapper.delete(wrapper);
    }

    /**
     * 生成穿透式 Excel 台账。
     */
    public byte[] generateTraceExcel(Long majorId, Integer enrollmentYear) {
        LambdaQueryWrapper<MajorAchievement> wrapper = new LambdaQueryWrapper<MajorAchievement>();
        if (majorId != null) {
            wrapper.eq(MajorAchievement::getMajorId, majorId);
        }
        if (enrollmentYear != null) {
            wrapper.eq(MajorAchievement::getEnrollmentYear, enrollmentYear);
        }
        List<MajorAchievement> achievements = majorAchievementMapper.selectList(wrapper);

        List<Indicator> indicators = indicatorMapper.selectList(null);
        Map<Long, Indicator> indMap = new HashMap<>();
        for (Indicator ind : indicators) indMap.put(ind.getId(), ind);

        // Determine majorIds for course lookup
        Set<Long> majorIds = new HashSet<>();
        if (majorId != null) {
            majorIds.add(majorId);
        } else {
            achievements.stream().map(MajorAchievement::getMajorId).filter(Objects::nonNull).forEach(majorIds::add);
        }
        Set<Long> courseIds = new HashSet<>();
        for (Long mid : majorIds) {
            courseMajorMapper.selectList(
                    new LambdaQueryWrapper<CourseMajor>().eq(CourseMajor::getMajorId, mid))
                    .forEach(cm -> courseIds.add(cm.getCourseId()));
        }
        List<Course> courses = courseIds.isEmpty() ? List.of()
                : courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds));

        List<MacroSupportMatrix> matrix = macroMatrixMapper.selectList(
                new LambdaQueryWrapper<MacroSupportMatrix>()
                        .in(MacroSupportMatrix::getCourseId, courses.stream().map(Course::getId).toList()));

        Map<String, BigDecimal> majorResultsMap = new LinkedHashMap<>();
        for (MajorAchievement ma : achievements) {
            Indicator ind = indMap.get(ma.getIndicatorId());
            majorResultsMap.put(ind != null ? ind.getIndicatorNo() : String.valueOf(ma.getIndicatorId()),
                    ma.getAchievement());
        }

        List<TraceExcelExporter.IndicatorTraceSheet> traceSheets = new ArrayList<>();
        for (MajorAchievement ma : achievements) {
            Long indicatorId = ma.getIndicatorId();
            Indicator ind = indMap.get(indicatorId);
            String indNo = ind != null ? ind.getIndicatorNo() : String.valueOf(indicatorId);
            List<TraceExcelExporter.TraceRow> rows = new ArrayList<>();

            for (MacroSupportMatrix m : matrix) {
                if (!m.getIndicatorId().equals(indicatorId)) continue;
                Course course = courses.stream().filter(c -> c.getId().equals(m.getCourseId())).findFirst().orElse(null);
                if (course == null) continue;

                List<TeachingClass> classes = teachingClassMapper.selectList(
                        new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getCourseId, course.getId()));

                for (TeachingClass tc : classes) {
                    CourseAchievement ca = courseAchievementMapper.selectOne(
                            new LambdaQueryWrapper<CourseAchievement>()
                                    .eq(CourseAchievement::getClassId, tc.getId())
                                    .eq(CourseAchievement::getIndicatorId, indicatorId));
                    BigDecimal ek = ca != null ? ca.getAchievement() : null;

                    CourseOutline outline = outlineMapper.selectOne(
                            new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, tc.getId()));
                    if (outline == null) continue;

                    List<CourseObjective> objectives = objectiveMapper.selectList(
                            new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));

                    for (CourseObjective obj : objectives) {
                        ObjAchievement oa = objAchievementMapper.selectOne(
                                new LambdaQueryWrapper<ObjAchievement>()
                                        .eq(ObjAchievement::getClassId, tc.getId())
                                        .eq(ObjAchievement::getObjectiveId, obj.getId()));

                        ObjectiveIndicatorWeight wijk = weightMapper.selectOne(
                                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                                        .eq(ObjectiveIndicatorWeight::getObjectiveId, obj.getId())
                                        .eq(ObjectiveIndicatorWeight::getIndicatorId, indicatorId));

                        List<AssessmentPoint> aps = assessmentPointMapper.selectList(
                                new LambdaQueryWrapper<AssessmentPoint>()
                                        .eq(AssessmentPoint::getObjectiveId, obj.getId()));

                        for (AssessmentPoint ap : aps) {
                            ScoreSheet ss = scoreSheetMapper.selectOne(
                                    new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, tc.getId()));
                            BigDecimal avgScore = null;
                            if (ss != null) {
                                List<StudentScore> scores = studentScoreMapper.selectList(
                                        new LambdaQueryWrapper<StudentScore>()
                                                .eq(StudentScore::getSheetId, ss.getId())
                                                .eq(StudentScore::getAssessmentId, ap.getId()));
                                if (!scores.isEmpty()) {
                                    avgScore = scores.stream().map(StudentScore::getScore)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                                }
                            }
                            rows.add(new TraceExcelExporter.TraceRow(
                                    course.getName(), ek, m.getWeight(),
                                    obj.getObjNo(), oa != null ? oa.getAchievement() : null,
                                    wijk != null ? wijk.getWeight() : null,
                                    ap.getName(), ap.getMaxScore(), avgScore));
                        }
                    }
                }
            }
            traceSheets.add(new TraceExcelExporter.IndicatorTraceSheet(indNo, rows));
        }
        return TraceExcelExporter.generateTraceLedger(majorResultsMap, traceSheets);
    }
}
