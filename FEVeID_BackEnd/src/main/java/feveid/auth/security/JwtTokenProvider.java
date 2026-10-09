package feveid.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Lấy khóa bí mật từ file application.properties
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Lấy thời gian sống của token
    @Value("${app.jwt.expiration-milliseconds}")
    private long jwtExpirationDate;

    // Hàm phụ: Chuyển đổi chuỗi bí mật thành đối tượng SecretKey mà thư viện yêu cầu
    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 1. TẠO TOKEN: Được gọi khi user đăng nhập thành công
    public String generateToken(Authentication authentication) {
        String email = authentication.getName(); // Lấy email của user đang đăng nhập
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        return Jwts.builder()
                .subject(email) // Gắn email vào Token (được gọi là subject)
                .issuedAt(currentDate) // Thời điểm phát hành
                .expiration(expireDate) // Thời điểm hết hạn
                .signWith(key()) // "Đóng mộc" bằng chữ ký số
                .compact(); // Nén thành một chuỗi String (Header.Payload.Signature)
    }

    // 2. LẤY EMAIL TỪ TOKEN: Được gọi khi user gửi Token lên để lấy thông tin
    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key()) // Cung cấp chìa khóa để mở Token
                .build()
                .parseSignedClaims(token) // Đọc dữ liệu
                .getPayload(); // Lấy phần nội dung (Payload)

        return claims.getSubject(); // Trả về email
    }

    // 3. KIỂM TRA TOKEN HỢP LỆ: Được gọi trước mỗi request cần bảo mật
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token);
            return true; // Token chuẩn xác, chưa hết hạn, chữ ký đúng
        } catch (Exception ex) {
            // Nếu token bị sửa đổi, hoặc đã hết hạn, thư viện sẽ quăng lỗi và chạy vào đây
            return false;
        }
    }
}