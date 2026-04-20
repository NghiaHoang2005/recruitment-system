package com.recruitment.backend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(1001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    JOB_NOT_FOUND(1002, "Công việc không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1003, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    USER_EXISTED(1005, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),

    CV_NOT_FOUND(2001, "CV không tồn tại", HttpStatus.NOT_FOUND),
    CV_PROCESSING_FAILED(2002, "Xử lý CV thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_PROCESSING(2003, "AI đang xử lý CV, vui lòng thử lại sau", HttpStatus.ACCEPTED),
    PRESIGNED_URL_FAILED(2004, "Không thể tạo đường dẫn tải file", HttpStatus.INTERNAL_SERVER_ERROR),
    READ_FILE_FAILED(2005, "Không thể đọc file CV", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EMPTY(2006, "File CV không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(2007, "Định dạng file không hợp lệ. Chỉ chấp nhận PDF hoặc DOCX", HttpStatus.BAD_REQUEST),
    URL_NOT_FOUND(2008, "Không tìm thấy URL của file CV", HttpStatus.INTERNAL_SERVER_ERROR),
    CV_REVIEW_NOT_FOUND(2009, "Không tìm thấy kết quả review CV", HttpStatus.NOT_FOUND),
    CV_REVIEW_RATE_LIMIT_EXCEEDED(2010, "Bạn đã dùng hết 3 lượt review CV trong 24 giờ qua", HttpStatus.TOO_MANY_REQUESTS),

    CANDIDATE_NOT_FOUND(3001, "Ứng viên không tồn tại", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
