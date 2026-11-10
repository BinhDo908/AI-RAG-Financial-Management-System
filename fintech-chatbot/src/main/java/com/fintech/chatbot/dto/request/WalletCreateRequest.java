package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.WalletType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * UC-A01 — Thêm ví.
 */
public record WalletCreateRequest(
        @NotBlank(message = "Tên ví không được để trống") @Size(max = 100) String name,
        @NotNull(message = "Vui lòng chọn loại ví") WalletType type,
        @NotNull(message = "Vui lòng nhập số dư ban đầu") @PositiveOrZero(message = "Số dư ban đầu không được âm") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal initialBalance,
        boolean defaultWallet
) {
}
