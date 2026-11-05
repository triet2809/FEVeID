package feveid.common.exception;


import com.jayway.jsonpath.JsonPath;
import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApiErrorTest {
    private static final String PASSSWORD = "secret123";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    PasswordEncoder passwordEncoder;


    private String hrToken;
    @BeforeEach
    void setUp() throws Exception {
        accountRepository.deleteAll();

        accountRepository.save(Account.builder()
                .email("hr@feveid.test")
                .passwordHash(passwordEncoder.encode(PASSSWORD))
                .role(AccountRole.HR)
                .build());
        hrToken = loginAndGetToken("hr@feveid.test");
    }
    private String loginAndGetToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSSWORD)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
    @Test
    void getAccount_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999")
                .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản"));
    }
    @Test
    void register_duplicatEmail_returns409() throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                .header("Authorization", "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "hr@feveid.test", "password": "Another123"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email đã được sử dụng"));
    }
    @Test
    void login_malformedJson_returns400WithFriendlyMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("Authorization", "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "a@b.c", 
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dữ liệu không đúng định dạng"));
    }
    @Test
    void wrongHttpMethod_returns405() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }
    @Test
    void tokenOfDeleteAccount_returns401() throws Exception {
        accountRepository.deleteAll();
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isUnauthorized());
    }

}
