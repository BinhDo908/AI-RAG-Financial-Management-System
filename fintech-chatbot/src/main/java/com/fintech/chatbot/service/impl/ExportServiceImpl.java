package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final String[] HEADERS = {"Ngày", "Loại", "Danh mục", "Ví", "Ví đích", "Số tiền", "Ghi chú", "Nguồn nhập"};
    private static final int[] COLUMN_WIDTHS = {12, 12, 20, 18, 18, 15, 40, 12};
    /** BOM giúp Excel đọc đúng tiếng Việt trong file CSV UTF-8. */
    private static final String UTF8_BOM = "﻿";

    private final TransactionDao transactionDao;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactions(Long userId, LocalDate from, LocalDate to, Format format) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        List<Transaction> rows = transactionDao.findAll(userId, TransactionFilter.between(from, to));
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_DATA_TO_EXPORT);
        }
        return format == Format.CSV ? csv(rows) : xlsx(rows);
    }

    private byte[] csv(List<Transaction> rows) {
        StringBuilder sb = new StringBuilder(UTF8_BOM).append(String.join(",", HEADERS)).append("\r\n");
        for (Transaction t : rows) {
            String[] cells = {
                    t.getTxnDate().toString(), typeLabel(t.getType()), categoryName(t), t.getWallet().getName(),
                    t.getToWallet() == null ? "" : t.getToWallet().getName(), t.getAmount().toPlainString(),
                    t.getNote() == null ? "" : t.getNote(), sourceLabel(t.getSource())
            };
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapeCsv(cells[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(List<Transaction> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Giao dịch");
            CreationHelper helper = wb.getCreationHelper();

            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(bold);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0"));

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int r = 1;
            for (Transaction t : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(t.getTxnDate());
                row.getCell(0).setCellStyle(dateStyle);
                row.createCell(1).setCellValue(typeLabel(t.getType()));
                row.createCell(2).setCellValue(categoryName(t));
                row.createCell(3).setCellValue(t.getWallet().getName());
                row.createCell(4).setCellValue(t.getToWallet() == null ? "" : t.getToWallet().getName());
                row.createCell(5).setCellValue(t.getAmount().doubleValue());
                row.getCell(5).setCellStyle(moneyStyle);
                row.createCell(6).setCellValue(t.getNote() == null ? "" : t.getNote());
                row.createCell(7).setCellValue(sourceLabel(t.getSource()));
            }
            // Độ rộng cố định (autoSizeColumn cần font hệ thống, dễ lỗi trên server không có giao diện)
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String categoryName(Transaction t) {
        if (t.getCategory() != null) {
            return t.getCategory().getName();
        }
        if (t.getGoalId() != null) {
            return "Mục tiêu tiết kiệm";
        }
        return t.getDebtId() != null ? "Khoản nợ" : "";
    }

    private static String typeLabel(TransactionType type) {
        return switch (type) {
            case INCOME -> "Thu";
            case EXPENSE -> "Chi";
            case TRANSFER -> "Chuyển tiền";
            case ADJUST -> "Điều chỉnh";
        };
    }

    private static String sourceLabel(TransactionSource source) {
        return switch (source) {
            case MANUAL -> "Thủ công";
            case CHAT -> "Chat";
            case VOICE -> "Giọng nói";
            case RECEIPT -> "Ảnh hóa đơn";
            case RECURRING -> "Định kỳ";
        };
    }

    private static String escapeCsv(String value) {
        // Chặn CSV injection: ô bắt đầu bằng = + - @ bị Excel hiểu là công thức
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0 && !safe.matches("-?\\d+")) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
