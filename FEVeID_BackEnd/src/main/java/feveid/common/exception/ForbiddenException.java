package feveid.common.exception;
// Biết người gọi là ai nhưng nghiệp vụ không cho phép (vd khách đăng ký sự kiện chỉ dành cho thành viên) -> 403.
// Khác AccessDeniedException (do @PreAuthorize ném, message chung): ở đây trả message cụ thể
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
