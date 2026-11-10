package com.fintech.chatbot.ai.rag;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * UC-C01 bước 4 — Trích xuất và làm sạch văn bản từ PDF (PDFBox), DOCX (POI), HTML (Jsoup), TXT, MD.
 */
@Component
public class TextExtractor {

    /** Dòng mục lục kiểu "Chương 1 ........ 12". */
    private static final Pattern TOC_LINE = Pattern.compile(".*\\.{5,}\\s*\\d+\\s*$");
    private static final Pattern PAGE_NUMBER_LINE = Pattern.compile("^\\s*(trang|page)?\\s*\\d+\\s*(/\\s*\\d+)?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MD_HEADING = Pattern.compile("^#{1,6}\\s+(.*)$");

    public List<TextSection> extract(byte[] content, String filename) {
        String ext = extension(filename);
        List<TextSection> sections = switch (ext) {
            case "pdf" -> pdf(content);
            case "docx" -> docx(content);
            case "html", "htm" -> html(new String(content, StandardCharsets.UTF_8));
            case "txt" -> List.of(new TextSection(null, null, new String(content, StandardCharsets.UTF_8)));
            case "md" -> markdown(new String(content, StandardCharsets.UTF_8));
            default -> throw new BusinessException(ErrorCode.DOCUMENT_UNSUPPORTED);
        };
        List<TextSection> cleaned = sections.stream()
                .map(s -> new TextSection(s.heading(), s.pageNumber(), normalize(s.text())))
                .filter(s -> !s.text().isBlank())
                .toList();
        if (cleaned.stream().mapToInt(s -> s.text().length()).sum() < 20) {
            throw new BusinessException(ErrorCode.DOCUMENT_EXTRACT_FAILED);
        }
        return cleaned;
    }

    public List<TextSection> html(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        doc.select("script, style, nav, header, footer, aside, form, noscript").remove();
        Element body = doc.body() == null ? doc : doc.body();
        List<TextSection> sections = new ArrayList<>();
        String heading = doc.title().isBlank() ? null : doc.title();
        StringBuilder current = new StringBuilder();
        for (Element el : body.select("h1, h2, h3, h4, p, li, td, pre, blockquote")) {
            if (el.tagName().matches("h[1-4]")) {
                flush(sections, heading, current);
                heading = el.text();
            } else if (el.parents().stream().noneMatch(p -> p.tagName().matches("li|td|blockquote") && p != el)) {
                current.append(el.text()).append("\n\n");
            }
        }
        flush(sections, heading, current);
        if (sections.isEmpty()) {
            sections.add(new TextSection(heading, null, body.text()));
        }
        return sections;
    }

    private List<TextSection> pdf(byte[] content) {
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(stripper.getText(doc));
            }
            List<String> repeated = repeatedLines(pages);
            List<TextSection> sections = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                StringBuilder sb = new StringBuilder();
                for (String line : pages.get(i).split("\\R")) {
                    if (!repeated.contains(line.trim())) {
                        sb.append(line).append('\n');
                    }
                }
                sections.add(new TextSection(null, i + 1, sb.toString()));
            }
            return sections;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_EXTRACT_FAILED);
        }
    }

    private List<TextSection> docx(byte[] content) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<TextSection> sections = new ArrayList<>();
            String heading = null;
            StringBuilder current = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String style = p.getStyle() == null ? "" : p.getStyle().toLowerCase(Locale.ROOT);
                if (style.startsWith("heading") || style.startsWith("title")) {
                    flush(sections, heading, current);
                    heading = p.getText();
                } else {
                    current.append(p.getText()).append('\n');
                }
            }
            flush(sections, heading, current);
            return sections;
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_EXTRACT_FAILED);
        }
    }

    private List<TextSection> markdown(String text) {
        List<TextSection> sections = new ArrayList<>();
        String heading = null;
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\R")) {
            var m = MD_HEADING.matcher(line);
            if (m.matches()) {
                flush(sections, heading, current);
                heading = m.group(1).trim();
            } else {
                current.append(line).append('\n');
            }
        }
        flush(sections, heading, current);
        return sections;
    }

    /** Dòng xuất hiện ở hơn một nửa số trang (từ 3 trang trở lên) là header / footer lặp lại. */
    private static List<String> repeatedLines(List<String> pages) {
        if (pages.size() < 3) {
            return List.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String page : pages) {
            page.lines().map(String::trim).filter(l -> !l.isEmpty()).distinct()
                    .forEach(l -> counts.merge(l, 1, Integer::sum));
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() > pages.size() / 2)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Bỏ dòng mục lục, dòng chỉ có số trang; gộp khoảng trắng thừa nhưng giữ ngắt đoạn. */
    static String normalize(String text) {
        StringBuilder sb = new StringBuilder();
        int blank = 0;
        for (String raw : text.split("\\R")) {
            String line = raw.replace(' ', ' ').replaceAll("[ \\t]+", " ").trim();
            if (TOC_LINE.matcher(line).matches() || PAGE_NUMBER_LINE.matcher(line).matches()) {
                continue;
            }
            if (line.isEmpty()) {
                if (++blank == 1 && !sb.isEmpty()) {
                    sb.append('\n');
                }
                continue;
            }
            blank = 0;
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    private static void flush(List<TextSection> sections, String heading, StringBuilder current) {
        if (!current.toString().isBlank()) {
            sections.add(new TextSection(heading, null, current.toString()));
        }
        current.setLength(0);
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.DOCUMENT_UNSUPPORTED);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
