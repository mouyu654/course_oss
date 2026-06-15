package com.obe.moduled.exporter;

import com.obe.common.BizException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Drill-through Excel ledger exporter for major-level report.
 * Creates a multi-sheet workbook where each sheet corresponds to one indicator point,
 * showing the computation trace from course → indicator → objective → assessment scores.
 */
public class TraceExcelExporter {

    /**
     * Generate the drill-through Excel ledger.
     *
     * @param indicatorResults      Map<indicatorNo, G_k> — major-level results
     * @param indicatorTraceData    per-indicator trace data
     * @return Excel byte array
     */
    public static byte[] generateTraceLedger(
            Map<String, BigDecimal> indicatorResults,
            List<IndicatorTraceSheet> indicatorTraceData) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Summary sheet
            Sheet summarySheet = workbook.createSheet("专业级达成度汇总");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = summarySheet.createRow(0);
            headerRow.createCell(0).setCellValue("指标点");
            headerRow.createCell(1).setCellValue("专业级达成度 G_k");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);

            int rowIdx = 1;
            for (Map.Entry<String, BigDecimal> e : indicatorResults.entrySet()) {
                Row row = summarySheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue().doubleValue());
                row.getCell(0).setCellStyle(dataStyle);
                row.getCell(1).setCellStyle(dataStyle);
            }

            // Per-indicator drill-down sheets
            for (IndicatorTraceSheet trace : indicatorTraceData) {
                Sheet sheet = workbook.createSheet("指标点" + trace.indicatorNo());
                Row hRow = sheet.createRow(0);
                String[] colHeaders = {"课程", "课程级达成度 E_k", "总支撑权重 W_c",
                        "课程目标", "目标达成度", "内部权重 w_jk", "考核点", "满分", "平均得分"};
                for (int i = 0; i < colHeaders.length; i++) {
                    Cell cell = hRow.createCell(i);
                    cell.setCellValue(colHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                int r = 1;
                for (TraceRow tr : trace.rows()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(tr.courseName());
                    row.createCell(1).setCellValue(tr.ek() != null ? tr.ek().doubleValue() : 0);
                    row.createCell(2).setCellValue(tr.wc() != null ? tr.wc().doubleValue() : 0);
                    row.createCell(3).setCellValue(tr.objectiveNo());
                    row.createCell(4).setCellValue(tr.objAchievement() != null ? tr.objAchievement().doubleValue() : 0);
                    row.createCell(5).setCellValue(tr.wjk() != null ? tr.wjk().doubleValue() : 0);
                    row.createCell(6).setCellValue(tr.assessmentName());
                    row.createCell(7).setCellValue(tr.maxScore() != null ? tr.maxScore().doubleValue() : 0);
                    row.createCell(8).setCellValue(tr.avgScore() != null ? tr.avgScore().doubleValue() : 0);
                    for (int i = 0; i < colHeaders.length; i++) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }

                // Auto-size columns
                for (int i = 0; i < colHeaders.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成穿透式台账失败: " + e.getMessage());
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /** Generate a course-level detail report with 4 sheets */
    public static byte[] generateCourseDetailReport(CourseReportData data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle h = createHeaderStyle(workbook);
            CellStyle d = createDataStyle(workbook);
            CellStyle pass = createPassFailStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle fail = createPassFailStyle(workbook, IndexedColors.ROSE);

            // === Sheet 1: 学生成绩明细 ===
            Sheet s1 = workbook.createSheet("学生成绩明细");
            int c = 0;
            Row r1 = s1.createRow(0);
            for (String t : new String[]{"学号", "姓名"}) { Cell cell = r1.createCell(c++); cell.setCellValue(t); cell.setCellStyle(h); }
            for (AssessmentInfo ai : data.assessments()) { Cell cell = r1.createCell(c++); cell.setCellValue(ai.name() + "(" + ai.maxScore() + ")"); cell.setCellStyle(h); }
            r1.createCell(c).setCellValue("总分"); r1.getCell(c++).setCellStyle(h);
            r1.createCell(c).setCellValue("得分率"); r1.getCell(c).setCellStyle(h);
            int sc = 1;
            for (StudentScoreRow sr : data.studentScores()) {
                Row row = s1.createRow(sc++); c = 0;
                row.createCell(c++).setCellValue(sr.studentNo()); row.getCell(0).setCellStyle(d);
                row.createCell(c++).setCellValue(sr.studentName()); row.getCell(1).setCellStyle(d);
                double tScore = 0, tMax = 0;
                for (AssessmentInfo ai : data.assessments()) {
                    Double s = sr.scores().get(ai.id());
                    Cell cell = row.createCell(c++);
                    if (s != null) { cell.setCellValue(s); tScore += s; tMax += ai.maxScore(); cell.setCellStyle(d); }
                    else { cell.setCellValue("缺"); cell.setCellStyle(fail); }
                }
                row.createCell(c++).setCellValue(Math.round(tScore * 100.0) / 100.0); row.getCell(c - 1).setCellStyle(d);
                double pct = tMax > 0 ? tScore / tMax : 0;
                Cell pc = row.createCell(c++); pc.setCellValue(Math.round(pct * 10000.0) / 100.0 + "%"); pc.setCellStyle(pct >= 0.6 ? pass : fail);
            }
            for (int i = 0; i <= c; i++) s1.autoSizeColumn(i);

            // === Sheet 2: 学生课程目标达成度 (per-student C_ij) ===
            Sheet s2 = workbook.createSheet("学生课程目标达成度");
            int c2 = 0;
            Row r2 = s2.createRow(0);
            r2.createCell(c2++).setCellValue("学号"); r2.getCell(0).setCellStyle(h);
            r2.createCell(c2++).setCellValue("姓名"); r2.getCell(1).setCellStyle(h);
            for (String objLabel : data.objectiveLabels()) { Cell cell = r2.createCell(c2++); cell.setCellValue(objLabel); cell.setCellStyle(h); }
            int s2r = 1;
            for (var ps : data.perStudentObjectives()) {
                Row row = s2.createRow(s2r++);
                int cc = 0;
                row.createCell(cc++).setCellValue(ps.studentNo()); row.getCell(0).setCellStyle(d);
                row.createCell(cc++).setCellValue(ps.studentName()); row.getCell(1).setCellStyle(d);
                for (int i = 0; i < data.objectiveLabels().size(); i++) {
                    Double ach = ps.achievements().size() > i ? ps.achievements().get(i) : null;
                    Cell cell = row.createCell(cc++);
                    cell.setCellValue(ach != null ? ach : 0.0);
                    cell.setCellStyle(ach != null && ach >= 0.6 ? pass : ach != null ? fail : d);
                }
            }
            for (int i = 0; i <= c2; i++) s2.autoSizeColumn(i);

            // === Sheet 3: 课程目标达成度 ===
            Sheet s3 = workbook.createSheet("课程目标达成度");
            Row r3 = s3.createRow(0);
            String[] oh = {"目标编号", "目标描述", "班级平均达成度", "达标状态"};
            for (int i = 0; i < oh.length; i++) { Cell cell = r3.createCell(i); cell.setCellValue(oh[i]); cell.setCellStyle(h); }
            int s3r = 1;
            for (ObjectiveResult or : data.objectiveResults()) {
                Row row = s3.createRow(s3r++);
                row.createCell(0).setCellValue(or.objNo()); row.getCell(0).setCellStyle(d);
                row.createCell(1).setCellValue(or.description()); row.getCell(1).setCellStyle(d);
                double ach = or.achievement() != null ? or.achievement().doubleValue() : 0;
                row.createCell(2).setCellValue(ach); row.getCell(2).setCellStyle(d);
                boolean ps = or.achievement() != null && or.achievement().compareTo(new BigDecimal("0.6")) >= 0;
                row.createCell(3).setCellValue(ps ? "达标" : "未达标"); row.getCell(3).setCellStyle(ps ? pass : fail);
            }
            for (int i = 0; i < oh.length; i++) s3.autoSizeColumn(i);

            // === Sheet 4: 指标点达成度 + 综合汇总 ===
            Sheet s4 = workbook.createSheet("指标点达成度");
            Row r4 = s4.createRow(0);
            String[] ih = {"指标点编号", "课程级达成度 E_k", "达标状态", "相关课程目标"};
            for (int i = 0; i < ih.length; i++) { Cell cell = r4.createCell(i); cell.setCellValue(ih[i]); cell.setCellStyle(h); }
            int s4r = 1, passC = 0, totalC = 0;
            for (IndicatorResult ir : data.indicatorResults()) {
                Row row = s4.createRow(s4r++);
                row.createCell(0).setCellValue(ir.indicatorNo()); row.getCell(0).setCellStyle(d);
                double ach = ir.achievement() != null ? ir.achievement().doubleValue() : 0;
                row.createCell(1).setCellValue(ach); row.getCell(1).setCellStyle(d);
                boolean ps = ir.achievement() != null && ir.achievement().compareTo(new BigDecimal("0.6")) >= 0;
                row.createCell(2).setCellValue(ps ? "达标" : "未达标"); row.getCell(2).setCellStyle(ps ? pass : fail);
                row.createCell(3).setCellValue(ir.relatedObjectives() != null ? ir.relatedObjectives() : ""); row.getCell(3).setCellStyle(d);
                if (ps) passC++; totalC++;
            }
            s4r++;
            double rate = totalC > 0 ? (double) passC / totalC * 100 : 0;
            double avgSum = data.indicatorResults().stream().mapToDouble(ir -> ir.achievement() != null ? ir.achievement().doubleValue() : 0).sum();
            double avg = totalC > 0 ? avgSum / totalC : 0;
            for (String[] sr : new String[][]{{"综合汇总", "", "", ""}, {"达标率", passC + "/" + totalC + " = " + String.format("%.1f%%", rate), "", ""}, {"平均达成度", String.format("%.4f", avg), "", ""}, {"课程", data.courseName() + " — " + data.className(), "", ""}}) {
                Row row = s4.createRow(s4r++);
                for (int i = 0; i < sr.length; i++) { Cell cell = row.createCell(i); cell.setCellValue(sr[i]); cell.setCellStyle(h); }
            }
            for (int i = 0; i < ih.length; i++) s4.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成课程报告失败: " + e.getMessage());
        }
    }

    private static CellStyle createPassFailStyle(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    public record CourseReportData(String courseName, String className,
            java.util.List<AssessmentInfo> assessments, java.util.List<StudentScoreRow> studentScores,
            java.util.List<String> objectiveLabels, java.util.List<PerStudentObj> perStudentObjectives,
            java.util.List<ObjectiveResult> objectiveResults, java.util.List<IndicatorResult> indicatorResults) {}
    public record AssessmentInfo(Long id, String name, double maxScore) {}
    public record StudentScoreRow(String studentNo, String studentName, java.util.Map<Long, Double> scores) {}
    public record PerStudentObj(String studentNo, String studentName, java.util.List<Double> achievements) {}
    public record ObjectiveResult(String objNo, String description, BigDecimal achievement) {}
    public record IndicatorResult(String indicatorNo, BigDecimal achievement, String relatedObjectives) {}

    public record IndicatorTraceSheet(String indicatorNo, List<TraceRow> rows) {}

    public record TraceRow(String courseName, BigDecimal ek, BigDecimal wc,
                            String objectiveNo, BigDecimal objAchievement, BigDecimal wjk,
                            String assessmentName, BigDecimal maxScore, BigDecimal avgScore) {}
}
