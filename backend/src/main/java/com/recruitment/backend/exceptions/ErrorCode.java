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
    ROLE_NOT_FOUND(1006, "Vai tro khong ton tai", HttpStatus.NOT_FOUND),
    RECRUITER_PROFILE_ALREADY_EXISTS(1007, "Thong tin nha tuyen dung da ton tai", HttpStatus.BAD_REQUEST),
    RECRUITER_PROFILE_NOT_FOUND(1008, "Thong tin nha tuyen dung khong ton tai", HttpStatus.NOT_FOUND),
    COMPANY_NOT_FOUND(1009, "Cong ty khong ton tai", HttpStatus.NOT_FOUND),
    COMPANY_MEMBER_EXISTED(1010, "Nha tuyen dung dang cho duoc duyet", HttpStatus.BAD_REQUEST),
    RECRUITER_ALREADY_JOINED(1011, "Nha tuyen dung da tham gia mot cong ty khac", HttpStatus.BAD_REQUEST),
    COMPANY_MEMBER_NOT_FOUND(1012, "Yeu cau tham gia khong ton tai", HttpStatus.NOT_FOUND),
    INVALID_KEY(1013, "Invalid message key", HttpStatus.BAD_REQUEST),
    APPLICATION_NOT_FOUND(1014, "Don ung tuyen khong ton tai", HttpStatus.NOT_FOUND),
    APPLICATION_ALREADY_EXISTS(1015, "Ban da ung tuyen cong viec nay", HttpStatus.BAD_REQUEST),
    APPLICATION_INVALID_STATUS(1016, "Trang thai don ung tuyen khong hop le", HttpStatus.BAD_REQUEST),
    ADMIN_ACCESS_DENIED(1017, "Chi admin moi co quyen thuc hien thao tac nay", HttpStatus.FORBIDDEN),
    ADMIN_INVALID_MODERATION_ACTION(1018, "Thao tác kiểm duyệt không hợp lệ với trạng thái hiện tại", HttpStatus.BAD_REQUEST),
    USER_DISABLED(1019, "Tai khoan da bi vo hieu hoa", HttpStatus.FORBIDDEN),

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
    CV_BUILDER_TEMPLATE_NOT_FOUND(2011, "Template CV không tồn tại", HttpStatus.NOT_FOUND),
    CV_BUILDER_TEMPLATE_REQUIRED(2012, "Vui lòng chọn template trước khi tạo CV", HttpStatus.BAD_REQUEST),
    CV_BUILDER_DRAFT_NOT_FOUND(2013, "Bản nháp CV không tồn tại", HttpStatus.NOT_FOUND),
    CV_BUILDER_INVALID_CONTENT(2014, "Nội dung CV draft không hợp lệ", HttpStatus.BAD_REQUEST),
    CV_BUILDER_INVALID_SECTION(2015, "Section custom không hợp lệ", HttpStatus.BAD_REQUEST),
    CV_BUILDER_INVALID_SECTION_ORDER(2016, "Thứ tự section không hợp lệ", HttpStatus.BAD_REQUEST),
    CV_BUILDER_SECTION_NOT_FOUND(2017, "Section không tồn tại trong bản nháp", HttpStatus.NOT_FOUND),
    CV_BUILDER_STRICT_VALIDATION_FAILED(2018, "CV chưa đạt yêu cầu để xuất bản hoặc export", HttpStatus.BAD_REQUEST),
    CV_BUILDER_INVALID_CURSOR(2019, "Cursor không hợp lệ", HttpStatus.BAD_REQUEST),
    CV_BUILDER_VERSION_CONFLICT(2020, "Bản nháp đã được cập nhật ở nơi khác", HttpStatus.CONFLICT),

    CANDIDATE_NOT_FOUND(3001, "Ứng viên không tồn tại", HttpStatus.NOT_FOUND),
    OTP_INVALID_OR_EXPIRED(3002, "OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(3003, "Vui lòng nhập OTP", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_INVALID_OR_EXPIRED(3004, "Reset token không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_TOO_MANY_REQUESTS(3005, "Bạn đã yêu cầu OTP quá thường xuyên. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS),
    CANDIDATE_PROFILE_ALREADY_EXISTS(3006, "Thông tin ứng viên đã tồn tại", HttpStatus.BAD_REQUEST),
    AVATAR_FILE_EMPTY(3007, "Vui lòng chọn ảnh đại diện", HttpStatus.BAD_REQUEST),
    AVATAR_FILE_TOO_LARGE(3008, "Ảnh đại diện không được vượt quá 5 MB", HttpStatus.BAD_REQUEST),
    AVATAR_INVALID_FILE_TYPE(3009, "Ảnh đại diện chỉ hỗ trợ JPG, PNG hoặc WebP", HttpStatus.BAD_REQUEST),
    AVATAR_UPLOAD_FAILED(3010, "Không thể tải ảnh đại diện lên", HttpStatus.INTERNAL_SERVER_ERROR),
    JOB_REPORT_REASON_REQUIRED(3011, "Vui lòng chọn lý do báo cáo", HttpStatus.BAD_REQUEST),
    JOB_REPORT_ALREADY_EXISTS(3012, "Bạn đã báo cáo tin tuyển dụng này", HttpStatus.CONFLICT),
    JOB_REPORT_NOT_FOUND(3013, "Không tìm thấy báo cáo tin tuyển dụng", HttpStatus.NOT_FOUND),
    JOB_REPORT_INVALID_STATUS(3014, "Trạng thái xử lý báo cáo không hợp lệ", HttpStatus.BAD_REQUEST),
    JOB_CATEGORY_REQUIRED(3015, "Vui lòng chọn ít nhất một ngành nghề", HttpStatus.BAD_REQUEST),
    JOB_CATEGORY_INVALID(3016, "Ngành nghề đã chọn không hợp lệ", HttpStatus.BAD_REQUEST),
    LOCATION_INVALID(3017, "Địa điểm đã chọn không hợp lệ", HttpStatus.BAD_REQUEST),
    COMPANY_LOGO_FILE_EMPTY(3018, "Vui lòng chọn logo công ty", HttpStatus.BAD_REQUEST),
    COMPANY_LOGO_FILE_TOO_LARGE(3019, "Logo công ty không được vượt quá 5 MB", HttpStatus.BAD_REQUEST),
    COMPANY_LOGO_INVALID_FILE_TYPE(3020, "Logo công ty chỉ hỗ trợ JPG, PNG hoặc WebP", HttpStatus.BAD_REQUEST),
    COMPANY_LOGO_UPLOAD_FAILED(3021, "Không thể tải logo công ty lên", HttpStatus.INTERNAL_SERVER_ERROR),
    JOB_HAS_APPLICATIONS(3022, "Không thể xóa tin tuyển dụng đã có ứng viên nộp hồ sơ. Vui lòng đóng tin để ẩn tin tuyển dụng.", HttpStatus.BAD_REQUEST),

    MATCHING_WEIGHT_PROFILE_NOT_FOUND(4001, "Weight profile không tồn tại", HttpStatus.NOT_FOUND),
    MATCHING_DATASET_NOT_FOUND(4002, "Relevance dataset không tồn tại", HttpStatus.NOT_FOUND),
    MATCHING_EVALUATION_FAILED(4003, "Đánh giá matching thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    PIPELINE_JOB_NOT_FOUND(5001, "Pipeline job không tồn tại", HttpStatus.NOT_FOUND),
    PIPELINE_JOB_ALREADY_RUNNING(5002, "Một pipeline job cùng loại đang chạy", HttpStatus.CONFLICT),
    PIPELINE_JOB_CANNOT_CANCEL(5003, "Pipeline job không thể hủy", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
