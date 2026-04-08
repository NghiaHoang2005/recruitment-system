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
    USER_EXISTED(1005, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
