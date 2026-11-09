package feveid.support;

import com.jayway.jsonpath.JsonPath;
import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class ApiTestSupport {
    protected static final String PASSWORD = "Secret123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected AccountRepository accountRepository;
    @Autowired
    protected MemberRepository memberRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected void cleanDatabase() {
        memberRepository.deleteAll();
        accountRepository.deleteAll();
    }

    protected Member createMember(String email, AccountRole role, String fullName) {
        Account account = accountRepository.save(Account.builder()
                .email(email)
                .role(role)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
        return memberRepository.save(Member.builder()
                .account(account)
                .fullName(fullName)
                .build());
    }

    protected String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}