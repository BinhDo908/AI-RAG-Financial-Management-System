package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.TransactionSearchResponse;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.ExportService;
import com.fintech.chatbot.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@Tag(name = "Transactions", description = "UC-A02 Giao dịch, UC-A08 Tìm kiếm & lọc, UC-A10 Xuất dữ liệu")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final ExportService exportService;

    @Operation(summary = "UC-A08 — Lọc giao dịch (mới nhất trước) kèm tổng thu / chi")
    @GetMapping
    public ApiResponse<TransactionSearchResponse> search(
            @CurrentUser UserPrincipal me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) TransactionSource source,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new TransactionFilter(from, to, walletId, categoryId, type, minAmount, maxAmount, source, keyword);
        return ApiResponse.ok(transactionService.search(me.id(), filter, page, size));
    }

    @Operation(summary = "UC-A10 — Xuất giao dịch ra CSV hoặc Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @CurrentUser UserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "XLSX") ExportService.Format format) {
        byte[] content = exportService.exportTransactions(me.id(), from, to, format);
        String filename = "giao-dich_" + from + "_" + to + "." + format.extension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }

    @Operation(summary = "Chi tiết giao dịch")
    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(transactionService.get(me.id(), id));
    }

    @Operation(summary = "UC-A02 — Thêm giao dịch thu / chi / chuyển tiền")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResult> create(@CurrentUser UserPrincipal me,
                                                 @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(transactionService.create(me.id(), request), "Đã lưu giao dịch");
    }

    @Operation(summary = "UC-A02 AF-1 — Sửa giao dịch")
    @PutMapping("/{id}")
    public ApiResponse<TransactionResult> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                 @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(transactionService.update(me.id(), id, request), "Đã cập nhật giao dịch");
    }

    @Operation(summary = "UC-A02 AF-2 — Xóa giao dịch")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        transactionService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa giao dịch");
    }
}
