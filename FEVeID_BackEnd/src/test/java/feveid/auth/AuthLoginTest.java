package feveid.auth;


import com.jayway.jsonpath.JsonPath;
import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.enums.AccountStatus;
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
import org.springframework.test.web.servlet.ResultActions;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class AuthLoginTest {
    private static final String PASSWORD = "Secret123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        accountRepository.deleteAll();
    }

    private Account createAccount(String email, AccountStatus status) {
        Account acount = Account.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .accountStatus(status)
                .build();
        return accountRepository.save(acount);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    @Test
    void activeAccount_correctPassword_returnsToken() throws Exception {
        createAccount("active@feveid.test", AccountStatus.ACTIVE);

        login("active@feveid.test", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

    }


    @Test
    void wrongPassword_return401() throws Exception {
        createAccount("active@feveid.test", AccountStatus.ACTIVE);

        login("active@feveid.test", "wrongPassword")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccount_return401() throws Exception {
        createAccount("disable@feveid.test", AccountStatus.DISABLED);

        login("disable@feveid.test", PASSWORD)
                .andExpect(status().isUnauthorized());

    }

    @Test
    void lockedAccount_return401() throws Exception {
        createAccount("locked@feveid.test", AccountStatus.LOCKED);
        login("locked@feveid.test", PASSWORD)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenStopsWorking_afterAccountIsDisable() throws Exception {
        Account account = createAccount("later-disable@feveid.test", AccountStatus.ACTIVE);
        String body = login("later-disable@feveid.test", PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        account.setAccountStatus(AccountStatus.DISABLED);
        accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/" + account.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

    }
    @Test
    void requestWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }


}
