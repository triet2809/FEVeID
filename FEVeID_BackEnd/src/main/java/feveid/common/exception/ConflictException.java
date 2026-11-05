package feveid.common.exception;
// Dữ liệu bị trùng/ conflict (email đã tồn tại) -> 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
