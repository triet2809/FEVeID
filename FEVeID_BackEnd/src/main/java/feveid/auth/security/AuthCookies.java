package feveid.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tạo cookie chứa JWT. Trình duyệt tự lưu và tự gửi cookie này kèm mọi request tới /api,
 * JavaScript của trang không đọc được (HttpOnly) nên script lạ chèn vào trang (XSS) không lấy được token.
 */
@Component
public class AuthCookies {

    public static final String ACCESS_TOKEN = "access_token";

    private final Duration maxAge;
    private final boolean secure;

    public AuthCookies(@Value("${app.jwt.expiration-milliseconds}") long expirationMs,
                       @Value("${app.jwt.cookie-secure}") boolean secure) {
        this.maxAge = Duration.ofMillis(expirationMs); // cookie hết hạn cùng lúc với token bên trong
        this.secure = secure;
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(maxAge).build();
    }

    // Không xóa được cookie từ server, chỉ ghi đè bằng cookie rỗng có Max-Age=0 để trình duyệt tự bỏ
    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(ACCESS_TOKEN, value)
                .httpOnly(true)      // JavaScript không đọc được
                .secure(secure)      // chỉ gửi qua HTTPS (dev có thể tắt bằng JWT_COOKIE_SECURE=false)
                .sameSite("Strict")  // không gửi kèm request xuất phát từ trang web khác
                .path("/api");       // chỉ gửi kèm request tới API, không gửi khi tải ảnh/file tĩnh
    }
}
