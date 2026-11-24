package feveid.common.exception;
// Dữ liệu sai nghiệp vụ mà @Valid không bắt được (vd: lịch giai đoạn sai thứ tự) -> 400
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
