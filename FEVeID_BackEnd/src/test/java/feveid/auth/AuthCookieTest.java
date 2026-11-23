package feveid.auth;

import feveid.account.enums.AccountRole;
import feveid.auth.security.AuthCookies;
import feveid.support.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Đăng nhập bằng cookie httpOnly + chống CSRF kiểu double-submit (cookie XSRF-TOKEN ↔ header X-XSRF-TOKEN).
 * Các test ở đây giả lập đúng những gì trình duyệt làm: tự gửi lại cookie, còn header thì do JS của trang tự gắn.
 */
public class AuthCookieTest extends ApiTestSupport {

    private static final String EMAIL = "cookie@feveid.test";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @BeforeEach
    void setUp() {
        cleanDatabase();
        createMember(EMAIL, AccountRole.MEMBER, "Nguyen Van Cookie");
    }

    private MockHttpServletResponse login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andReturn().getResponse();
    }

    private Cookie loginCookie() throws Exception {
        return login().getCookie(AuthCookies.ACCESS_TOKEN);
    }

    private String setCookieHeaderOf(MockHttpServletResponse response, String cookieName) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst().orElseThrow();
    }

    private MockHttpServletResponse patchPhone(String phone, Cookie... cookies) throws Exception {
        return patchPhone(phone, null, cookies);
    }

    private MockHttpServletResponse patchPhone(String phone, String csrfHeader, Cookie... cookies) throws Exception {
        var request = patch("/api/members/me")
                .cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone": "%s"}
                        """.formatted(phone));
        if (csrfHeader != null) {
            request.header(CSRF_HEADER, csrfHeader);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    @Test
    void login_setsHttpOnlyStrictCookie_andNoTokenInBody() throws Exception {
        MockHttpServletResponse response = login();

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getContentAsString()).isEmpty(); // JS của trang không nhận được token

        Cookie cookie = response.getCookie(AuthCookies.ACCESS_TOKEN);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge()).isEqualTo(86400); // = app.jwt.expiration-milliseconds / 1000
        assertThat(setCookieHeaderOf(response, AuthCookies.ACCESS_TOKEN))
                .contains("SameSite=Strict")
                .contains("Secure");
    }

    @Test
    void login_alsoHandsOutCsrfCookie_readableByJs() throws Exception {
        Cookie csrf = login().getCookie(CSRF_COOKIE);

        assertThat(csrf).isNotNull();
        assertThat(csrf.isHttpOnly()).isFalse(); // frontend phải đọc được để gửi lại trong header
    }

    @Test
    void cookieAlone_authenticatesGetRequest() throws Exception {
        mockMvc.perform(get("/api/members/me").cookie(loginCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void forgedCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .cookie(new Cookie(AuthCookies.ACCESS_TOKEN, "eyJhbGciOiJIUzI1NiJ9.gia.mao")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cookie_mutatingRequestWithoutCsrfHeader_returns403_andChangesNothing() throws Exception {
        // Giống trang web lạ gửi form tới API của mình: trình duyệt tự kèm cookie, nhưng không có header CSRF
        MockHttpServletResponse login = login();

        MockHttpServletResponse response = patchPhone("0999999999",
                login.getCookie(AuthCookies.ACCESS_TOKEN), login.getCookie(CSRF_COOKIE));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(memberRepository.findByAccountEmail(EMAIL).orElseThrow().getPhone()).isNull();
    }

    @Test
    void cookie_mutatingRequestWithWrongCsrfHeader_returns403() throws Exception {
        MockHttpServletResponse login = login();

        MockHttpServletResponse response = patchPhone("0999999999", "doan-bua",
                login.getCookie(AuthCookies.ACCESS_TOKEN), login.getCookie(CSRF_COOKIE));

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void cookie_mutatingRequestWithMatchingCsrfHeader_succeeds() throws Exception {
        // Đúng như frontend sẽ làm: đọc cookie XSRF-TOKEN rồi gửi lại giá trị đó trong header
        MockHttpServletResponse login = login();
        Cookie csrf = login.getCookie(CSRF_COOKIE);

        MockHttpServletResponse response = patchPhone("0901234567", csrf.getValue(),
                login.getCookie(AuthCookies.ACCESS_TOKEN), csrf);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(memberRepository.findByAccountEmail(EMAIL).orElseThrow().getPhone()).isEqualTo("0901234567");
    }

    @Test
    void bearerHeader_mutatingRequestWithoutCsrf_stillWorks() throws Exception {
        // Swagger/Postman/test tự gắn header → không dùng cookie → không cần CSRF
        String token = loginCookie().getValue();

        mockMvc.perform(patch("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "0901234567"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void invalidAuthorizationHeader_doesNotFallBackToCookie() throws Exception {
        // Có header thì chỉ tin header; nếu lùi về cookie thì request này vừa bỏ qua CSRF vừa đăng nhập được
        mockMvc.perform(patch("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sai")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "0999999999"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(memberRepository.findByAccountEmail(EMAIL).orElseThrow().getPhone()).isNull();
    }

    @Test
    void logout_expiresCookie() throws Exception {
        MockHttpServletResponse login = login();
        Cookie csrf = login.getCookie(CSRF_COOKIE);

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/logout")
                        .cookie(login.getCookie(AuthCookies.ACCESS_TOKEN), csrf)
                        .header(CSRF_HEADER, csrf.getValue()))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(204);
        Cookie cleared = response.getCookie(AuthCookies.ACCESS_TOKEN);
        assertThat(cleared.getValue()).isEmpty();
        assertThat(cleared.getMaxAge()).isZero(); // Max-Age=0 → trình duyệt xóa cookie ngay
    }
}
