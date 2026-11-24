package feveid.common.upload;

import feveid.common.exception.BadRequestException;

import java.nio.charset.StandardCharsets;

// Kiểm tra file upload có đúng là ảnh JPG/PNG/WebP không, dựa vào NỘI DUNG file (các byte đầu = "magic bytes").
// KHÔNG tin tên file (.jpg) hay header Content-Type: cả hai do client tự đặt, đổi tên file .exe thành .jpg là qua.
public final class ImageValidator {

    // Giới hạn của gói Cloudinary free: ảnh tối đa 10MB
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    private ImageValidator() {
    }

    /**
     * Ảnh hợp lệ -> trả về loại ảnh; sai -> throw BadRequestException (message tiếng Việt cho người dùng).
     *
     * @param data toàn bộ nội dung file (MultipartFile.getBytes())
     */
    // 1. byte có dấu: 0xFF, 0xD8, 0x89 > 127 nên phải ép (byte); 0x50, 0x4E,... < 127 thì không cần
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    public static ImageType validate(byte[] data) {
        if (data.length == 0) {
            throw new BadRequestException("File rỗng");
        }
        if (data.length > MAX_BYTES) {
            throw new BadRequestException("Ảnh tối đa 10MB");
        }

        // (3) So magic bytes
        if (hasBytesAt(data, 0, JPEG_MAGIC)) {
            return ImageType.JPEG;
        }
        if (hasBytesAt(data, 0, PNG_MAGIC)) {
            return ImageType.PNG;
        }
        // WAV, AVI cũng bắt đầu bằng "RIFF" -> phải xem thêm byte 8-11 (byte 4-7 là kích thước, bỏ qua)
        if (hasBytesAt(data, 0, RIFF) && hasBytesAt(data, 8, WEBP)) {
            return ImageType.WEBP;
        }

        // (4) Danh sách cho phép (allowlist): không khớp loại nào đã biết thì từ chối
        throw new BadRequestException("Chỉ nhận ảnh JPG, PNG hoặc WebP");
    }

    // data[offset], data[offset+1]... có trùng với expected không
    private static boolean hasBytesAt(byte[] data, int offset, byte[] expected) {
        // (5) File ngắn hơn đoạn cần so -> chắc chắn không khớp; trả false thay vì để đọc tràn mảng
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}