package feveid.account;


import com.jayway.jsonpath.JsonPath;
import feveid.TestcontainersConfiguration;
import feveid.account.dto.CreateAccountRequest;
import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import feveid.account.service.AccountService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AccountRegistrationTest {
    private final static String PASSWORD = "secret1123";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AccountService accountService;

    private String hrToken;

    @BeforeEach
    public void setup() throws Exception {
        memberRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(Account.builder()
                .email("hr@feveid.test")
                .role(AccountRole.HR)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
        hrToken = tokenOf("hr@feveid.test");

    }

    @Test
    void register_createMemberProfile() throws Exception {
        String body = register("""
                {"email": "a@feveid.test", "password": "Secret123", "fullName": "Nguyen Van A", "studentCode": "SE12345"}
                """)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        // Json trả Integer với số nhỏ -> ép qua Number rồi lấy long
        Long accountId = ((Number) JsonPath.read(body, "$.id")).longValue();

        Member member = memberRepository.findByAccountId(accountId).orElseThrow();
        assertThat(member.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(member.getStudentCode()).isEqualTo("SE12345");
        assertThat(member.getMemberStatus()).isEqualTo(MemberStatus.CTV);
    }

    @Test
    void register_missingFullName_returns400() throws Exception {
        register("""
                {"email": "a@feveid.test", "password": "Secret123", "studentCode": "SE12345"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateStudentCode_returns409() throws Exception {
        register("""
                {"email": "a@feveid.test", "password": "Secret123", "fullName": "Nguyen Van A", "studentCode": "SE12345"}
                """)
                .andExpect(status().isCreated());
        register("""
                {"email": "b@feveid.test", "password": "Secret123", "fullName": "Nguyen Van B", "studentCode": "SE12345"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Mã số sinh viên đã được sử dụng"));
        assertThat(accountRepository.existsByEmail("b@feveid.test")).isFalse();
    }

    @Test
    void register_memberSaveFails_accountIsRolledBack() throws Exception {
        //Gọi thẳng qua service -> không gọi qua @Valid của controller -> fullName 101 kí tự lọt vào được
        CreateAccountRequest request = new CreateAccountRequest(
                "rollback@feveid.test", "Secret123", "A".repeat(101), null);
        assertThrows(Exception.class, () -> accountService.createAccount(request));
        // Lưu member lỗi, account cũng phải bị rollback
        assertThat(accountRepository.existsByEmail("rollback@feveid.test")).isFalse();
    }

    @Test
    void register_blankStudentCode_isStoreAsNull() throws Exception {
        String body = register("""
                {"email": "a@feveid.test", "password": "Secret123", "fullName": "Nguyen Van A", "studentCode": ""}
                """)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long accountId = ((Number) JsonPath.read(body, "$.id")).longValue();

        register("""
                {"email": "b@feveid.test", "password": "Secret123", "fullName": "Nguyen Van B", "studentCode": ""}
                """)
                .andExpect(status().isCreated());


        Member member = memberRepository.findByAccountId(accountId).orElseThrow();
        assertThat(member.getStudentCode()).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions register(String json) throws Exception {
        return mockMvc.perform(post("/api/accounts/register")
                .header("Authorization", "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
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


}

