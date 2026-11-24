package feveid.common.upload;

// Các loại ảnh nhận upload (banner sự kiện, sau này avatar). Không nhận SVG (có thể chứa <script>), GIF, HEIC
public enum ImageType {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    private final String mimeType;

    ImageType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }
}
