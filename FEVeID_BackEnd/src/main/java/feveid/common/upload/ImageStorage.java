package feveid.common.upload;

// Nơi lưu ảnh đã kiểm tra (ImageValidator). Service chỉ biết interface này, không biết Cloudinary:
// test thay bằng bản giả (@MockitoBean), sau này đổi dịch vụ khác chỉ cần viết thêm một class
public interface ImageStorage {

    // Lưu ảnh với publicId cố định (vd "feveid/events/12/banner"), đã có thì ghi đè.
    // Trả về URL https để lưu vào DB. Lỗi -> ImageStorageException
    String upload(byte[] data, String publicId);

    // Xóa ảnh; publicId không tồn tại thì bỏ qua. Lỗi -> ImageStorageException
    void delete(String publicId);
}
