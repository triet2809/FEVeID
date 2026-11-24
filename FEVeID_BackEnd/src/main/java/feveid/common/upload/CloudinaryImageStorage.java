package feveid.common.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

// Bản thật của ImageStorage. Secret nằm trong CLOUDINARY_URL ở server, frontend không bao giờ thấy
@Slf4j
@Component
public class CloudinaryImageStorage implements ImageStorage {

    // null = chưa đặt CLOUDINARY_URL: app vẫn chạy (CI, máy mới), chỉ API upload báo lỗi
    private final Cloudinary cloudinary;

    public CloudinaryImageStorage(@Value("${app.cloudinary.url:}") String cloudinaryUrl) {
        if (cloudinaryUrl.isBlank()) {
            log.warn("CLOUDINARY_URL trống: các API upload ảnh sẽ báo lỗi");
            this.cloudinary = null;
        } else {
            this.cloudinary = new Cloudinary(cloudinaryUrl);  // đọc key, secret, cloud name từ URL
        }
    }

    @Override
    public String upload(byte[] data, String publicId) {
        Map<?, ?> result;
        try {
            result = client().uploader().upload(data, ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,          // cùng publicId -> thay ảnh cũ, không sinh file rác
                    "resource_type", "image"));
        } catch (Exception e) {
            // IOException (mạng) và RuntimeException (Cloudinary trả lỗi, vd sai key) đều gói lại thành một loại
            throw new ImageStorageException("Không tải được ảnh lên, vui lòng thử lại sau", e);
        }
        // URL có số version (".../v1732500000/feveid/events/12/banner.jpg"): ghi đè -> version mới -> URL mới,
        // nên trình duyệt/CDN không hiện lại ảnh cũ đã lưu cache
        return (String) result.get("secure_url");
    }

    @Override
    public void delete(String publicId) {
        try {
            client().uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true));       // xóa luôn bản cache trên CDN
        } catch (Exception e) {
            throw new ImageStorageException("Không xóa được ảnh, vui lòng thử lại sau", e);
        }
    }

    private Cloudinary client() {
        if (cloudinary == null) {
            throw new ImageStorageException("Chưa cấu hình dịch vụ lưu ảnh (CLOUDINARY_URL)");
        }
        return cloudinary;
    }
}
