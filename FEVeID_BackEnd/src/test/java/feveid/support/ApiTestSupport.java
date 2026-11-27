package feveid.support;

import feveid.auth.security.AuthCookies;
import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import feveid.department.entity.Department;
import feveid.department.repository.DepartmentMembershipRepository;
import feveid.department.repository.DepartmentRepository;
import feveid.event.repository.EventRepository;
import feveid.registration.repository.RegistrationRepository;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import feveid.term.repository.TermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired
    protected DepartmentRepository departmentRepository;
    @Autowired
    protected DepartmentMembershipRepository membershipRepository;
    @Autowired
    protected TermRepository termRepository;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected RegistrationRepository registrationRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void cleanDatabase() {
        DatabaseCleaner.clean(jdbcTemplate);
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
        // token nằm trong cookie access_token; test vẫn gửi bằng header Bearer (không cần CSRF)
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andReturn().getResponse()
                .getCookie(AuthCookies.ACCESS_TOKEN).getValue();
    }
    protected Department createDepartment(String name) {
        return departmentRepository.save(Department.builder().name(name).build());
    }
}