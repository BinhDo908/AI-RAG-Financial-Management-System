package com.fintech.chatbot.service;

import java.time.LocalDate;

/**
 * UC-A10 — Xuất giao dịch ra file.
 */
public interface ExportService {

    /** Cột: Ngày, Loại, Danh mục, Ví, Ví đích, Số tiền, Ghi chú, Nguồn nhập. */
    byte[] exportTransactions(Long userId, LocalDate from, LocalDate to, Format format);

    enum Format {
        CSV("csv", "text/csv; charset=UTF-8"),
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private final String extension;
        private final String contentType;

        Format(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String extension() {
            return extension;
        }

        public String contentType() {
            return contentType;
        }
    }
}
