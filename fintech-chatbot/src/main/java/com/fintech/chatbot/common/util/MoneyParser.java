package com.fintech.chatbot.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuyển chuỗi số tiền tiếng Việt thành số VND (BR-08).
 *
 * <p>Hỗ trợ:
 * <ul>
 *   <li>Số thuần: {@code 50000}, {@code 15.000.000}, {@code 15,000,000}</li>
 *   <li>Đơn vị: {@code k / nghìn / ngàn} = ×1.000; {@code tr / triệu / củ} = ×1.000.000; {@code tỷ / tỉ} = ×1.000.000.000</li>
 *   <li>Số lẻ: {@code 1.5tr}, {@code 1,5 triệu}</li>
 *   <li>Kiểu nói tắt: {@code 1tr2} = 1.200.000, {@code 2k5} = 2.500</li>
 *   <li>Hậu tố tiền tệ được bỏ qua: {@code đ, vnd, vnđ, đồng}</li>
 * </ul>
 * Số đọc bằng chữ ("tám mươi nghìn") xử lý ở giai đoạn AI (UC-A12).
 */
public final class MoneyParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^(\\d+(?:[.,]\\d+)*)\\s*(k|nghìn|nghin|ngàn|ngan|tr|triệu|trieu|củ|cu|tỷ|tỉ|ty|ti)?\\s*(\\d+)?$");

    private static final Pattern CURRENCY_SUFFIX = Pattern.compile("\\s*(vnđ|vnd|đồng|dong|đ)$");

    private MoneyParser() {
    }

    /**
     * @return số tiền VND (số nguyên dương), hoặc rỗng nếu không hiểu được chuỗi.
     */
    public static Optional<BigDecimal> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        text = CURRENCY_SUFFIX.matcher(text).replaceFirst("").trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }

        Matcher m = PATTERN.matcher(text);
        if (!m.matches()) {
            return Optional.empty();
        }
        String numberPart = m.group(1);
        String unit = m.group(2);
        String tail = m.group(3);

        // "1tr2" hợp lệ, nhưng "50 2" (không có đơn vị) thì không
        if (tail != null && unit == null) {
            return Optional.empty();
        }

        BigDecimal number = parseNumber(numberPart, unit != null);
        if (number == null) {
            return Optional.empty();
        }

        BigDecimal multiplier = multiplierOf(unit);
        if (tail != null) {
            if (numberPart.contains(".") || numberPart.contains(",")) {
                return Optional.empty();
            }
            // "1tr2" → 1.2 triệu; "1tr25" → 1.25 triệu
            number = new BigDecimal(numberPart + "." + tail);
        }

        BigDecimal amount = number.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        return amount.signum() > 0 ? Optional.of(amount) : Optional.empty();
    }

    /**
     * Phân biệt dấu phân cách hàng nghìn và dấu thập phân.
     * Quy tắc: nếu mọi nhóm sau dấu phân cách đều đúng 3 chữ số và (có ≥ 2 dấu phân cách hoặc không có đơn vị)
     * thì đó là phân cách hàng nghìn; ngược lại chỉ cho phép 1 dấu và coi là dấu thập phân.
     */
    private static BigDecimal parseNumber(String numberPart, boolean hasUnit) {
        String[] groups = numberPart.split("[.,]");
        if (groups.length == 1) {
            return new BigDecimal(numberPart);
        }

        boolean allThreeDigits = true;
        for (int i = 1; i < groups.length; i++) {
            if (groups[i].length() != 3) {
                allThreeDigits = false;
                break;
            }
        }
        boolean thousandSeparators = allThreeDigits && (groups.length > 2 || !hasUnit);
        if (thousandSeparators) {
            return new BigDecimal(String.join("", groups));
        }
        if (groups.length == 2) {
            return new BigDecimal(groups[0] + "." + groups[1]);
        }
        return null;
    }

    private static BigDecimal multiplierOf(String unit) {
        if (unit == null) {
            return BigDecimal.ONE;
        }
        return switch (unit) {
            case "k", "nghìn", "nghin", "ngàn", "ngan" -> BigDecimal.valueOf(1_000L);
            case "tr", "triệu", "trieu", "củ", "cu" -> BigDecimal.valueOf(1_000_000L);
            case "tỷ", "tỉ", "ty", "ti" -> BigDecimal.valueOf(1_000_000_000L);
            default -> BigDecimal.ONE;
        };
    }
}
