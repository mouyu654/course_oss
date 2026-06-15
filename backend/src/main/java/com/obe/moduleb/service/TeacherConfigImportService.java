package com.obe.moduleb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obe.common.BizException;
import com.obe.moduleb.entity.CourseObjective;
import com.obe.moduleb.entity.CourseOutline;
import com.obe.moduleb.mapper.CourseObjectiveMapper;
import com.obe.moduleb.mapper.CourseOutlineMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherConfigImportService {

    private static final String[] OBJECTIVE_HEADERS = {"课程目标编号", "维度", "目标描述"};

    private final CourseOutlineMapper outlineMapper;
    private final CourseObjectiveMapper objectiveMapper;

    // ==================== 课程目标导入 ====================

    /**
     * 生成课程目标导入模板（Excel）。
     */
    public byte[] generateObjectiveTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = dataStyle(workbook);

            // ---- 主Sheet：数据填写区 ----
            Sheet sheet = workbook.createSheet("课程目标导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < OBJECTIVE_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(OBJECTIVE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 预置5个空行
            for (int r = 1; r <= 5; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < OBJECTIVE_HEADERS.length; c++) {
                    row.createCell(c).setCellStyle(dataStyle);
                }
            }

            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 50 * 256);
            sheet.createFreezePane(0, 1);

            // ---- 辅助Sheet：填写说明 ----
            Sheet note = workbook.createSheet("填写说明");
            String[] instructions = {
                    "课程目标编号、维度、目标描述均为必填项。",
                    "填写示例：1-1｜知识｜掌握课程核心概念与基本原理。",
                    "课程目标编号不能与当前教学班已有编号或文件内其他行重复。",
                    "维度可选值（参考）：知识、能力、价值。",
                    "请勿修改第一行表头；完全空白的行会被自动忽略。"
            };
            for (int i = 0; i < instructions.length; i++) {
                note.createRow(i).createCell(0).setCellValue((i + 1) + ". " + instructions[i]);
            }
            note.setColumnWidth(0, 100 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成导入模板失败: " + e.getMessage());
        }
    }

    /**
     * 从 Excel 文件批量导入课程目标。
     *
     * @param classId 教学班级ID
     * @param file    上传的 Excel 文件
     * @return 成功导入的条数
     */
    @Transactional
    public int importObjectives(Long classId, MultipartFile file) {
        // 1. 获取或创建教学大纲
        CourseOutline outline = getOrCreateOutline(classId);

        // 2. 查询已存在的课程目标编号（用于去重）
        Set<String> existingNos = objectiveMapper.selectList(
                        new LambdaQueryWrapper<CourseObjective>()
                                .eq(CourseObjective::getOutlineId, outline.getId()))
                .stream()
                .map(obj -> normalize(obj.getObjNo()))
                .collect(Collectors.toSet());

        // 3. 校验文件
        validateFile(file);

        List<String> errors = new ArrayList<>();
        Set<String> fileNos = new HashSet<>();
        List<CourseObjective> toInsert = new ArrayList<>();

        // 4. 逐行解析
        try (InputStream input = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(input)) {

            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0), OBJECTIVE_HEADERS);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                int rowNo = i + 1;
                String objNo = text(row.getCell(0));
                String dimension = text(row.getCell(1));
                String description = text(row.getCell(2));

                // 全空行跳过
                if (objNo.isBlank() && dimension.isBlank() && description.isBlank()) continue;

                // 必填校验
                if (objNo.isBlank()) errors.add(error(rowNo, "课程目标编号不能为空"));
                if (dimension.isBlank()) errors.add(error(rowNo, "维度不能为空"));
                if (description.isBlank()) errors.add(error(rowNo, "目标描述不能为空"));

                // 重复校验
                String key = normalize(objNo);
                if (!objNo.isBlank() && existingNos.contains(key)) {
                    errors.add(error(rowNo, "课程目标编号「" + objNo + "」已存在"));
                } else if (!objNo.isBlank() && !fileNos.add(key)) {
                    errors.add(error(rowNo, "课程目标编号「" + objNo + "」在文件中重复"));
                }

                // 如果有错误，跳过插入
                if (objNo.isBlank() || dimension.isBlank() || description.isBlank()
                        || existingNos.contains(key)
                        || toInsert.stream().anyMatch(o -> normalize(o.getObjNo()).equals(key))) {
                    continue;
                }

                CourseObjective objective = new CourseObjective();
                objective.setOutlineId(outline.getId());
                objective.setObjNo(objNo);
                objective.setDimension(dimension);
                objective.setDescription(description);
                toInsert.add(objective);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析Excel文件失败: " + e.getMessage());
        }

        // 5. 最终校验
        if (!errors.isEmpty()) {
            throw new BizException("导入失败，共 " + errors.size() + " 处问题，请修改后重新上传：\n"
                    + String.join("\n", errors));
        }
        if (toInsert.isEmpty()) {
            throw new BizException("文件中没有可导入的课程目标数据");
        }

        // 6. 批量插入
        toInsert.forEach(objectiveMapper::insert);
        return toInsert.size();
    }

    // ==================== 工具方法 ====================

    private CourseOutline getOrCreateOutline(Long classId) {
        CourseOutline outline = outlineMapper.selectOne(
                new LambdaQueryWrapper<CourseOutline>().eq(CourseOutline::getClassId, classId));
        if (outline == null) {
            outline = new CourseOutline();
            outline.setClassId(classId);
            outline.setStatus("DRAFT");
            outlineMapper.insert(outline);
        }
        return outline;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空，请选择标准模板文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException("仅支持.xlsx格式文件，请先下载标准模板");
        }
    }

    private void validateHeaders(Row header, String[] expected) {
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) {
            actual.add(header == null ? "" : text(header.getCell(i)));
        }
        if (!Arrays.asList(expected).equals(actual)) {
            throw new BizException("模板格式不正确，请重新下载标准模板。\n期望表头："
                    + String.join("、", expected) + "\n实际表头：" + String.join("、", actual));
        }
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception ignored) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            }
        }
        return cell.toString().trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String error(int rowNo, String message) {
        return "第" + rowNo + "行：" + message;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = dataStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
