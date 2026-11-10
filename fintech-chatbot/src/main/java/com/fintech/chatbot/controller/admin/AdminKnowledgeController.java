package com.fintech.chatbot.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.request.DocumentMetadataRequest;
import com.fintech.chatbot.dto.response.KnowledgeResponses;
import com.fintech.chatbot.entity.enums.DocumentStatus;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Admin - Knowledge base", description = "UC-C01 Quản lý kho tri thức RAG")
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseService knowledgeService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(summary = "Danh sách tài liệu")
    @GetMapping("/documents")
    public ApiResponse<PageResponse<KnowledgeResponses.DocumentSummary>> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(knowledgeService.list(status, keyword, page, size));
    }

    @Operation(summary = "Chi tiết tài liệu + xem trước các đoạn")
    @GetMapping("/documents/{id}")
    public ApiResponse<KnowledgeResponses.DocumentDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeService.get(id));
    }

    @Operation(summary = "Tải lên file (PDF/DOCX/TXT/MD/HTML) + metadata JSON",
            description = "multipart: phần 'file' và phần 'metadata' (JSON DocumentMetadataRequest). Tài liệu ở trạng thái DRAFT.")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeResponses.DocumentDetail> upload(@CurrentUser UserPrincipal admin,
                                                                 @RequestPart("file") MultipartFile file,
                                                                 @RequestPart("metadata") String metadataJson) throws IOException {
        DocumentMetadataRequest metadata = parseMetadata(metadataJson);
        return ApiResponse.ok(knowledgeService.upload(admin.id(), metadata, file.getBytes(), file.getOriginalFilename()),
                "Đã tải lên, hãy xem trước các đoạn rồi duyệt");
    }

    @Operation(summary = "Nhập tài liệu từ URL trang công khai (metadata.url)")
    @PostMapping("/documents/from-url")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeResponses.DocumentDetail> importUrl(@CurrentUser UserPrincipal admin,
                                                                    @Valid @RequestBody DocumentMetadataRequest metadata) {
        return ApiResponse.ok(knowledgeService.importUrl(admin.id(), metadata));
    }

    @Operation(summary = "Sửa metadata")
    @PutMapping("/documents/{id}")
    public ApiResponse<KnowledgeResponses.DocumentSummary> update(@PathVariable Long id,
                                                                  @Valid @RequestBody DocumentMetadataRequest metadata) {
        return ApiResponse.ok(knowledgeService.updateMetadata(id, metadata));
    }

    @Operation(summary = "Duyệt → tạo embedding → APPROVED (cũng dùng để bật lại)")
    @PostMapping("/documents/{id}/approve")
    public ApiResponse<KnowledgeResponses.DocumentSummary> approve(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeService.approve(id), "Đã duyệt và index tài liệu");
    }

    @Operation(summary = "AF-1 — Tắt tài liệu (không còn được truy xuất)")
    @PostMapping("/documents/{id}/disable")
    public ApiResponse<KnowledgeResponses.DocumentSummary> disable(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeService.disable(id));
    }

    @Operation(summary = "AF-2 — Cập nhật phiên bản mới (về DRAFT, cần duyệt lại)")
    @PostMapping(value = "/documents/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeResponses.DocumentDetail> replace(@PathVariable Long id,
                                                                  @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(knowledgeService.replaceContent(id, file.getBytes(), file.getOriginalFilename()));
    }

    @Operation(summary = "AF-4 — Xóa tài liệu, đoạn và vector")
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResponse.message("Đã xóa tài liệu");
    }

    @Operation(summary = "Index lại toàn bộ tài liệu đã duyệt (sau khi đổi model embedding)")
    @PostMapping("/reindex")
    public ApiResponse<KnowledgeResponses.ReindexResult> reindex() {
        return ApiResponse.ok(knowledgeService.reindexAll());
    }

    @Operation(summary = "AF-3 — Thử truy vấn: xem top_k đoạn và điểm")
    @GetMapping("/test-query")
    public ApiResponse<KnowledgeResponses.RetrievalTest> testQuery(@RequestParam String q) {
        return ApiResponse.ok(knowledgeService.testQuery(q));
    }

    private DocumentMetadataRequest parseMetadata(String json) {
        DocumentMetadataRequest metadata;
        try {
            metadata = objectMapper.readValue(json, DocumentMetadataRequest.class);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Phần metadata không phải JSON hợp lệ");
        }
        var violations = validator.validate(metadata);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return metadata;
    }
}
