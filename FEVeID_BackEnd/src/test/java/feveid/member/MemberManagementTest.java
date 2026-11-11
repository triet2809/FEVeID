package feveid.member;

import feveid.account.enums.AccountRole;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberManagementTest extends ApiTestSupport {

    private String hrToken;
    private String memberToken;
    private Member ctv;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        ctv = createMember("ctv@feveid.test", AccountRole.MEMBER, "Tran Van B");
        hrToken = tokenOf("hr@feveid.test");
        memberToken = tokenOf("ctv@feveid.test");
    }

    // ---------- Danh sách ----------

    @Test
    void list_asHr_returnsAllMembers() throws Exception {
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void list_filterByStatus_returnsOnlyMatching() throws Exception {
        setStatusDirectly(ctv, MemberStatus.ACTIVE);

        mockMvc.perform(get("/api/members")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("ctv@feveid.test"));
    }

    @Test
    void list_asMember_returns403() throws Exception {
        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    // ---------- Chi tiết ----------

    @Test
    void getById_asHr_returns200() throws Exception {
        mockMvc.perform(get("/api/members/{id}", ctv.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Tran Van B"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/members/{id}", 999999)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNotFound());
    }

    // ---------- Đổi trạng thái ----------

    @Test
    void changeStatus_ctvToActive_returns200() throws Exception {
        changeStatus(ctv.getId(), "ACTIVE", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberStatus").value("ACTIVE"));

        assertThat(memberRepository.findById(ctv.getId()).orElseThrow().getMemberStatus())
                .isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void changeStatus_disqualifiedToActive_returns409() throws Exception {
        setStatusDirectly(ctv, MemberStatus.DISQUALIFIED);

        changeStatus(ctv.getId(), "ACTIVE", hrToken)
                .andExpect(status().isConflict());

        assertThat(memberRepository.findById(ctv.getId()).orElseThrow().getMemberStatus())
                .isEqualTo(MemberStatus.DISQUALIFIED);
    }

    @Test
    void changeStatus_invalidValue_returns400() throws Exception {
        changeStatus(ctv.getId(), "BOSS", hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_asMember_returns403() throws Exception {
        changeStatus(ctv.getId(), "ACTIVE", memberToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void disqualify_blocksLoginAndOldToken() throws Exception {
        changeStatus(ctv.getId(), "DISQUALIFIED", hrToken)
                .andExpect(status().isOk());

        // Đăng nhập mới → 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ctv@feveid.test", "password": "%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());

        // Token lấy TRƯỚC khi bị disqualify cũng không dùng được nữa (filter kiểm tra lại isEnabled, Bài 5.1)
        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Helper ----------

    private ResultActions changeStatus(Long id, String status, String token) throws Exception {
        return mockMvc.perform(patch("/api/members/{id}/status", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "%s"}
                        """.formatted(status)));
    }

    // Đặt trạng thái thẳng qua repository để dựng dữ liệu cho test, không đi qua luật chuyển trạng thái
    private void setStatusDirectly(Member member, MemberStatus status) {
        member.setMemberStatus(status);
        memberRepository.save(member);
    }
}