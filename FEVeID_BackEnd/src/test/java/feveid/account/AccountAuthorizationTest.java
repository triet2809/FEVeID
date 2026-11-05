package feveid.account;

import com.jayway.jsonpath.JsonPath;
import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import feveid.auth.security.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AccountAuthorizationTest {
    private static final String PASSWORD = "Secret123";
    private static final String NEW_ACCOUNT_JSON = """
            {"email": "new@feveid.test", "password": "Secret123"}
            """;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String hrToken;
    private String presidentToken;
    private String memberToken;
    private Account member;
    private Account otherMember;

    @BeforeEach
    void setUp() throws Exception {
        accountRepository.deleteAll();
        createAccount("hr@feveid.test", AccountRole.HR);
        createAccount("president@feveid.test", AccountRole.PRESIDENT);
        member = createAccount("member@feveid.test", AccountRole.MEMBER);
        otherMember = createAccount("other@feveid.test", AccountRole.MEMBER);

        hrToken = tokenOf("hr@feveid.test");
        presidentToken = tokenOf("president@feveid.test");
        memberToken = tokenOf("member@feveid.test");
    }


    private Account createAccount(String email, AccountRole role) {
        return accountRepository.save(Account.builder()
                .email(email)
                .role(role)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSWORD))
        ).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
    // ----------------POST /api/accounts/register----------------

    @Test
    void hr_canRegisterAccount() throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                .header("Authorization", "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ACCOUNT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void member_cannotRegisterAccount() throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_ACCOUNT_JSON))
                .andExpect(status().isForbidden());
    }
    //---------------- GET /api/accounts ----------------
    @Test
    void president_canListAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + presidentToken))
                .andExpect(status().isOk());
    }
    @Test
    void member_cannotListAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
    // ----------------GET /api/accounts/{id} ----------------
    @Test
    void member_canViewOwnAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts/" + member.getId())
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }
    @Test
    void member_cannotViewOtherAccount() throws  Exception {
        mockMvc.perform(get("/api/accounts/" + otherMember.getId())
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
    @Test
    void hr_canViewAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/" + member.getId())
                .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }
}
