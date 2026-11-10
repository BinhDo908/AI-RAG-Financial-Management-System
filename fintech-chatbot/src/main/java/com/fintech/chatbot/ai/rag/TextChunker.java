package com.fintech.chatbot.ai.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-C01 bước 5 — Chia văn bản thành đoạn ~500 token, chồng lấn ~50 token, theo ranh giới đề mục / đoạn / câu.
 *
 * <p>Ước lượng theo ký tự: tiếng Việt trung bình ~4 ký tự / token, nên 500 token ≈ 2000 ký tự, 50 token ≈ 200 ký tự.
 * Một đoạn không bao giờ vượt qua ranh giới đề mục để trích dẫn chính xác.
 */
@Component
public class TextChunker {

    public static final int CHUNK_CHARS = 2000;
    public static final int OVERLAP_CHARS = 200;

    public List<TextSection> chunk(List<TextSection> sections) {
        List<TextSection> chunks = new ArrayList<>();
        for (TextSection section : sections) {
            for (String piece : split(section.text())) {
                chunks.add(new TextSection(section.heading(), section.pageNumber(), piece));
            }
        }
        return chunks;
    }

    List<String> split(String text) {
        List<String> units = units(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.length() > 0 && current.length() + unit.length() + 1 > CHUNK_CHARS) {
                chunks.add(current.toString().trim());
                current = new StringBuilder(overlapTail(current.toString()));
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(unit);
        }
        if (!current.toString().isBlank()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    /** Tách thành đoạn văn; đoạn quá dài tách tiếp theo câu, câu quá dài cắt cứng. */
    private static List<String> units(String text) {
        List<String> units = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String p = paragraph.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= CHUNK_CHARS) {
                units.add(p);
                continue;
            }
            for (String sentence : p.split("(?<=[.!?…;:])\\s+")) {
                for (int i = 0; i < sentence.length(); i += CHUNK_CHARS) {
                    units.add(sentence.substring(i, Math.min(sentence.length(), i + CHUNK_CHARS)));
                }
            }
        }
        return units;
    }

    /** Lấy ~200 ký tự cuối, bắt đầu từ đầu một từ, làm phần chồng lấn của đoạn sau. */
    private static String overlapTail(String chunk) {
        if (chunk.length() <= OVERLAP_CHARS) {
            return chunk;
        }
        String tail = chunk.substring(chunk.length() - OVERLAP_CHARS);
        int space = tail.indexOf(' ');
        return space > 0 ? tail.substring(space + 1) : tail;
    }
}
