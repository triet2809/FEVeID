package feveid.member;

import feveid.account.enums.AccountRole;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MemberProfileTest extends ApiTestSupport{

    private static final String EMAIL = "a@feveid.test";
    private String token;

    @BeforeEach
    public void setUp() throws Exception {
        cleanDatabase();
        createMember("a@feveid.test", AccountRole.MEMBER, "Nguyen Van A");
        token = tokenOf(EMAIL);
    }

    @Test
    void getMe_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/members/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.memberStatus").value("CTV"));
    }

    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_changesPhoneAndAvatar() throws Exception {
        mockMvc.perform(patch("/api/members/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone": "0901234567", "avatarUrl": "https://img.feveid.test/a.png"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0901234567"));

        //Đọc lại từ DB: chứng minh đã lưu thật, không chỉ trả về đúng trong response
        Member member = memberRepository.findByAccountEmail(EMAIL).orElseThrow();
        assertThat(member.getPhone()).isEqualTo("0901234567");
        assertThat(member.getAvatarUrl()).isEqualTo("https://img.feveid.test/a.png");
    }

    @Test
    void updateMe_nullField_keepOldValue() throws Exception {
        String auth = "Bearer " + token;
        mockMvc.perform(patch("/api/members/me")
                .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone": "0901234567"}
                        """))
                .andExpect(status().isOk());

        //Lần 2 chỉ gửi avatarUrl -> phone phải giữ nguyên (ngữ nghãi PATCH)
        mockMvc.perform(patch("/api/members/me")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"avatarUrl": "https://img.feveid.test/a.png"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0901234567"));
    }

    @Test
    void updateMe_cannotChangeFullNameOrStatus() throws Exception {
        mockMvc.perform(patch("/api/members/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName": "Hacker", "memberStatus": "ACTIVE", "phone": "0901234567"}
                        """))
                .andExpect(status().isOk());
        Member member = memberRepository.findByAccountEmail(EMAIL).orElseThrow();
        assertThat(member.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(member.getMemberStatus()).isEqualTo(MemberStatus.CTV);
    }

    @Test
    void updateMe_phoneTooLong_returns400() throws Exception {
        mockMvc.perform(patch("/api/members/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phone": "012345678900123456789"}
                        """))
                .andExpect(status().isBadRequest());
    }

}
