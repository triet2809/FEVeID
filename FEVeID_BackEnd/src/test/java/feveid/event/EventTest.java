package feveid.event;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.member.entity.Member;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventTest extends ApiTestSupport {

    // Mỗi test tự đặt "bây giờ" để kiểm tra trạng thái tính theo lịch
    @MockitoBean
    private Clock clock;

    private String hrToken;
    private String presidentToken;
    private String memberToken;
    private Member owner;

    // Lịch mẫu (giờ VN, gửi dạng +07:00): Coming soon 01/12 -> Check-in 05/12 08:00 -> Biểu diễn 09:00 -> Done 17:00
    private static final String SCHEDULE = """
            {"phases": [
              {"name": "Coming soon", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
              {"name": "Check-in",    "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
              {"name": "Biểu diễn",   "startsAt": "2026-12-05T09:00:00+07:00", "kind": "ONGOING"},
              {"name": "Done",        "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
            ]}
            """;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("president@feveid.test", AccountRole.PRESIDENT, "Chu Nhiem");
        owner = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        hrToken = tokenOf("hr@feveid.test");
        presidentToken = tokenOf("president@feveid.test");
        memberToken = tokenOf("an@feveid.test");
        setNow("2026-11-25T10:00");
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    // ---------- Tạo / sửa ----------

    @Test
    void create_asHr_returns201_draftWithoutSchedule() throws Exception {
        create(info("Đêm nhạc", owner.getId()), hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Đêm nhạc"))
                .andExpect(jsonPath("$.publication").value("DRAFT"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentPhaseName").doesNotExist())
                .andExpect(jsonPath("$.phases").isEmpty())
                .andExpect(jsonPath("$.startsAt").doesNotExist())
                .andExpect(jsonPath("$.owner.fullName").value("Nguyen Van An"))
                // gửi lên +07:00, trả về dạng UTC (Z): cùng một mốc thời gian
                .andExpect(jsonPath("$.registrationOpensAt").value("2026-11-24T17:00:00Z"));
    }

    @Test
    void create_asPresident_returns201() throws Exception {
        create(info("Đêm nhạc", null), presidentToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").doesNotExist());  // BCN gán leader sau khi chọn team
    }

    @Test
    void create_asMember_returns403() throws Exception {
        create(info("Đêm nhạc", null), memberToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void create_registrationClosesBeforeOpens_returns400() throws Exception {
        create("""
                {"name": "Đêm nhạc", "visibility": "PUBLIC",
                 "registrationOpensAt": "2026-12-04T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-01T00:00:00+07:00"}
                """, hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_onlyRegistrationOpens_returns400() throws Exception {
        create("""
                {"name": "Đêm nhạc", "visibility": "PUBLIC",
                 "registrationOpensAt": "2026-12-04T00:00:00+07:00"}
                """, hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownOwner_returns400() throws Exception {
        create(info("Đêm nhạc", 999_999L), hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_changesInfo() throws Exception {
        long id = createEvent();

        mockMvc.perform(put("/api/events/" + id)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(info("Đêm nhạc mùa đông", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Đêm nhạc mùa đông"))
                .andExpect(jsonPath("$.owner").doesNotExist());
    }

    @Test
    void get_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/events/999999").header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsNewestFirst() throws Exception {
        createEvent();
        create(info("Hội thảo", null), hrToken);

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Hội thảo"));
    }

    // ---------- Lịch giai đoạn ----------

    @Test
    void replacePhases_valid_returnsScheduleAndDerivedTimes() throws Exception {
        long id = createEvent();

        replacePhases(id, SCHEDULE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases.length()").value(4))
                .andExpect(jsonPath("$.phases[0].id").isNumber())
                .andExpect(jsonPath("$.phases[1].name").value("Check-in"))
                // bắt đầu = ONGOING đầu tiên, kết thúc = COMPLETED đầu tiên (đổi sang UTC: trừ 7 tiếng)
                .andExpect(jsonPath("$.startsAt").value("2026-12-05T01:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2026-12-05T10:00:00Z"))
                .andExpect(jsonPath("$.status").value("DRAFT"));  // chưa công bố thì lịch không đổi trạng thái
    }

    @Test
    void replacePhases_replacesOldSchedule() throws Exception {
        long id = createEvent();
        replacePhases(id, SCHEDULE);

        replacePhases(id, """
                {"phases": [
                  {"name": "Sắp diễn ra", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
                  {"name": "Diễn ra",     "startsAt": "2026-12-06T08:00:00+07:00", "kind": "ONGOING"},
                  {"name": "Kết thúc",    "startsAt": "2026-12-06T17:00:00+07:00", "kind": "COMPLETED"}
                ]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases.length()").value(3))
                .andExpect(jsonPath("$.phases[2].name").value("Kết thúc"));

        // orphanRemoval: 4 giai đoạn cũ đã bị DELETE khỏi DB, không chỉ bị bỏ khỏi list.
        // Dùng hàm có @EntityGraph: findById trả phases dạng LAZY, đọc ngoài transaction sẽ ném LazyInitializationException
        assertThat(eventRepository.findAllByOrderByCreatedAtDesc().getFirst().getPhases()).hasSize(3);
    }

    @Test
    void replacePhases_invalidSchedule_returns400() throws Exception {
        long id = createEvent();

        // thiếu UPCOMING ở đầu: PhaseScheduleValidator (Bài 10.1) chặn
        replacePhases(id, """
                {"phases": [
                  {"name": "Diễn ra",  "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
                  {"name": "Kết thúc", "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
                ]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void replacePhases_blankName_returns400() throws Exception {
        long id = createEvent();

        // @Valid trên List<@Valid PhaseRequest> kiểm tra cả từng phần tử
        replacePhases(id, """
                {"phases": [
                  {"name": " ",        "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
                  {"name": "Diễn ra",  "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
                  {"name": "Kết thúc", "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
                ]}
                """)
                .andExpect(status().isBadRequest());
    }

    // ---------- Công bố + trạng thái theo giờ ----------

    @Test
    void publish_withoutSchedule_returns400() throws Exception {
        long id = createEvent();

        publish(id).andExpect(status().isBadRequest());
    }

    @Test
    void publish_withoutRegistrationWindow_returns400() throws Exception {
        long id = idOf(create(info("Đêm nhạc", null, null, null), hrToken));
        replacePhases(id, SCHEDULE);

        publish(id).andExpect(status().isBadRequest());
    }

    @Test
    void publish_registrationClosesAfterEnd_returns400() throws Exception {
        long id = idOf(create(info("Đêm nhạc", null,
                "2026-11-25T00:00:00+07:00", "2026-12-06T00:00:00+07:00"), hrToken));
        replacePhases(id, SCHEDULE);

        publish(id).andExpect(status().isBadRequest());
    }

    @Test
    void publish_eventAlreadyEnded_returns400() throws Exception {
        long id = createScheduledEvent();
        setNow("2026-12-06T00:00");

        publish(id).andExpect(status().isBadRequest());
    }

    @Test
    void publish_twice_returns409() throws Exception {
        long id = createScheduledEvent();
        publish(id).andExpect(status().isOk());

        publish(id).andExpect(status().isConflict());
    }

    @Test
    void published_statusFollowsClock() throws Exception {
        long id = createScheduledEvent();
        publish(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publication").value("PUBLISHED"))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.currentPhaseName").value("Coming soon"));

        // Không có job nào chạy: chỉ đổi "bây giờ" rồi đọc lại là trạng thái đổi theo
        setNow("2026-12-05T08:30");
        getEvent(id)
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.currentPhaseName").value("Check-in"));

        setNow("2026-12-05T17:00");
        getEvent(id)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentPhaseName").value("Done"));
    }

    // ---------- Khóa sau khi kết thúc / hủy ----------

    @Test
    void completed_scheduleAndInfoLocked_returns409() throws Exception {
        long id = createPublishedEvent();
        setNow("2026-12-05T18:00");

        replacePhases(id, SCHEDULE).andExpect(status().isConflict());
        mockMvc.perform(put("/api/events/" + id)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(info("Đổi tên", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_published_thenLocked() throws Exception {
        long id = createPublishedEvent();

        cancel(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.currentPhaseName").doesNotExist());

        cancel(id).andExpect(status().isConflict());
        replacePhases(id, SCHEDULE).andExpect(status().isConflict());
        publish(id).andExpect(status().isConflict());  // hủy là vĩnh viễn
    }

    @Test
    void cancel_completed_returns409() throws Exception {
        long id = createPublishedEvent();
        setNow("2026-12-05T18:00");

        cancel(id).andExpect(status().isConflict());
    }

    // ---------- Chuyển giai đoạn ngay ----------

    @Test
    void startNow_movesPhaseToNow() throws Exception {
        long id = createPublishedEvent();
        long checkInId = phaseId(id, 1);
        setNow("2026-12-05T07:30");  // mở cửa sớm 30 phút

        mockMvc.perform(post("/api/events/" + id + "/phases/" + checkInId + "/start-now")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.currentPhaseName").value("Check-in"))
                .andExpect(jsonPath("$.phases[1].startsAt").value("2026-12-05T00:30:00Z"));
    }

    @Test
    void startNow_breaksOrder_returns400_andKeepsSchedule() throws Exception {
        long id = createPublishedEvent();
        long doneId = phaseId(id, 3);
        setNow("2026-12-05T07:30");  // chưa tới Check-in mà đã bấm "Done"

        mockMvc.perform(post("/api/events/" + id + "/phases/" + doneId + "/start-now")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());

        // transaction rollback: giờ của "Done" vẫn là 17:00
        getEvent(id).andExpect(jsonPath("$.phases[3].startsAt").value("2026-12-05T10:00:00Z"));
    }

    @Test
    void startNow_onDraft_returns409() throws Exception {
        long id = createScheduledEvent();

        mockMvc.perform(post("/api/events/" + id + "/phases/" + phaseId(id, 1) + "/start-now")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isConflict());
    }

    // ---------- Xóa ----------

    @Test
    void delete_draft_returns204() throws Exception {
        long id = createScheduledEvent();

        deleteEvent(id).andExpect(status().isNoContent());

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void delete_published_returns409() throws Exception {
        long id = createPublishedEvent();

        deleteEvent(id).andExpect(status().isConflict());
    }

    // ---------- Helper ----------

    private String info(String name, Long ownerId) {
        return info(name, ownerId, "2026-11-25T00:00:00+07:00", "2026-12-04T23:59:00+07:00");
    }

    private String info(String name, Long ownerId, String opensAt, String closesAt) {
        return """
                {"name": "%s", "description": "Mô tả", "location": "Hội trường A", "visibility": "PUBLIC",
                 "capacity": 200, "ownerMemberId": %s,
                 "registrationOpensAt": %s, "registrationClosesAt": %s}
                """.formatted(name, ownerId, quoted(opensAt), quoted(closesAt));
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions create(String json, String token) throws Exception {
        return mockMvc.perform(post("/api/events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    // Đọc "$.id" từ body của response 201
    private static long idOf(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createEvent() throws Exception {
        return idOf(create(info("Đêm nhạc", owner.getId()), hrToken));
    }

    private long createScheduledEvent() throws Exception {
        long id = createEvent();
        replacePhases(id, SCHEDULE).andExpect(status().isOk());
        return id;
    }

    private long createPublishedEvent() throws Exception {
        long id = createScheduledEvent();
        publish(id).andExpect(status().isOk());
        return id;
    }

    // id của giai đoạn thứ index (0 = Coming soon, 1 = Check-in, 2 = Biểu diễn, 3 = Done)
    private long phaseId(long eventId, int index) throws Exception {
        String body = getEvent(eventId).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.phases[" + index + "].id")).longValue();
    }

    private ResultActions getEvent(long id) throws Exception {
        return mockMvc.perform(get("/api/events/" + id).header("Authorization", "Bearer " + hrToken));
    }

    private ResultActions replacePhases(long id, String json) throws Exception {
        return mockMvc.perform(put("/api/events/" + id + "/phases")
                .header("Authorization", "Bearer " + hrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions publish(long id) throws Exception {
        return mockMvc.perform(post("/api/events/" + id + "/publish").header("Authorization", "Bearer " + hrToken));
    }

    private ResultActions cancel(long id) throws Exception {
        return mockMvc.perform(post("/api/events/" + id + "/cancel").header("Authorization", "Bearer " + hrToken));
    }

    private ResultActions deleteEvent(long id) throws Exception {
        return mockMvc.perform(delete("/api/events/" + id).header("Authorization", "Bearer " + hrToken));
    }
}
