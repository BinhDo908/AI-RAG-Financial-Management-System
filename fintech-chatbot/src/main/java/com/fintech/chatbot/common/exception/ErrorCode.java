package com.fintech.chatbot.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ. Thông điệp mặc định bằng tiếng Việt (NFR-06).
 */
@Getter
public enum ErrorCode {

    // Chung
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp sự cố, vui lòng thử lại sau"),

    // Tài khoản (UC-D01, D02, D03, C05)
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "Xác nhận mật khẩu không khớp"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    LOGIN_TEMPORARILY_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã nhập sai quá nhiều lần, vui lòng thử lại sau"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên"),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng"),
    DELETE_CONFIRMATION_INVALID(HttpStatus.BAD_REQUEST, "Câu xác nhận xóa tài khoản không đúng"),
    CANNOT_LOCK_SELF(HttpStatus.BAD_REQUEST, "Không thể tự khóa tài khoản của chính mình"),

    // Danh mục (UC-A03, C06)
    CATEGORY_NAME_DUPLICATE(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại"),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "Danh mục đã được sử dụng, chỉ có thể ẩn hoặc gộp (BR-10)"),
    CATEGORY_PARENT_INVALID(HttpStatus.BAD_REQUEST, "Danh mục cha không hợp lệ"),
    CATEGORY_NOT_EDITABLE(HttpStatus.FORBIDDEN, "Danh mục mặc định chỉ Admin được sửa"),
    CATEGORY_MERGE_INVALID(HttpStatus.BAD_REQUEST, "Danh mục đích phải khác danh mục nguồn và cùng loại"),

    // Ví (UC-A01)
    WALLET_NAME_DUPLICATE(HttpStatus.CONFLICT, "Tên ví đã tồn tại"),
    WALLET_HAS_TRANSACTIONS(HttpStatus.CONFLICT, "Ví đã có giao dịch, chỉ có thể lưu trữ"),
    WALLET_ARCHIVED(HttpStatus.BAD_REQUEST, "Ví đã được lưu trữ, không thể dùng cho giao dịch mới"),
    BALANCE_UNCHANGED(HttpStatus.BAD_REQUEST, "Số dư mới bằng số dư hiện tại"),

    // Giao dịch (UC-A02)
    TRANSFER_SAME_WALLET(HttpStatus.BAD_REQUEST, "Ví nguồn và ví đích phải khác nhau"),
    TRANSFER_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng chọn ví đích"),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng chọn danh mục"),
    CATEGORY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Loại danh mục không khớp loại giao dịch"),
    TRANSACTION_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Loại giao dịch này không được tạo trực tiếp"),
    TRANSACTION_LINKED(HttpStatus.CONFLICT, "Giao dịch thuộc khoản nợ / mục tiêu tiết kiệm, hãy sửa ở chức năng tương ứng"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Khoảng thời gian không hợp lệ"),
    NO_DATA_TO_EXPORT(HttpStatus.NOT_FOUND, "Không có dữ liệu trong khoảng đã chọn"),

    // Ngân sách (UC-A04)
    BUDGET_DUPLICATE(HttpStatus.CONFLICT, "Đã có ngân sách cho danh mục này trong tháng, hãy sửa ngân sách cũ"),
    BUDGET_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "Ngân sách chỉ áp dụng cho danh mục chi"),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "Tháng phải có dạng yyyy-MM"),

    // Mục tiêu tiết kiệm (UC-A05)
    GOAL_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "Mục tiêu đã hủy"),
    GOAL_WITHDRAW_EXCEEDS(HttpStatus.BAD_REQUEST, "Số tiền rút lớn hơn số tiền đang có trong mục tiêu"),

    // Nợ (UC-A06)
    DEBT_ALREADY_PAID(HttpStatus.BAD_REQUEST, "Khoản nợ đã được thanh toán xong"),
    DEBT_PAYMENT_EXCEEDS(HttpStatus.BAD_REQUEST, "Số tiền thanh toán lớn hơn dư nợ còn lại"),

    // Hội thoại (UC-B08)
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hội thoại"),

    // Hồ sơ tài chính (UC-B04)
    RISK_ANSWERS_INVALID(HttpStatus.BAD_REQUEST, "Cần trả lời đủ 5 câu, mỗi câu chọn 1–3"),

    // AI (giai đoạn 3)
    AI_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "Chức năng AI chưa được cấu hình (thiếu API key hoặc model)"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "Hệ thống AI đang bận, vui lòng thử lại sau"),
    STT_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình chuyển giọng nói ở server, hãy dùng nhận dạng giọng nói của trình duyệt"),
    AUDIO_INVALID(HttpStatus.BAD_REQUEST, "File ghi âm không hợp lệ hoặc quá dài (tối đa 60 giây)"),
    IMAGE_INVALID(HttpStatus.BAD_REQUEST, "Ảnh không hợp lệ: chỉ nhận JPG, PNG hoặc WEBP, tối đa 8 MB"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn"),
    FEEDBACK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Chỉ đánh giá được câu trả lời của chatbot"),

    // Kho tri thức (UC-C01)
    DOCUMENT_EXTRACT_FAILED(HttpStatus.BAD_REQUEST, "Không trích xuất được văn bản (PDF dạng ảnh?), hãy dùng bản có chữ"),
    DOCUMENT_UNSUPPORTED(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ PDF, DOCX, TXT, MD, HTML"),
    DOCUMENT_DUPLICATE(HttpStatus.CONFLICT, "Tài liệu có nội dung trùng với một tài liệu đã có"),
    DOCUMENT_METADATA_REQUIRED(HttpStatus.BAD_REQUEST, "Thiếu nguồn hoặc giấy phép, không thể duyệt (BR-03)"),
    DOCUMENT_URL_INVALID(HttpStatus.BAD_REQUEST, "URL không hợp lệ hoặc không được phép truy cập"),
    DOCUMENT_NOT_DRAFT(HttpStatus.CONFLICT, "Thao tác chỉ áp dụng cho tài liệu ở trạng thái phù hợp"),

    // Cấu hình AI (UC-C04)
    AI_CONFIG_INVALID(HttpStatus.BAD_REQUEST, "Giá trị cấu hình ngoài phạm vi cho phép"),

    // Dữ liệu thị trường (UC-B07)
    MARKET_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Không lấy được dữ liệu thị trường lúc này");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
