package feveid.department;

import feveid.account.enums.AccountRole;
import feveid.department.entity.Department;
import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;
import feveid.member.entity.Member;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentAccessTest extends ApiTestSupport {

    private Department media;
    private Department event;
    private Member leaderMedia;
    private Member viceMedia;
    private Member memberMedia;
    private Member memberEvent;
    private Member noDept;
    private String leaderToken;
    private String viceToken;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        media = createDepartment("Ban Truyền Thông");
        event = createDepartment("Ban Sự Kiện");

        leaderMedia = createMember("leader@feveid.test", AccountRole.MEMBER, "Nguyen Van Truong");
        viceMedia = createMember("vice@feveid.test", AccountRole.MEMBER, "Tran Thi Pho");
        memberMedia = createMember("an@feveid.test", AccountRole.MEMBER, "Le Van An");
        memberEvent = createMember("binh@feveid.test", AccountRole.MEMBER, "Pham Thi Binh");
        noDept = createMember("cuong@feveid.test", AccountRole.MEMBER, "Do Van Cuong");

        // Gán thẳng qua repository cho nhanh: test này kiểm tra phân quyền, không kiểm tra API gán ban
        join(leaderMedia, media, DepartmentPosition.LEADER);
        join(viceMedia, media, DepartmentPosition.VICE_LEADER);
        join(memberMedia, media, DepartmentPosition.MEMBER);
        join(memberEvent, event, DepartmentPosition.MEMBER);

        leaderToken = tokenOf("leader@feveid.test");
        viceToken = tokenOf("vice@feveid.test");
        memberToken = tokenOf("an@feveid.test");
    }

    // ---------- Danh sách thành viên của ban ----------

    @Test
    void leader_listOwnDepartment_returns200() throws Exception {
        listMembers(media.getId(), leaderToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void leader_listOtherDepartment_returns403() throws Exception {
        listMembers(event.getId(), leaderToken).andExpect(status().isForbidden());
    }

    @Test
    void normalMember_listOwnDepartment_returns403() throws Exception {
        listMembers(media.getId(), memberToken).andExpect(status().isForbidden());
    }

    @Test
    void leader_listUnknownDepartment_returns403() throws Exception {
        // 403 chứ không phải 404: không cho người ngoài dò xem ban nào tồn tại
        listMembers(9999L, leaderToken).andExpect(status().isForbidden());
    }

    @Test
    void formerLeader_listOldDepartment_returns403() throws Exception {
        Member former = createMember("former@feveid.test", AccountRole.MEMBER, "Vo Van Cu");
        membershipRepository.save(DepartmentMembership.builder()
                .member(former)
                .department(media)
                .position(DepartmentPosition.LEADER)
                .endDate(LocalDate.now())   // đã rời chức
                .build());

        listMembers(media.getId(), tokenOf("former@feveid.test")).andExpect(status().isForbidden());
    }

    @Test
    void viceLeader_listOwnDepartment_returns200() throws Exception {
        listMembers(media.getId(), viceToken).andExpect(status().isOk());
    }

    // ---------- Hồ sơ từng thành viên ----------

    @Test
    void leader_viewMemberInOwnDepartment_returns200() throws Exception {
        viewMember(memberMedia.getId(), leaderToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Le Van An"));
    }

    @Test
    void leader_viewMemberInOtherDepartment_returns403() throws Exception {
        viewMember(memberEvent.getId(), leaderToken).andExpect(status().isForbidden());
    }

    @Test
    void leader_viewMemberWithoutDepartment_returns403() throws Exception {
        viewMember(noDept.getId(), leaderToken).andExpect(status().isForbidden());
    }

    @Test
    void viceLeader_viewMemberInOtherDepartment_returns403() throws Exception {
        viewMember(memberEvent.getId(), viceToken).andExpect(status().isForbidden());
    }

    // ---------- Trưởng ban chỉ được xem, không được gán ----------

    @Test
    void leader_assignDepartment_returns403() throws Exception {
        mockMvc.perform(put("/api/members/{id}/department", memberMedia.getId())
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId": %d, "position": "VICE_LEADER"}
                                """.formatted(media.getId())))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private void join(Member member, Department department, DepartmentPosition position) {
        membershipRepository.save(DepartmentMembership.builder()
                .member(member)
                .department(department)
                .position(position)
                .build());
    }

    private ResultActions listMembers(Long departmentId, String token) throws Exception {
        return mockMvc.perform(get("/api/departments/{id}/members", departmentId)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions viewMember(Long memberId, String token) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", memberId)
                .header("Authorization", "Bearer " + token));
    }
}