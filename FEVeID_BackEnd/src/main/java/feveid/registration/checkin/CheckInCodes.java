package feveid.registration.checkin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

// Mã QR check-in đổi mỗi 30 giây (requirements.md 12.3). Hàm thuần như EventStatusCalculator:
// không đọc DB, không gọi Instant.now(); now do service truyền vào từ Clock
public final class CheckInCodes {
    public static final long WINDOW_SECONDS = 30;
    private static final int CODE_BYTES = 12; // 96 bit -> 16 ký tự, đoán mò không nổi

    private CheckInCodes() {
    }

    // Màn hình chiếu QR gọi hàm này: mã của ô 30 giây đang chạy
    public static String codeAt(byte[] secret, Instant now) {
        return codeFor(secret, windowOf(now));
    }

    // Lúc mã hiện tại hết hạn = giây đầu tiên của ô kế tiếp. Frontend dùng để biết khi nào lấy mã mới
    public static Instant expiresAt(Instant now) {
        return Instant.ofEpochSecond((windowOf(now) + 1) * WINDOW_SECONDS);
    }

    // Member gửi mã lên: đúng nếu khớp ô hiện tại hoặc ô ngay trước (người quét lúc mã sắp đổi).
    // Không cần ô sau: chính server tạo mã nên không có mã "của tương lai"
    public static boolean verify(byte[] secret, String code, Instant now) {
        if (code == null) {
            return false;
        }
        byte[] given = code.getBytes(StandardCharsets.UTF_8);
        long current = windowOf(now);
        for (long window = current - 1; window <= current; window++) {
            byte[] expected = codeFor(secret, window).getBytes(StandardCharsets.UTF_8);
            // So sánh hết rồi mới trả lời (không dừng ở ký tự sai đầu tiên như String.equals)
            if (MessageDigest.isEqual(expected, given)) {
                return true;
            }
        }
        return false;
    }

    // Số thứ tự ô 30 giây chứa now (chia nguyên)
    private static long windowOf(Instant now) {
        return now.getEpochSecond() / WINDOW_SECONDS;
    }

    // "Đóng mộc" HMAC lên số ô -> cắt 12 byte -> Base64 URL-safe để đặt thẳng vào link ?code=...
    private static String codeFor(byte[] secret, long window) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] message = ByteBuffer.allocate(Long.BYTES).putLong(window).array(); // long -> 8 byte
            byte[] full = mac.doFinal(message);                 // luôn 32 byte với SHA-256
            byte[] truncated = Arrays.copyOf(full, CODE_BYTES);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
        } catch (GeneralSecurityException e) {
            // Java luôn có HmacSHA256, nên tới được đây là lỗi môi trường chứ không phải lỗi người dùng
            throw new IllegalStateException("Không tạo được mã check-in", e);
        }
    }
}