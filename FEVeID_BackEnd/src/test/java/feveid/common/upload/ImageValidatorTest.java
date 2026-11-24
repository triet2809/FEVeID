package feveid.common.upload;

import feveid.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorTest {

    // Tạo file giả: các byte đầu cho trước, phần sau toàn số 0 cho đủ totalLength
    private static byte[] sized(int totalLength, int... header) {
        byte[] data = new byte[totalLength];
        for (int i = 0; i < header.length; i++) {
            data[i] = (byte) header[i];
        }
        return data;
    }

    // File 100 byte với các byte đầu cho trước
    private static byte[] header(int... header) {
        return sized(100, header);
    }

    private static byte[] text(String content) {
        return content.getBytes(StandardCharsets.US_ASCII);
    }

    // "RIFF" + 4 byte kích thước + loại (WEBP / WAVE...)
    private static byte[] riff(String type) {
        return Arrays.copyOf(text("RIFF\0\0\0\0" + type), 100);
    }

    private static void assertRejected(byte[] data) {
        assertThatThrownBy(() -> ImageValidator.validate(data))
                .isInstanceOf(BadRequestException.class)
                .hasMessageMatching("(?s).*\\S.*");
    }

    // ---- Ảnh hợp lệ ----

    @Test
    void jpeg() {
        assertThat(ImageValidator.validate(header(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(ImageType.JPEG);
    }

    @Test
    void png() {
        assertThat(ImageValidator.validate(header(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .isEqualTo(ImageType.PNG);
    }

    @Test
    void webp() {
        assertThat(ImageValidator.validate(riff("WEBP"))).isEqualTo(ImageType.WEBP);
    }

    @Test
    void exactlyMaxSize_isAccepted() {
        assertThat(ImageValidator.validate(sized(ImageValidator.MAX_BYTES, 0xFF, 0xD8, 0xFF)))
                .isEqualTo(ImageType.JPEG);
    }

    // ---- Kích thước ----

    @Test
    void emptyFile() {
        assertRejected(new byte[0]);
    }

    @Test
    void oneByteOverMax_isRejected_evenIfHeaderIsJpeg() {
        assertRejected(sized(ImageValidator.MAX_BYTES + 1, 0xFF, 0xD8, 0xFF));
    }

    // ---- Không phải ảnh được nhận ----

    @Test
    void gif() {
        assertRejected(text("GIF89a" + "x".repeat(50)));
    }

    @Test
    void svg_isRejected_becauseItCanContainScript() {
        assertRejected(text("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"));
    }

    @Test
    void pdf() {
        assertRejected(text("%PDF-1.7" + "x".repeat(50)));
    }

    @Test
    void textFileRenamedToJpg() {
        // tên file không được gửi vào validate: chỉ nội dung mới quyết định
        assertRejected(text("hello, toi la file .txt doi ten thanh .jpg"));
    }

    @Test
    void riffButNotWebp_isRejected() {
        // file âm thanh WAV cũng bắt đầu bằng "RIFF": phải kiểm cả byte 8-11
        assertRejected(riff("WAVE"));
    }

    @Test
    void pngWithOneWrongByte() {
        assertRejected(header(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x00));
    }

    // ---- File ngắn: không được ném ArrayIndexOutOfBoundsException (sẽ thành 500) ----

    @Test
    void twoBytesOnly() {
        assertRejected(sized(2, 0xFF, 0xD8));
    }

    @Test
    void riffWithoutType() {
        assertRejected(text("RIFF"));
    }

    @Test
    void singleByte() {
        assertRejected(sized(1, 0x89));
    }
}
