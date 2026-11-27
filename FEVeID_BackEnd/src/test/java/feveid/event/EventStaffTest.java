package feveid.event;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.event.dto.EventStaffRequest;
import feveid.event.dto.StaffAssignmentRequest;
import feveid.event.enums.StaffRole;
import feveid.event.repository.EventStaffRepository;
import feveid.event.service.EventStaffService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static feveid.event.service.impl.EventStaffServiceImpl.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Team sự kiện (requirements.md 5.2, 11.1, Bài 13.1)
class EventStaffTest extends ApiTestSupport {

    @Autowired
    private EventStaffService eventStaffService;
    @Autowired
    private EventStaffRepository eventStaffRepository;

    private String hrToken;
    private String anToken;
    private Member an;
    private Member binh;
    private Member chi;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        an = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        binh = createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Binh");
        chi = createMember("chi@feveid.test", AccountRole.MEMBER, "Pham Chi");
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
    }

    // ---------- Xem / thay team ----------

    @Test
    void put_assignsTeam_organizersFirstThenByName() throws Exception {
        long eventId = createEvent();

        putStaff(eventId, team(binh, "SUPPORTER", chi, "ORGANIZER", an, "SUPPORTER"), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Pham Chi"))
                .andExpect(jsonPath("$[0].role").value("ORGANIZER"))
                .andExpect(jsonPath("$[1].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$[2].fullName").value("Tran Binh"))
                .andExpect(jsonPath("$[2].role").value("SUPPORTER"));

        getStaff(eventId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].memberId").value(chi.getId()));
    }

    @Test
    void put_replacesWholeTeam_canSwapRoleOfSameMember() throws Exception {
        long eventId = createEvent();
        putStaff(eventId, team(an, "ORGANIZER", binh, "SUPPORTER"), hrToken).andExpect(status().isOk());

        // An đổi vai trò, Bình bị gỡ, Chi được thêm. Đổi vai trò = xóa dòng cũ + thêm dòng mới cùng (event, member):
        // nếu xóa bằng deleteAll(list) thì Hibernate INSERT trước DELETE -> đụng unique -> 500
        putStaff(eventId, team(an, "SUPPORTER", chi, "ORGANIZER"), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Pham Chi"))
                .andExpect(jsonPath("$[1].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$[1].role").value("SUPPORTER"));
    }

    @Test
    void put_emptyList_clearsTeam() throws Exception {
        long eventId = createEvent();
        putStaff(eventId, team(an, "ORGANIZER"), hrToken).andExpect(status().isOk());

        putStaff(eventId, "{\"staff\": []}", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void put_onlyChangesThisEvent() throws Exception {
        long eventId = createEvent();
        long otherEventId = createEvent();
        putStaff(otherEventId, team(binh, "ORGANIZER"), hrToken).andExpect(status().isOk());

        putStaff(eventId, team(an, "ORGANIZER"), hrToken).andExpect(status().isOk());

        getStaff(otherEventId, hrToken)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Tran Binh"));
    }

    // ---------- Dữ liệu sai ----------

    @Test
    void put_sameMemberTwice_returns400() throws Exception {
        long eventId = createEvent();

        putStaff(eventId, team(an, "ORGANIZER", an, "SUPPORTER"), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(DUPLICATE_MEMBER));
    }

    @Test
    void put_unknownMember_returns400() throws Exception {
        long eventId = createEvent();

        putStaff(eventId, """
                {"staff": [{"memberId": 999999, "role": "ORGANIZER"}]}
                """, hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MEMBER_NOT_FOUND));
    }

    @Test
    void put_disqualifiedMember_returns400() throws Exception {
        long eventId = createEvent();
        binh.setMemberStatus(MemberStatus.DISQUALIFIED);
        memberRepository.save(binh);

        putStaff(eventId, team(an, "ORGANIZER", binh, "SUPPORTER"), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MEMBER_DISQUALIFIED));
        assertThat(eventStaffRepository.count()).isZero(); // lỗi thì không lưu ai cả
    }

    @Test
    void put_missingRole_returns400() throws Exception {
        long eventId = createEvent();

        putStaff(eventId, """
                {"staff": [{"memberId": %d}]}
                """.formatted(an.getId()), hrToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Trạng thái sự kiện ----------

    @Test
    void put_cancelledEvent_returns409() throws Exception {
        long eventId = createEvent();
        asUser(post("/api/events/" + eventId + "/cancel"), null, hrToken).andExpect(status().isOk());

        putStaff(eventId, team(an, "ORGANIZER"), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(EVENT_CANCELLED));
    }

    @Test
    void deleteDraft_withTeam_returns204_teamGone() throws Exception {
        long eventId = createEvent();
        putStaff(eventId, team(an, "ORGANIZER"), hrToken).andExpect(status().isOk());

        asUser(delete("/api/events/" + eventId), null, hrToken).andExpect(status().isNoContent());

        assertThat(eventStaffRepository.count()).isZero(); // ON DELETE CASCADE trong V13
    }

    // ---------- Phân quyền / không tồn tại ----------

    @Test
    void member_returns403() throws Exception {
        long eventId = createEvent();

        getStaff(eventId, anToken).andExpect(status().isForbidden());
        putStaff(eventId, team(an, "ORGANIZER"), anToken).andExpect(status().isForbidden());
    }

    @Test
    void unknownEvent_returns404() throws Exception {
        getStaff(999_999L, hrToken).andExpect(status().isNotFound());
        putStaff(999_999L, team(an, "ORGANIZER"), hrToken).andExpect(status().isNotFound());
    }

    // ---------- Đồng thời ----------

    @Test
    void put_concurrently_allSucceed_noDuplicateKey() throws Exception {
        long eventId = createEvent();
        EventStaffRequest request = new EventStaffRequest(List.of(
                new StaffAssignmentRequest(an.getId(), StaffRole.ORGANIZER),
                new StaffAssignmentRequest(binh.getId(), StaffRole.SUPPORTER)));

        // Bỏ khóa dòng sự kiện trong replaceStaff thì test này đỏ: 2 transaction cùng xóa (chưa thấy dòng của nhau)
        // rồi cùng thêm -> duplicate key -> DataIntegrityViolationException
        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    return eventStaffService.replaceStaff(eventId, request);
                }));
            }
            startGun.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS); // exception trong luồng -> ExecutionException -> test đỏ
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(eventStaffRepository.count()).isEqualTo(2);
    }

    // ---------- Helpers ----------

    private long createEvent() throws Exception {
        String body = asUser(post("/api/events"), """
                {"name": "Đêm nhạc", "visibility": "INTERNAL"}
                """, hrToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // team(an, "ORGANIZER", binh, "SUPPORTER") -> {"staff": [{"memberId": .., "role": ".."}, ...]}
    private static String team(Object... memberRolePairs) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < memberRolePairs.length; i += 2) {
            Member member = (Member) memberRolePairs[i];
            items.add("{\"memberId\": %d, \"role\": \"%s\"}".formatted(member.getId(), memberRolePairs[i + 1]));
        }
        return "{\"staff\": [" + String.join(", ", items) + "]}";
    }

    private ResultActions getStaff(long eventId, String token) throws Exception {
        return asUser(get("/api/events/" + eventId + "/staff"), null, token);
    }

    private ResultActions putStaff(long eventId, String json, String token) throws Exception {
        return asUser(put("/api/events/" + eventId + "/staff"), json, token);
    }

    private ResultActions asUser(MockHttpServletRequestBuilder request, String json, String token)
            throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }
}
