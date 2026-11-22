package feveid.auth.controller;


import feveid.auth.dto.LoginRequest;
import feveid.auth.security.AuthCookies;
import feveid.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Thành công → 204, token nằm trong cookie httpOnly access_token (không có trong body)")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest loginRequest) {
        // chuyen du lieu xuong Service xu ly va nhan lai token
        String token = authService.login(loginRequest);
        // Set-Cookie: trình duyệt tự lưu token, các request sau tự gửi kèm
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.create(token).toString())
                .build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Xóa cookie access_token trên trình duyệt")
    public ResponseEntity<Void> logout() {
        // JWT không thu hồi được ở server (stateless); chỉ bảo trình duyệt bỏ cookie đi
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .build();
    }
}
