package feveid.common.config;

import feveid.auth.security.AuthCookies;
import feveid.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.util.WebUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Token nằm trong cookie → trình duyệt tự gửi kèm cả khi request do trang web khác gây ra (CSRF).
                // spa(): server gửi cookie XSRF-TOKEN (JS đọc được); mọi POST/PUT/PATCH/DELETE phải gửi lại giá trị đó
                // trong header X-XSRF-TOKEN. Trang web lạ không đọc được cookie của mình nên không giả được header.
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers("/api/auth/login") // chưa đăng nhập thì chưa có gì để giả mạo
                        // CSRF chỉ nguy hiểm khi cookie access_token là thứ chứng minh danh tính. Bỏ qua khi:
                        // - có header Authorization: chỉ dùng header, không dùng cookie (xem JwtAuthenticationFilter),
                        //   mà trình duyệt không tự gắn header này cho trang web lạ;
                        // - không có cookie access_token: không có quyền gì để mượn (request ẩn danh vẫn ra 401 như cũ).
                        .ignoringRequestMatchers(request -> request.getHeader(HttpHeaders.AUTHORIZATION) != null
                                || WebUtils.getCookie(request, AuthCookies.ACCESS_TOKEN) == null)
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/auth/login",
                                "/api/auth/logout" // token hết hạn vẫn phải đăng xuất (xóa cookie) được
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/departments", "/api/departments/*").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}