package com.fintech.chatbot.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Định dạng số tiền kiểu Việt Nam: {@code 1234567 → "1.234.567đ"}.
 */
public final class MoneyFormatter {

    private MoneyFormatter() {
    }

    public static String format(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount.setScale(0, RoundingMode.HALF_UP)) + "đ";
    }
}
