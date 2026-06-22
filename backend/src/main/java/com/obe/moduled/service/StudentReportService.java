package com.obe.moduled.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.BizException;
import com.obe.engine.Level1Calculator;
import com.obe.engine.Level2Calculator;
import com.obe.modulea.entity.*;
import com.obe.modulea.mapper.*;
import com.obe.moduleb.entity.*;
import com.obe.moduleb.mapper.*;
import com.obe.modulec.entity.ScoreSheet;
import com.obe.modulec.entity.StudentScore;
import com.obe.modulec.mapper.ScoreSheetMapper;
import com.obe.modulec.mapper.StudentScoreMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentReportService {

    private final StudentMapper studentMapper;
    private final ClassStudentMapper classStudentMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final CourseMapper courseMapper;
    private final ScoreSheetMapper scoreSheetMapper;
    private final StudentScoreMapper studentScoreMapper;
    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;
    private final AssessmentPointMapper assessmentPointMapper;
    private final ObjectiveIndicatorWeightMapper weightMapper;
    private final IndicatorMapper indicatorMapper;
    private final AssessmentObjectiveMapper assessmentObjectiveMapper;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final QuestionObjectiveMapper questionObjectiveMapper;
    private final SysMajorMapper majorMapper;
    private final SysCollegeMapper collegeMapper;

    // ============ Data DTOs ============

    public record StudentAchievementData(
            StudentInfo student,
            List<CourseAchievementItem> courses,
            Map<String, BigDecimal> indicatorAchievements,
            Map<String, String> indicatorNames,
            Map<String, Long> indicatorIds,
            LocalDateTime generatedAt
    ) {}

    public record StudentInfo(Long id, String name, String studentNo, String major, String college) {}

    public record CourseAchievementItem(
            Long courseId, String courseCode, String courseName,
            String className,
            Map<Long, BigDecimal> objectiveAchievements,
            Map<Long, BigDecimal> indicatorAchievements,
            Map<Long, String> objectiveLabels,
            Map<Long, String> objectiveDescs,
            Map<Long, String> indicatorLabels,
            Map<Long, String> indicatorDescs
    ) {}

    // ============ Main computation ============

    public StudentAchievementData getStudentAchievement(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(404, "学生不存在");
        }

        // Student info
        SysMajor major = null;
        SysCollege college = null;
        if (student.getMajorId() != null) {
            major = majorMapper.selectById(student.getMajorId());
        }
        if (student.getCollegeId() != null) {
            college = collegeMapper.selectById(student.getCollegeId());
        }
        if (college == null && major != null && major.getCollegeId() != null) {
            college = collegeMapper.selectById(major.getCollegeId());
        }
        StudentInfo info = new StudentInfo(
                student.getId(), student.getName(), student.getStudentNo(),
                major != null ? major.getName() : "", college != null ? college.getName() : "");

        // Find all classes this student is in
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getStudentId, studentId));
        if (classStudents.isEmpty()) {
            return new StudentAchievementData(info, List.of(), Map.of(), Map.of(), Map.of(), LocalDateTime.now());
        }

        // Compute per-course achievements
        List<CourseAchievementItem> courses = new ArrayList<>();
        Map<Long, List<BigDecimal>> indicatorValues = new HashMap<>(); // indicatorId -> per-course achievements
        Map<Long, String> indicatorNameMap = new HashMap<>();

        for (ClassStudent cs : classStudents) {
            try {
                CourseAchievementItem item = computeCourseAchievementForStudent(cs.getClassId(), studentId);
                if (item != null) {
                    courses.add(item);
                    for (var entry : item.indicatorAchievements().entrySet()) {
                        Long indId = entry.getKey();
                        indicatorValues.computeIfAbsent(indId, k -> new ArrayList<>()).add(entry.getValue());
                        if (!indicatorNameMap.containsKey(indId)) {
                            var ind = indicatorMapper.selectById(indId);
                            indicatorNameMap.put(indId, ind != null ? ind.getContent() : "");
                        }
                    }
                }
            } catch (Exception ignored) {
                // Skip classes without valid calculation data
            }
        }

        // Aggregate indicator achievements across courses (simple average)
        Map<String, BigDecimal> aggregated = new LinkedHashMap<>();
        Map<String, String> aggregatedNames = new LinkedHashMap<>();
        Map<String, Long> aggregatedIds = new LinkedHashMap<>();
        for (var entry : indicatorValues.entrySet()) {
            Long indId = entry.getKey();
            var ind = indicatorMapper.selectById(indId);
            String key = ind != null ? ind.getIndicatorNo() : "指标" + indId;
            BigDecimal avg = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 4, RoundingMode.HALF_UP);
            aggregated.put(key, avg);
            aggregatedNames.put(key, indicatorNameMap.getOrDefault(indId, key));
            aggregatedIds.put(key, indId);
        }

        return new StudentAchievementData(info, courses, aggregated, aggregatedNames, aggregatedIds, LocalDateTime.now());
    }

    private CourseAchievementItem computeCourseAchievementForStudent(Long classId, Long studentId) {
        // 1. Validate score sheet
        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, classId));
        if (sheet == null || !"LOCKED".equals(sheet.getStatus())) {
            return null;
        }

        // 2. Load course outline and teaching class info
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        if (outline == null) return null;

        TeachingClass tc = teachingClassMapper.selectById(classId);
        if (tc == null) return null;

        Course course = courseMapper.selectById(tc.getCourseId());
        if (course == null) return null;

        // 3. Load objectives, assessments, scores for this student
        List<CourseObjective> objectives = objectiveMapper.selectList(
                new LambdaQueryWrapper<CourseObjective>().eq(CourseObjective::getOutlineId, outline.getId()));
        if (objectives.isEmpty()) return null;

        List<AssessmentPoint> assessments = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>().eq(AssessmentPoint::getOutlineId, outline.getId()));
        if (assessments.isEmpty()) return null;

        List<StudentScore> scores = studentScoreMapper.selectList(
                new LambdaQueryWrapper<StudentScore>()
                        .eq(StudentScore::getSheetId, sheet.getId())
                        .eq(StudentScore::getStudentId, studentId));
        if (scores.isEmpty()) return null;

        // 4. Build N:M bindings
        List<Long> assessmentIds = assessments.stream().map(AssessmentPoint::getId).toList();
        List<AssessmentObjective> aoList = assessmentObjectiveMapper.selectList(
                new LambdaQueryWrapper<AssessmentObjective>().in(AssessmentObjective::getAssessmentId, assessmentIds));
        Map<Long, List<Long>> assessmentObjMap = new HashMap<>();
        for (var ao : aoList) {
            assessmentObjMap.computeIfAbsent(ao.getAssessmentId(), k -> new ArrayList<>()).add(ao.getObjectiveId());
        }
        for (AssessmentPoint ap : assessments) {
            if (!assessmentObjMap.containsKey(ap.getId()) && ap.getObjectiveId() != null) {
                assessmentObjMap.put(ap.getId(), List.of(ap.getObjectiveId()));
            }
        }
        if (assessmentObjMap.isEmpty()) return null;

        // 5. Build max-score maps
        Map<Long, BigDecimal> assessmentMaxMap = new HashMap<>();
        for (var ap : assessments) assessmentMaxMap.put(ap.getId(), ap.getMaxScore());

        List<AssessmentQuestion> questions = assessmentQuestionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>().in(AssessmentQuestion::getAssessmentId, assessmentIds));
        Map<Long, BigDecimal> questionMaxMap = new HashMap<>();
        for (var q : questions) questionMaxMap.put(q.getId(), q.getMaxScore());

        Map<Long, List<Long>> questionObjMap = new HashMap<>();
        if (!questions.isEmpty()) {
            List<Long> qIds = questions.stream().map(AssessmentQuestion::getId).toList();
            List<QuestionObjective> qoList = questionObjectiveMapper.selectList(
                    new LambdaQueryWrapper<QuestionObjective>().in(QuestionObjective::getQuestionId, qIds));
            for (var qo : qoList) {
                questionObjMap.computeIfAbsent(qo.getQuestionId(), k -> new ArrayList<>()).add(qo.getObjectiveId());
            }
        }

        // 6. Compute per-student objective achievements
        Map<Long, BigDecimal> objAchievements = new LinkedHashMap<>();
        for (CourseObjective obj : objectives) {
            BigDecimal achievement = calcSingleStudentObjectiveAchievement(
                    scores, obj.getId(), assessmentMaxMap, assessmentObjMap, questionMaxMap, questionObjMap);
            objAchievements.put(obj.getId(), achievement);
        }

        // 7. Compute indicator achievements via weights
        List<ObjectiveIndicatorWeight> weights = weightMapper.selectList(
                new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                        .in(ObjectiveIndicatorWeight::getObjectiveId,
                                objectives.stream().map(CourseObjective::getId).toList()));
        List<Level2Calculator.WeightRecord> weightRecords = weights.stream()
                .map(w -> new Level2Calculator.WeightRecord(w.getObjectiveId(), w.getIndicatorId(), w.getWeight()))
                .toList();
        Map<Long, BigDecimal> indicatorAchievements = new LinkedHashMap<>();
        for (var wr : weightRecords) {
            BigDecimal objVal = objAchievements.getOrDefault(wr.objectiveId(), BigDecimal.ZERO);
            BigDecimal contrib = objVal.multiply(wr.weight());
            indicatorAchievements.merge(wr.indicatorId(), contrib, BigDecimal::add);
        }

        // 8. Build labels
        Map<Long, String> objLabels = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getObjNo));
        Map<Long, String> objDescs = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, o -> o.getDescription() != null ? o.getDescription() : o.getObjNo()));
        Map<Long, String> indLabels = new HashMap<>();
        Map<Long, String> indDescs = new HashMap<>();
        if (!indicatorAchievements.isEmpty()) {
            var inds = indicatorMapper.selectBatchIds(indicatorAchievements.keySet());
            for (var ind : inds) {
                indLabels.put(ind.getId(), ind.getIndicatorNo());
                indDescs.put(ind.getId(), ind.getContent() != null ? ind.getContent() : ind.getIndicatorNo());
            }
        }

        return new CourseAchievementItem(
                course.getId(), course.getCode(), course.getName(),
                tc.getClassName(), objAchievements, indicatorAchievements,
                objLabels, objDescs, indLabels, indDescs);
    }

    private BigDecimal calcSingleStudentObjectiveAchievement(
            List<StudentScore> scores, Long objectiveId,
            Map<Long, BigDecimal> assessmentMaxMap,
            Map<Long, List<Long>> assessmentObjMap,
            Map<Long, BigDecimal> questionMaxMap,
            Map<Long, List<Long>> questionObjMap) {

        List<Level1Calculator.StudentScoreRecord> records = scores.stream()
                .map(s -> new Level1Calculator.StudentScoreRecord(
                        s.getStudentId(), s.getAssessmentId(), s.getQuestionId(), s.getScore()))
                .toList();

        // Find relevant assessments/questions for this objective
        Set<Long> relevantAssessIds = new HashSet<>();
        Set<Long> relevantQuestionIds = new HashSet<>();
        for (var e : assessmentObjMap.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(objectiveId)) {
                relevantAssessIds.add(e.getKey());
            }
        }
        for (var e : questionObjMap.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(objectiveId)) {
                relevantQuestionIds.add(e.getKey());
            }
        }
        if (relevantAssessIds.isEmpty() && relevantQuestionIds.isEmpty()) return BigDecimal.ZERO;

        // Determine which assessments have question-level data
        Set<Long> questionedAssessments = new HashSet<>();
        for (var r : records) {
            if (r.questionId() != null && relevantQuestionIds.contains(r.questionId())) {
                questionedAssessments.add(r.assessmentId());
            }
        }

        BigDecimal actualSum = BigDecimal.ZERO;
        BigDecimal maxSum = BigDecimal.ZERO;

        for (var r : records) {
            if (r.questionId() != null && relevantQuestionIds.contains(r.questionId())) {
                actualSum = actualSum.add(r.score());
                BigDecimal qMax = questionMaxMap.get(r.questionId());
                if (qMax != null) maxSum = maxSum.add(qMax);
            } else if (r.questionId() == null && r.assessmentId() != null
                    && relevantAssessIds.contains(r.assessmentId())
                    && !questionedAssessments.contains(r.assessmentId())) {
                actualSum = actualSum.add(r.score());
                BigDecimal aMax = assessmentMaxMap.get(r.assessmentId());
                if (aMax != null) maxSum = maxSum.add(aMax);
            }
        }

        return maxSum.compareTo(BigDecimal.ZERO) > 0
                ? actualSum.divide(maxSum, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    // ============ Excel export ============

    public byte[] generateExcel(Long studentId) {
        StudentAchievementData data = getStudentAchievement(studentId);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle normal = normalStyle(wb);
            CellStyle bold = boldStyle(wb);

            // ==== Sheet 1: 达成度总览 ====
            Sheet s1 = wb.createSheet("达成度总览");
            int r = 0;
            titleRow(s1, r++, "学生个人毕业要求达成度报告", header);
            infoRow(s1, r++, data, normal);
            r++;
            // 指标总览表头
            Row hr = s1.createRow(r++);
            String[] h1 = {"指标点编号", "指标点描述", "达成度"};
            for (int i = 0; i < 3; i++) cell(hr, i, h1[i], header);
            for (var e : data.indicatorAchievements().entrySet()) {
                Row row = s1.createRow(r++);
                cell(row, 0, e.getKey(), normal);
                cell(row, 1, data.indicatorNames().getOrDefault(e.getKey(), ""), normal);
                cell(row, 2, e.getValue().setScale(4, RoundingMode.HALF_UP).toString(), normal);
            }
            s1.setColumnWidth(0, 4000); s1.setColumnWidth(1, 14000); s1.setColumnWidth(2, 3000);

            // ==== Sheet 2: 各课程达成度明细 ====
            Sheet s2 = wb.createSheet("课程达成度明细");
            int r2 = 0;
            for (var course : data.courses()) {
                // 课程标题行
                Row ct = s2.createRow(r2++);
                cell(ct, 0, course.courseCode() + " " + course.courseName() + " (" + course.className() + ")", bold);
                r2++;
                // 课程目标表
                Row oh = s2.createRow(r2++);
                cell(oh, 0, "目标编号", header); cell(oh, 1, "目标描述", header); cell(oh, 2, "达成度", header);
                for (var oe : course.objectiveAchievements().entrySet()) {
                    Row or2 = s2.createRow(r2++);
                    cell(or2, 0, course.objectiveLabels().getOrDefault(oe.getKey(), ""), normal);
                    cell(or2, 1, course.objectiveDescs().getOrDefault(oe.getKey(), ""), normal);
                    cell(or2, 2, oe.getValue().setScale(4, RoundingMode.HALF_UP).toString(), normal);
                }
                r2++;
                // 指标点表
                Row ih = s2.createRow(r2++);
                cell(ih, 0, "指标点编号", header); cell(ih, 1, "指标点描述", header); cell(ih, 2, "达成度", header);
                for (var ie : course.indicatorAchievements().entrySet()) {
                    Row ir2 = s2.createRow(r2++);
                    cell(ir2, 0, course.indicatorLabels().getOrDefault(ie.getKey(), ""), normal);
                    cell(ir2, 1, course.indicatorDescs().getOrDefault(ie.getKey(), ""), normal);
                    cell(ir2, 2, ie.getValue().setScale(4, RoundingMode.HALF_UP).toString(), normal);
                }
                r2 += 2;
            }
            s2.setColumnWidth(0, 4000); s2.setColumnWidth(1, 14000); s2.setColumnWidth(2, 3000);

            // ==== Sheet 3: 指标钻取溯源 ====
            Sheet s3 = wb.createSheet("指标溯源");
            int r3 = 0;
            for (var indEntry : data.indicatorAchievements().entrySet()) {
                String indNo = indEntry.getKey();
                Long indId = data.indicatorIds().get(indNo);
                if (indId == null) continue;
                IndicatorTraceData drill = getIndicatorTrace(studentId, indId);
                // 指标标题
                Row dr = s3.createRow(r3++);
                cell(dr, 0, indNo + " " + drill.indicatorContent() + "  总达成度: " + drill.overallAchievement().setScale(4, RoundingMode.HALF_UP), bold);
                for (var cd : drill.courses()) {
                    Row cr3 = s3.createRow(r3++);
                    cell(cr3, 0, "  " + cd.courseCode() + " " + cd.courseName() + " (" + cd.className() + ")  达成度: " + cd.indicatorAchievement().setScale(4, RoundingMode.HALF_UP), bold);
                    Row oh3 = s3.createRow(r3++);
                    cell(oh3, 0, "目标", header); cell(oh3, 1, "权重", header); cell(oh3, 2, "目标达成度", header);
                    cell(oh3, 3, "贡献值", header); cell(oh3, 4, "考核点及成绩", header);
                    for (var od : cd.objectives()) {
                        Row odr = s3.createRow(r3++);
                        cell(odr, 0, od.objNo(), normal);
                        cell(odr, 1, od.weight().setScale(4, RoundingMode.HALF_UP).toString(), normal);
                        cell(odr, 2, od.objAchievement().setScale(4, RoundingMode.HALF_UP).toString(), normal);
                        cell(odr, 3, od.weight().multiply(od.objAchievement()).setScale(4, RoundingMode.HALF_UP).toString(), normal);
                        StringBuilder sb = new StringBuilder();
                        for (var s : od.scores()) {
                            sb.append(s.assessmentName());
                            if (s.questionContent() != null && !s.questionContent().isEmpty())
                                sb.append("-").append(s.questionContent());
                            sb.append(": ").append(s.score());
                            if (s.maxScore().compareTo(BigDecimal.ZERO) > 0)
                                sb.append("/").append(s.maxScore());
                            sb.append("  ");
                        }
                        cell(odr, 4, sb.toString(), normal);
                    }
                    r3++;
                }
                r3++;
            }
            s3.setColumnWidth(0, 3000); s3.setColumnWidth(1, 2000); s3.setColumnWidth(2, 3000);
            s3.setColumnWidth(3, 3000); s3.setColumnWidth(4, 12000);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成Excel失败: " + e.getMessage());
        }
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 11);
        s.setFont(f); s.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(s); return s;
    }
    private CellStyle normalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setFontHeightInPoints((short) 10);
        s.setFont(f); setBorders(s); return s;
    }
    private CellStyle boldStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 11); f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f); setBorders(s); return s;
    }
    private void setBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
    }
    private void cell(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }
    private void titleRow(Sheet sheet, int rowIdx, String text, CellStyle style) {
        Row r = sheet.createRow(rowIdx); cell(r, 0, text, style);
    }
    private void infoRow(Sheet sheet, int rowIdx, StudentAchievementData data, CellStyle style) {
        Row r = sheet.createRow(rowIdx);
        cell(r, 0, "姓名: " + data.student().name(), style);
        cell(r, 1, "学号: " + data.student().studentNo(), style);
        cell(r, 2, "专业: " + data.student().major(), style);
        cell(r, 3, "学院: " + data.student().college(), style);
        Row r2 = sheet.createRow(rowIdx + 1);
        cell(r2, 0, "生成时间: " + data.generatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), style);
    }

    // ============ Word export ============

    public byte[] generateWord(Long studentId) {
        StudentAchievementData data = getStudentAchievement(studentId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            addTitle(doc, "学生个人毕业要求达成度报告");
            addParagraph(doc, String.format("姓名: %s    学号: %s    专业: %s    学院: %s",
                    data.student().name(), data.student().studentNo(), data.student().major(), data.student().college()), 12, false);
            addParagraph(doc, "生成时间: " + data.generatedAt().format(dtf), 9, false, "999999");

            // 一、总览
            addSection(doc, "一、毕业要求指标点达成度总览");
            XWPFTable t1 = doc.createTable(data.indicatorAchievements().size() + 1, 3);
            setTableWidth(t1);
            String[] th = {"指标点编号", "指标点描述", "达成度"};
            for (int i = 0; i < 3; i++) setCell(t1.getRow(0).getCell(i), th[i], true, 10);
            int ri = 1;
            for (var e : data.indicatorAchievements().entrySet()) {
                setCell(t1.getRow(ri).getCell(0), e.getKey(), false, 10);
                setCell(t1.getRow(ri).getCell(1), data.indicatorNames().getOrDefault(e.getKey(), ""), false, 10);
                setCell(t1.getRow(ri).getCell(2), e.getValue().setScale(4, RoundingMode.HALF_UP).toString(), false, 10);
                ri++;
            }

            // 二、各课程明细
            addSection(doc, "二、各课程达成度明细");
            for (var course : data.courses()) {
                addParagraph(doc, course.courseCode() + " " + course.courseName() + " (" + course.className() + ")", 11, true);
                // 课程目标
                addParagraph(doc, "  课程目标达成度:", 10, true);
                if (!course.objectiveAchievements().isEmpty()) {
                    XWPFTable ot = doc.createTable(course.objectiveAchievements().size() + 1, 3);
                    setTableWidth(ot);
                    setCell(ot.getRow(0).getCell(0), "编号", true, 9);
                    setCell(ot.getRow(0).getCell(1), "描述", true, 9);
                    setCell(ot.getRow(0).getCell(2), "达成度", true, 9);
                    int oi = 1;
                    for (var oe : course.objectiveAchievements().entrySet()) {
                        setCell(ot.getRow(oi).getCell(0), course.objectiveLabels().getOrDefault(oe.getKey(), ""), false, 9);
                        setCell(ot.getRow(oi).getCell(1), course.objectiveDescs().getOrDefault(oe.getKey(), ""), false, 9);
                        setCell(ot.getRow(oi).getCell(2), oe.getValue().setScale(4, RoundingMode.HALF_UP).toString(), false, 9);
                        oi++;
                    }
                }
                // 指标点
                addParagraph(doc, "  指标点达成度:", 10, true);
                if (!course.indicatorAchievements().isEmpty()) {
                    XWPFTable it = doc.createTable(course.indicatorAchievements().size() + 1, 3);
                    setTableWidth(it);
                    setCell(it.getRow(0).getCell(0), "编号", true, 9);
                    setCell(it.getRow(0).getCell(1), "描述", true, 9);
                    setCell(it.getRow(0).getCell(2), "达成度", true, 9);
                    int ii = 1;
                    for (var ie : course.indicatorAchievements().entrySet()) {
                        setCell(it.getRow(ii).getCell(0), course.indicatorLabels().getOrDefault(ie.getKey(), ""), false, 9);
                        setCell(it.getRow(ii).getCell(1), course.indicatorDescs().getOrDefault(ie.getKey(), ""), false, 9);
                        setCell(it.getRow(ii).getCell(2), ie.getValue().setScale(4, RoundingMode.HALF_UP).toString(), false, 9);
                        ii++;
                    }
                }
                addParagraph(doc, "", 6, false);
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成Word失败: " + e.getMessage());
        }
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph(); p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun(); r.setText(text); r.setBold(true); r.setFontSize(18); r.setFontFamily("SimSun");
    }
    private void addSection(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph(); p.setSpacingBefore(240);
        XWPFRun r = p.createRun(); r.setText(text); r.setBold(true); r.setFontSize(13);
    }
    private void addParagraph(XWPFDocument doc, String text, int fontSize, boolean bold) {
        addParagraph(doc, text, fontSize, bold, null);
    }
    private void addParagraph(XWPFDocument doc, String text, int fontSize, boolean bold, String color) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun(); r.setText(text); r.setBold(bold); r.setFontSize(fontSize);
        if (color != null) r.setColor(color);
    }
    private void setCell(XWPFTableCell cell, String text, boolean bold, int fontSize) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun(); r.setText(text); r.setBold(bold); r.setFontSize(fontSize);
    }
    private void setTableWidth(XWPFTable table) {
        table.setWidth("100%");
    }

    // ============ Drill-down ============

    public record IndicatorTraceData(
            String indicatorNo, String indicatorContent,
            BigDecimal overallAchievement,
            List<CourseDrillItem> courses
    ) {}

    public record CourseDrillItem(
            String courseCode, String courseName, String className,
            BigDecimal indicatorAchievement,
            List<ObjDrillItem> objectives
    ) {}

    public record ObjDrillItem(
            String objNo, BigDecimal weight, BigDecimal objAchievement,
            List<ScoreDrillItem> scores
    ) {}

    public record ScoreDrillItem(
            String assessmentName, String questionContent,
            BigDecimal score, BigDecimal maxScore
    ) {}

    public IndicatorTraceData getIndicatorTrace(Long studentId, Long indicatorId) {
        StudentAchievementData base = getStudentAchievement(studentId);
        var indicator = indicatorMapper.selectById(indicatorId);
        if (indicator == null) throw new BizException(404, "指标点不存在");

        BigDecimal overall = base.indicatorAchievements().getOrDefault(
                indicator.getIndicatorNo(), BigDecimal.ZERO);

        List<CourseDrillItem> courses = new ArrayList<>();

        for (CourseAchievementItem courseItem : base.courses()) {
            BigDecimal courseAchievement = courseItem.indicatorAchievements()
                    .getOrDefault(indicatorId, BigDecimal.ZERO);
            if (courseAchievement.compareTo(BigDecimal.ZERO) == 0) continue;

            // Find objectives that contribute to this indicator
            List<ObjectiveIndicatorWeight> weights = weightMapper.selectList(
                    new LambdaQueryWrapper<ObjectiveIndicatorWeight>()
                            .eq(ObjectiveIndicatorWeight::getIndicatorId, indicatorId));
            Set<Long> contributingObjIds = weights.stream()
                    .map(ObjectiveIndicatorWeight::getObjectiveId).collect(Collectors.toSet());

            List<ObjDrillItem> objs = new ArrayList<>();
            for (var objEntry : courseItem.objectiveAchievements().entrySet()) {
                if (!contributingObjIds.contains(objEntry.getKey())) continue;
                BigDecimal w = weights.stream()
                        .filter(x -> x.getObjectiveId().equals(objEntry.getKey()))
                        .findFirst().map(ObjectiveIndicatorWeight::getWeight)
                        .orElse(BigDecimal.ZERO);
                String objLabel = courseItem.objectiveLabels()
                        .getOrDefault(objEntry.getKey(), "目标" + objEntry.getKey());

                // Get assessment scores for this student in this class
                List<ScoreDrillItem> scoreItems = new ArrayList<>();
                try {
                    scoreItems = getStudentScoreDetail(studentId, courseItem.className, objEntry.getKey());
                } catch (Exception ignored) {}

                objs.add(new ObjDrillItem(objLabel, w, objEntry.getValue(), scoreItems));
            }
            if (!objs.isEmpty()) {
                courses.add(new CourseDrillItem(courseItem.courseCode(), courseItem.courseName(),
                        courseItem.className(), courseAchievement, objs));
            }
        }

        return new IndicatorTraceData(indicator.getIndicatorNo(), indicator.getContent(),
                overall, courses);
    }

    private List<ScoreDrillItem> getStudentScoreDetail(Long studentId, String className, Long objectiveId) {
        // Find the teaching class by name, then get scores
        TeachingClass tc = teachingClassMapper.selectOne(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getClassName, className));
        if (tc == null) return List.of();

        ScoreSheet sheet = scoreSheetMapper.selectOne(
                new LambdaQueryWrapper<ScoreSheet>().eq(ScoreSheet::getClassId, tc.getId()));
        if (sheet == null) return List.of();

        List<StudentScore> scores = studentScoreMapper.selectList(
                new LambdaQueryWrapper<StudentScore>()
                        .eq(StudentScore::getSheetId, sheet.getId())
                        .eq(StudentScore::getStudentId, studentId));
        if (scores.isEmpty()) return List.of();

        // Get assessment info
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, tc.getId()));
        if (outline == null) return List.of();

        List<AssessmentPoint> points = assessmentPointMapper.selectList(
                new LambdaQueryWrapper<AssessmentPoint>().eq(AssessmentPoint::getOutlineId, outline.getId()));

        // Find assessments linked to this objective
        List<AssessmentObjective> aoList = assessmentObjectiveMapper.selectList(
                new LambdaQueryWrapper<AssessmentObjective>()
                        .eq(AssessmentObjective::getObjectiveId, objectiveId));
        Set<Long> relevantAssessIds = aoList.stream()
                .map(AssessmentObjective::getAssessmentId).collect(Collectors.toSet());
        for (var ap : points) {
            if (ap.getObjectiveId() != null && ap.getObjectiveId().equals(objectiveId)) {
                relevantAssessIds.add(ap.getId());
            }
        }

        Map<Long, String> assessNames = points.stream()
                .collect(Collectors.toMap(AssessmentPoint::getId, AssessmentPoint::getName));

        // Get questions
        List<AssessmentQuestion> questions = assessmentQuestionMapper.selectList(
                new LambdaQueryWrapper<AssessmentQuestion>()
                        .in(AssessmentQuestion::getAssessmentId, new ArrayList<>(relevantAssessIds)));
        Map<Long, String> questionContents = questions.stream()
                .collect(Collectors.toMap(AssessmentQuestion::getId, AssessmentQuestion::getName));
        Map<Long, BigDecimal> questionMaxMap = questions.stream()
                .collect(Collectors.toMap(AssessmentQuestion::getId, AssessmentQuestion::getMaxScore));

        List<ScoreDrillItem> result = new ArrayList<>();
        for (var score : scores) {
            if (!relevantAssessIds.contains(score.getAssessmentId())) continue;
            String assessName = assessNames.getOrDefault(score.getAssessmentId(), "考核点" + score.getAssessmentId());
            String qContent = score.getQuestionId() != null
                    ? questionContents.getOrDefault(score.getQuestionId(), "题目" + score.getQuestionId())
                    : "";
            BigDecimal maxScore = score.getQuestionId() != null
                    ? questionMaxMap.getOrDefault(score.getQuestionId(), BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            result.add(new ScoreDrillItem(assessName, qContent, score.getScore(), maxScore));
        }
        return result;
    }
}
