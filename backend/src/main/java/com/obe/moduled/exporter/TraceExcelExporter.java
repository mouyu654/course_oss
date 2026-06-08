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

    /**
     * Generate a course-level detail report with multiple sheets.
     */
    public static byte[] generateCourseDetailReport(CourseReportData data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle passStyle = createPassStyle(workbook);
            CellStyle failStyle = createFailStyle(workbook);

            // === Sheet 1: 学生成绩明细 ===
            Sheet scoreSheet = workbook.createSheet("学生成绩明细");
            int col = 0;
            Row shRow = scoreSheet.createRow(0);
            shRow.createCell(col++).setCellValue("学号"); shRow.getCell(0).setCellStyle(headerStyle);
            shRow.createCell(col++).setCellValue("姓名"); shRow.getCell(1).setCellStyle(headerStyle);
            for (AssessmentInfo ai : data.assessments()) {
                Cell cell = shRow.createCell(col++);
                cell.setCellValue(ai.name() + "\n(满分" + ai.maxScore() + ")");
                cell.setCellStyle(headerStyle);
            }
            Cell totalH = shRow.createCell(col++); totalH.setCellValue("总分"); totalH.setCellStyle(headerStyle);
            Cell rateH = shRow.createCell(col); rateH.setCellValue("得分率"); rateH.setCellStyle(headerStyle);
            int totalCols = col + 1;

            java.util.List<StudentScoreRow> sortedStudents = new java.util.ArrayList<>(data.studentScores());
            sortedStudents.sort((a, b) -> a.studentNo().compareTo(b.studentNo()));
            int r = 1;
            for (StudentScoreRow ss : sortedStudents) {
                Row row = scoreSheet.createRow(r++);
                col = 0;
                row.createCell(col++).setCellValue(ss.studentNo()); row.getCell(0).setCellStyle(dataStyle);
                row.createCell(col++).setCellValue(ss.studentName()); row.getCell(1).setCellStyle(dataStyle);
                double totalScore = 0, totalMax = 0;
                for (AssessmentInfo ai : data.assessments()) {
                    Double score = ss.scores().get(ai.id());
                    Cell cell = row.createCell(col++);
                    if (score != null) {
                        cell.setCellValue(score);
                        totalScore += score;
                        totalMax += ai.maxScore();
                        cell.setCellStyle(dataStyle);
                    } else {
                        cell.setCellValue("缺");
                        cell.setCellStyle(failStyle);
                    }
                }
                row.createCell(col++).setCellValue(Math.round(totalScore * 100.0) / 100.0);
                row.getCell(col - 1).setCellStyle(dataStyle);
                double pct = totalMax > 0 ? totalScore / totalMax : 0;
                Cell pctCell = row.createCell(col++);
                pctCell.setCellValue(Math.round(pct * 10000.0) / 100.0 + "%");
                pctCell.setCellStyle(pct >= 0.6 ? passStyle : failStyle);
            }
            for (int i = 0; i < totalCols; i++) scoreSheet.autoSizeColumn(i);

            // === Sheet 2: 课程目标达成度 ===
            Sheet objSheet = workbook.createSheet("课程目标达成度");
            Row ohRow = objSheet.createRow(0);
            String[] oh = {"目标编号", "目标描述", "班级平均达成度", "达标状态"};
            for (int i = 0; i < oh.length; i++) {
                Cell c = ohRow.createCell(i); c.setCellValue(oh[i]); c.setCellStyle(headerStyle);
            }
            int orIdx = 1;
            for (ObjectiveResult or : data.objectiveResults()) {
                Row row = objSheet.createRow(orIdx++);
                row.createCell(0).setCellValue(or.objNo()); row.getCell(0).setCellStyle(dataStyle);
                row.createCell(1).setCellValue(or.description()); row.getCell(1).setCellStyle(dataStyle);
                double ach = or.achievement() != null ? or.achievement().doubleValue() : 0;
                row.createCell(2).setCellValue(ach); row.getCell(2).setCellStyle(dataStyle);
                boolean pass = or.achievement() != null && or.achievement().compareTo(new BigDecimal("0.6")) >= 0;
                row.createCell(3).setCellValue(pass ? "达标" : "未达标");
                row.getCell(3).setCellStyle(pass ? passStyle : failStyle);
            }
            for (int i = 0; i < oh.length; i++) objSheet.autoSizeColumn(i);

            // === Sheet 3: 指标点达成度 + 综合汇总 ===
            Sheet indSheet = workbook.createSheet("毕业要求指标点达成度");
            Row ihRow = indSheet.createRow(0);
            String[] ih = {"指标点编号", "课程级达成度 E_k", "达标状态", "相关课程目标"};
            for (int i = 0; i < ih.length; i++) {
                Cell c = ihRow.createCell(i); c.setCellValue(ih[i]); c.setCellStyle(headerStyle);
            }
            int irIdx = 1, passCount = 0, totalCount = 0;
            for (IndicatorResult ir : data.indicatorResults()) {
                Row row = indSheet.createRow(irIdx++);
                row.createCell(0).setCellValue(ir.indicatorNo()); row.getCell(0).setCellStyle(dataStyle);
                double ach = ir.achievement() != null ? ir.achievement().doubleValue() : 0;
                row.createCell(1).setCellValue(ach); row.getCell(1).setCellStyle(dataStyle);
                boolean pass = ir.achievement() != null && ir.achievement().compareTo(new BigDecimal("0.6")) >= 0;
                row.createCell(2).setCellValue(pass ? "达标" : "未达标");
                row.getCell(2).setCellStyle(pass ? passStyle : failStyle);
                row.createCell(3).setCellValue(ir.relatedObjectives() != null ? ir.relatedObjectives() : "");
                row.getCell(3).setCellStyle(dataStyle);
                if (pass) passCount++;
                totalCount++;
            }
            irIdx++;
            double pct = totalCount > 0 ? (double) passCount / totalCount * 100 : 0;
            double avgSum = data.indicatorResults().stream().mapToDouble(ir -> ir.achievement() != null ? ir.achievement().doubleValue() : 0).sum();
            double avg = totalCount > 0 ? avgSum / totalCount : 0;

            String[][] summary = {
                {"综合汇总", "", "", ""},
                {"达标率", String.format("%d/%d = %.1f%%", passCount, totalCount, pct), "", ""},
                {"平均达成度", String.format("%.4f", avg), "", ""},
                {"课程", data.courseName() + " — " + data.className(), "", ""}
            };
            for (String[] sr : summary) {
                Row row = indSheet.createRow(irIdx++);
                for (int i = 0; i < sr.length; i++) {
                    Cell c = row.createCell(i); c.setCellValue(sr[i]); c.setCellStyle(headerStyle);
                }
            }
            for (int i = 0; i < ih.length; i++) indSheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成课程报告失败: " + e.getMessage());
        }
    }

    private static CellStyle createPassStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createFailStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN); style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // === Data records for course detail report ===

    public record CourseReportData(
            String courseName, String className,
            java.util.List<AssessmentInfo> assessments,
            java.util.List<StudentScoreRow> studentScores,
            java.util.List<ObjectiveResult> objectiveResults,
            java.util.List<IndicatorResult> indicatorResults) {}

    public record AssessmentInfo(Long id, String name, double maxScore) {}
    public record StudentScoreRow(String studentNo, String studentName, java.util.Map<Long, Double> scores) {}
    public record ObjectiveResult(String objNo, String description, BigDecimal achievement) {}
    public record IndicatorResult(String indicatorNo, BigDecimal achievement, String relatedObjectives) {}

    public record IndicatorTraceSheet(String indicatorNo, List<TraceRow> rows) {}

    public record TraceRow(String courseName, BigDecimal ek, BigDecimal wc,
                            String objectiveNo, BigDecimal objAchievement, BigDecimal wjk,
                            String assessmentName, BigDecimal maxScore, BigDecimal avgScore) {}
}
