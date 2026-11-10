package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-A01 AF-1 — Sửa ví (không sửa số dư; dùng điều chỉnh số dư).
 */
public record WalletUpdateRequest(
        @NotBlank(message = "Tên ví không được để trống") @Size(max = 100) String name,
        @NotNull(message = "Vui lòng chọn loại ví") WalletType type,
        boolean defaultWallet
) {
}
