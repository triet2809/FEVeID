package feveid.department;

import feveid.account.enums.AccountRole;
import feveid.department.entity.Department;
import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentMembershipTest extends ApiTestSupport {

    private String hrToken;
    private String memberToken;
    private Member an;
    private Member binh;
    private Department media;
    private Department event;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        an = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        binh = createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Thi Binh");
        hrToken = tokenOf("hr@feveid.test");
        memberToken = tokenOf("an@feveid.test");
        media = createDepartment("Ban Truyền Thông");
        event = createDepartment("Ban Sự Kiện");
    }

    // ---------- Gán / chuyển / đổi chức vụ ----------

    @Test
    void assign_newMember_returns200() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName").value("Ban Truyền Thông"))
                .andExpect(jsonPath("$.position").value("MEMBER"));

        DepartmentMembership current = membershipRepository.findByMemberIdAndEndDateIsNull(an.getId()).orElseThrow();
        assertThat(current.getPosition()).isEqualTo(DepartmentPosition.MEMBER);
    }

    @Test
    void transfer_closesOldAndOpensNew() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());

        assign(an.getId(), event.getId(), "MEMBER", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName").value("Ban Sự Kiện"));

        List<DepartmentMembership> history = historyOf(an);
        assertThat(history).hasSize(2);
        assertThat(history).filteredOn(m -> m.getEndDate() == null).hasSize(1);          // chỉ 1 dòng đang mở
        assertThat(history).filteredOn(m -> LocalDate.now().equals(m.getEndDate())).hasSize(1);  // dòng cũ đã đóng
    }

    @Test
    void changePosition_sameDepartment_keepsHistory() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());
        assign(an.getId(), media.getId(), "VICE_LEADER", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("VICE_LEADER"));

        assertThat(historyOf(an)).hasSize(2);
    }

    @Test
    void assign_sameDepartmentAndPosition_isNoOp() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());

        assertThat(historyOf(an)).hasSize(1);
    }

    @Test
    void assign_secondLeader_returns409() throws Exception {
        assign(an.getId(), media.getId(), "LEADER", hrToken).andExpect(status().isOk());

        assign(binh.getId(), media.getId(), "LEADER", hrToken)
                .andExpect(status().isConflict());

        assertThat(historyOf(binh)).isEmpty();
    }

    @Test
    void assign_disqualifiedMember_returns409() throws Exception {
        an.setMemberStatus(MemberStatus.DISQUALIFIED);
        memberRepository.save(an);

        assign(an.getId(), media.getId(), "MEMBER", hrToken)
                .andExpect(status().isConflict());
    }

    @Test
    void assign_unknownDepartment_returns404() throws Exception {
        assign(an.getId(), 999999L, "MEMBER", hrToken)
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_unknownMember_returns404() throws Exception {
        assign(999999L, media.getId(), "MEMBER", hrToken)
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_asMember_returns403() throws Exception {
        assign(an.getId(), media.getId(), "LEADER", memberToken)
                .andExpect(status().isForbidden());

        // Không có dòng nào được tạo: member không tự phong mình làm Trưởng ban được
        assertThat(historyOf(an)).isEmpty();
    }

    @Test
    void assign_invalidPosition_returns400() throws Exception {
        assign(an.getId(), media.getId(), "BOSS", hrToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Rút khỏi ban ----------

    @Test
    void remove_closesMembership_returns204() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());

        mockMvc.perform(delete("/api/members/{id}/department", an.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNoContent());

        assertThat(membershipRepository.findByMemberIdAndEndDateIsNull(an.getId())).isEmpty();
        assertThat(historyOf(an)).hasSize(1);   // lịch sử vẫn còn, chỉ bị đóng lại
    }

    @Test
    void remove_whenNotInDepartment_returns404() throws Exception {
        mockMvc.perform(delete("/api/members/{id}/department", an.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNotFound());
    }
    @Test
    void disqualify_closesCurrentMembership() throws Exception {
        // An đang là Trưởng ban Truyền Thông
        assign(an.getId(), media.getId(), "LEADER", hrToken).andExpect(status().isOk());

        // HR loại An khỏi CLB (CTV -> DISQUALIFIED là hợp lệ theo MemberStatus.canTransitionTo)
        mockMvc.perform(patch("/api/members/{id}/status", an.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISQUALIFIED"}
                                """))
                .andExpect(status().isOk());

        // 1. Dòng của An đã bị đóng, nhưng lịch sử vẫn còn (Personal Journey cần)
        assertThat(membershipRepository.findByMemberIdAndEndDateIsNull(an.getId())).isEmpty();
        assertThat(historyOf(an)).hasSize(1);

        // 2. An không còn xuất hiện trong danh sách thành viên hiện tại của ban
        mockMvc.perform(get("/api/departments/{id}/members", media.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 3. Ghế Trưởng ban đã trống -> gán được Trưởng ban mới
        assign(binh.getId(), media.getId(), "LEADER", hrToken)
                .andExpect(status().isOk());
    }
    // ---------- Danh sách thành viên của ban ----------

    @Test
    void listMembers_currentOnly_leaderFirst() throws Exception {
        assign(an.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());
        assign(binh.getId(), media.getId(), "LEADER", hrToken).andExpect(status().isOk());
        Member cuong = createMember("cuong@feveid.test", AccountRole.MEMBER, "Le Van Cuong");
        assign(cuong.getId(), media.getId(), "MEMBER", hrToken).andExpect(status().isOk());
        assign(cuong.getId(), event.getId(), "MEMBER", hrToken).andExpect(status().isOk());   // Cuong đã chuyển đi

        mockMvc.perform(get("/api/departments/{id}/members", media.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Tran Thi Binh"))     // LEADER đứng đầu dù tên xếp sau
                .andExpect(jsonPath("$[0].position").value("LEADER"))
                .andExpect(jsonPath("$[1].fullName").value("Nguyen Van An"));
    }

    @Test
    void listMembers_withoutToken_returns401() throws Exception {
        // Chứng minh "/api/departments/*" trong SecurityConfig KHÔNG mở công khai đường dẫn 2 đoạn
        mockMvc.perform(get("/api/departments/{id}/members", media.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Helper ----------

    private ResultActions assign(Long memberId, Long departmentId, String position, String token) throws Exception {
        return mockMvc.perform(put("/api/members/{id}/department", memberId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"departmentId": %d, "position": "%s"}
                        """.formatted(departmentId, position)));
    }

    private List<DepartmentMembership> historyOf(Member member) {
        return membershipRepository.findAll().stream()
                .filter(m -> m.getMember().getId().equals(member.getId()))
                .toList();
    }
}