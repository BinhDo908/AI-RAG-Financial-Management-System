package com.fintech.chatbot.service;

import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.request.DocumentMetadataRequest;
import com.fintech.chatbot.dto.response.KnowledgeResponses;
import com.fintech.chatbot.entity.enums.DocumentStatus;

/**
 * UC-C01 — Quản lý kho tri thức RAG: tải lên → trích xuất → chia đoạn (DRAFT) → Admin duyệt → embed (APPROVED).
 */
public interface KnowledgeBaseService {

    PageResponse<KnowledgeResponses.DocumentSummary> list(DocumentStatus status, String keyword, int page, int size);

    /** Bước 6: metadata kèm toàn bộ đoạn để xem trước. */
    KnowledgeResponses.DocumentDetail get(Long documentId);

    /** Bước 2–5 (file): PDF / DOCX / TXT / MD / HTML. */
    KnowledgeResponses.DocumentDetail upload(Long adminId, DocumentMetadataRequest metadata, byte[] content, String filename);

    /** Bước 2–5 (URL trang công khai). {@code metadata.url} là URL cần tải. */
    KnowledgeResponses.DocumentDetail importUrl(Long adminId, DocumentMetadataRequest metadata);

    KnowledgeResponses.DocumentSummary updateMetadata(Long documentId, DocumentMetadataRequest metadata);

    /** Bước 7: duyệt → embed mọi đoạn → APPROVED (BR-03). Cũng dùng để bật lại tài liệu DISABLED. */
    KnowledgeResponses.DocumentSummary approve(Long documentId);

    /** AF-1: không còn được truy xuất, giữ nguyên dữ liệu. */
    KnowledgeResponses.DocumentSummary disable(Long documentId);

    /** AF-2: thay nội dung bằng phiên bản mới → xóa đoạn cũ, chia lại, trạng thái về DRAFT chờ duyệt lại. */
    KnowledgeResponses.DocumentDetail replaceContent(Long documentId, byte[] content, String filename);

    /** AF-4: xóa tài liệu cùng đoạn và vector. */
    void delete(Long documentId);

    /** Index lại mọi tài liệu APPROVED (khi đổi model embedding). */
    KnowledgeResponses.ReindexResult reindexAll();

    /** AF-3: thử truy vấn để kiểm tra chất lượng index. */
    KnowledgeResponses.RetrievalTest testQuery(String question);
}
