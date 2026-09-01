package com.tikzy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNCATEGORIZED(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Auth / User 1xxx
    USER_NOT_FOUND(1001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(1002, "Email đã được đăng ký", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(1003, "Số điện thoại đã được đăng ký", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1004, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1005, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED(1006, "Phiên đăng nhập đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(1008, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_REUSED(1009, "Refresh token đã bị tái sử dụng, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),
    INVALID_USER_DATA(1010, "Thông tin người dùng không hợp lệ", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1011, "Vai trò không tồn tại", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_REQUEST(1012, "Thông tin email không hợp lệ", HttpStatus.BAD_REQUEST),
    EMAIL_SERVICE_NOT_CONFIGURED(1013, "Dịch vụ email chưa được cấu hình", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_SEND_FAILED(1014, "Không thể gửi email", HttpStatus.BAD_GATEWAY),
    EMAIL_TEMPLATE_NOT_FOUND(1015, "Không tìm thấy mẫu email", HttpStatus.NOT_FOUND),
    EMAIL_TEMPLATE_VARIABLE_MISSING(1016, "Mẫu email thiếu dữ liệu bắt buộc", HttpStatus.INTERNAL_SERVER_ERROR),

    // Event 2xxx
    EVENT_NOT_FOUND(2001, "Không tìm thấy sự kiện", HttpStatus.NOT_FOUND),
    EVENT_NOT_PUBLISHED(2002, "Sự kiện chưa được công bố", HttpStatus.BAD_REQUEST),
    EVENT_ALREADY_CANCELLED(2003, "Sự kiện đã bị hủy", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(2004, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    SHOW_TIME_NOT_FOUND(2005, "Không tìm thấy suất diễn", HttpStatus.NOT_FOUND),

    // Ticket / Inventory 3xxx
    TICKET_TYPE_NOT_FOUND(3001, "Không tìm thấy hạng vé", HttpStatus.NOT_FOUND),
    TICKET_SOLD_OUT(3002, "Hạng vé đã bán hết", HttpStatus.CONFLICT),
    TICKET_EXCEED_MAX_PER_ORDER(3003, "Vượt quá số lượng vé tối đa mỗi đơn", HttpStatus.BAD_REQUEST),
    TICKET_NOT_FOUND(3004, "Không tìm thấy vé", HttpStatus.NOT_FOUND),
    TICKET_ALREADY_CHECKED_IN(3005, "Vé đã được check-in", HttpStatus.CONFLICT),
    TICKET_INVALID_QR(3006, "Mã QR không hợp lệ hoặc đã bị giả mạo", HttpStatus.BAD_REQUEST),

    // Order / Promotion 4xxx
    ORDER_NOT_FOUND(4001, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_EXPIRED(4002, "Đơn hàng đã hết thời gian giữ chỗ", HttpStatus.BAD_REQUEST),
    ORDER_NOT_PAYABLE(4003, "Đơn hàng không ở trạng thái có thể thanh toán", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_FOUND(4004, "Mã giảm giá không tồn tại", HttpStatus.NOT_FOUND),
    PROMOTION_INVALID(4005, "Mã giảm giá không hợp lệ hoặc đã hết lượt dùng", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_APPLICABLE(4006, "Mã giảm giá không áp dụng cho đơn hàng này", HttpStatus.BAD_REQUEST),

    // Payment / Refund 5xxx
    PAYMENT_FAILED(5001, "Thanh toán thất bại", HttpStatus.BAD_REQUEST),
    PAYMENT_PROVIDER_NOT_SUPPORTED(5002, "Cổng thanh toán không được hỗ trợ", HttpStatus.BAD_REQUEST),
    REFUND_FAILED(5003, "Hoàn tiền thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_NOT_ALLOWED(5004, "Sự kiện này không áp dụng hoàn vé", HttpStatus.BAD_REQUEST),
    REFUND_DEADLINE_PASSED(5005, "Đã hết hạn được phép hoàn vé", HttpStatus.BAD_REQUEST),

    // Settlement 6xxx
    SETTLEMENT_NOT_FOUND(6001, "Không tìm thấy bảng quyết toán", HttpStatus.NOT_FOUND),
    SETTLEMENT_ALREADY_EXISTS(6002, "Sự kiện đã có bảng quyết toán", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
