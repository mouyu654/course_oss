package com.obe.moduled.exporter;

import com.obe.common.BizException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PdfExporter {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PDFont cachedFont = null;

    public static byte[] generateCourseReport(String courseName, String className,
                                               Map<String, BigDecimal> objectiveResults,
                                               Map<String, BigDecimal> indicatorResults,
                                               LocalDateTime calcTime) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            doc.addPage(page);
            PDFont font = loadFont(doc);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = page.getMediaBox().getHeight() - MARGIN;

            y = writeLine(cs, font, 16, MARGIN, y, safeText("Course Attainment Report", font));
            y -= LINE_HEIGHT;
            y = writeLine(cs, font, 11, MARGIN, y, safeText("Course: " + courseName + "    Class: " + className, font));
            if (calcTime != null) {
                y = writeLine(cs, font, 10, MARGIN, y, safeText("Calc Time: " + calcTime.format(DTF), font));
            }
            y -= LINE_HEIGHT;
            y = writeLine(cs, font, 13, MARGIN, y, safeText("--- Objective Achievements (Level 1) ---", font));
            for (Map.Entry<String, BigDecimal> e : objectiveResults.entrySet()) {
                y = writeLine(cs, font, 10, MARGIN + 20, y, safeText("Objective " + e.getKey() + ": " + e.getValue(), font));
            }
            y -= LINE_HEIGHT;
            y = writeLine(cs, font, 13, MARGIN, y, safeText("--- Indicator Achievements (Level 2) ---", font));
            for (Map.Entry<String, BigDecimal> e : indicatorResults.entrySet()) {
                y = writeLine(cs, font, 10, MARGIN + 20, y, safeText("Indicator " + e.getKey() + ": " + e.getValue(), font));
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("PDF export failed: " + e.getMessage());
        }
    }

    private static String safeText(String text, PDFont font) {
        if (text == null) return "";
        if (font == null) return text.replaceAll("[^\\x20-\\x7E]", "?");
        try { font.encode(text); return text; } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                try { font.encode(String.valueOf(c)); sb.append(c); } catch (Exception ignored) { sb.append('?'); }
            }
            return sb.toString();
        }
    }

    private static float writeLine(PDPageContentStream cs, PDFont font, float fontSize,
                                    float x, float y, String text) throws Exception {
        if (font == null) return y;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private static PDFont loadFont(PDDocument doc) {
        if (cachedFont != null) { try { cachedFont.encode("test"); return cachedFont; } catch (Exception ignored) {} }
        // 1. Classpath fonts
        for (String path : new String[]{"/fonts/NotoSansSC-Regular.ttf", "/fonts/SimHei.ttf", "/fonts/simsun.ttf"}) {
            try { InputStream is = PdfExporter.class.getResourceAsStream(path); if (is != null) { cachedFont = PDType0Font.load(doc, is); return cachedFont; } } catch (Exception ignored) {}
        }
        // 2. Windows system fonts
        for (String dir : new String[]{"C:/Windows/Fonts/", "C:/WinNT/Fonts/"}) {
            for (String name : new String[]{"simhei.ttf", "simsun.ttc", "msyh.ttc", "msyh.ttf"}) {
                try { File f = new File(dir + name); if (f.exists()) { cachedFont = PDType0Font.load(doc, f); return cachedFont; } } catch (Exception ignored) {}
            }
        }
        // 3. Linux system fonts
        for (String path : new String[]{"/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc", "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"}) {
            try { if (Files.exists(Paths.get(path))) { cachedFont = PDType0Font.load(doc, new File(path)); return cachedFont; } } catch (Exception ignored) {}
        }
        // 4. Fallback: null → safeText strips non-ASCII
        return null;
    }
}