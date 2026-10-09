package feveid.auth.service.impl;

import feveid.auth.dto.JwtAuthResponse;
import feveid.auth.dto.LoginRequest;
import feveid.auth.security.JwtTokenProvider;
import feveid.auth.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    public JwtAuthResponse login(LoginRequest loginRequest) {
        // 1. Gọi AuthenticationManager để kiểm tra email và password có khớp trong DB không
        // (Nó sẽ tự động gọi ngầm tới UserDetailsServiceImpl của bạn ở dưới)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        // 2. Nếu không bị văng lỗi (tức là pass đúng), lưu trạng thái đăng nhập vào Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Gọi "Nhà máy" tạo JWT Token
        String token = jwtTokenProvider.generateToken(authentication);

        // 4. Đóng gói Token và trả về cho Controller
        return new JwtAuthResponse(token, "Bearer");
    }
}
