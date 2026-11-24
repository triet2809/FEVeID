package feveid.common.upload;

// Dịch vụ lưu ảnh (Cloudinary) lỗi hoặc chưa cấu hình -> 502. Là RuntimeException nên transaction đang chạy bị rollback
public class ImageStorageException extends RuntimeException {
    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
