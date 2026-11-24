package feveid.event;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicEventTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    private String hrToken;
    private String memberToken;

    // Đăng ký 25/11 00:00 -> 04/12 23:59; diễn ra 05/12 08:00 -> 17:00 (giờ VN)
    private static final String DEC_5 = """
            {"phases": [
              {"name": "Coming soon", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
              {"name": "Check-in",    "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
              {"name": "Done",        "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
            ]}
            """;
    // Diễn ra 20/12 (muộn hơn DEC_5)
    private static final String DEC_20 = """
            {"phases": [
              {"name": "Coming soon", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
              {"name": "Onsite",      "startsAt": "2026-12-20T08:00:00+07:00", "kind": "ONGOING"},
              {"name": "Done",        "startsAt": "2026-12-20T17:00:00+07:00", "kind": "COMPLETED"}
            ]}
            """;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        hrToken = tokenOf("hr@feveid.test");
        memberToken = tokenOf("an@feveid.test");
        setNow("2026-11-25T10:00");
    }

    // ---------- Ai thấy sự kiện nào ----------

    @Test
    void list_asGuest_seesOnlyPublicAndListedInternal() throws Exception {
        publishedEvent("Đêm nhạc", "PUBLIC", false, DEC_5);
        publishedEvent("Sinh hoạt nội bộ", "INTERNAL", false, DEC_5);
        publishedEvent("Tổng kết kỳ", "INTERNAL", true, DEC_5);
        createEvent("Bản nháp", "PUBLIC", false);
        cancel(publishedEvent("Đã hủy", "PUBLIC", false, DEC_5));

        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Đêm nhạc", "Tổng kết kỳ")));
    }

    @Test
    void list_asMember_seesAllPublished() throws Exception {
        publishedEvent("Đêm nhạc", "PUBLIC", false, DEC_5);
        publishedEvent("Sinh hoạt nội bộ", "INTERNAL", false, DEC_5);
        createEvent("Bản nháp", "PUBLIC", false);

        mockMvc.perform(get("/api/public/events").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Đêm nhạc", "Sinh hoạt nội bộ")));
    }

    @Test
    void list_withInvalidToken_treatedAsGuest() throws Exception {
        // Token sai/hết hạn trên route permitAll: không 401, chỉ coi như chưa đăng nhập
        publishedEvent("Sinh hoạt nội bộ", "INTERNAL", false, DEC_5);

        mockMvc.perform(get("/api/public/events").header("Authorization", "Bearer khong-phai-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void get_unlistedInternal_asGuest_returns404_asMember_returns200() throws Exception {
        long id = publishedEvent("Sinh hoạt nội bộ", "INTERNAL", false, DEC_5);

        mockMvc.perform(get("/api/public/events/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/events/" + id).header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test
    void get_draft_returns404_evenForHr() throws Exception {
        long id = createEvent("Bản nháp", "PUBLIC", false);

        mockMvc.perform(get("/api/public/events/" + id).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_hidesInternalFields() throws Exception {
        long id = publishedEvent("Đêm nhạc", "PUBLIC", false, DEC_5);

        mockMvc.perform(get("/api/public/events/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.currentPhaseName").value("Coming soon"))
                .andExpect(jsonPath("$.startsAt").value("2026-12-05T01:00:00Z"))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.publication").doesNotExist())
                .andExpect(jsonPath("$.publiclyListed").doesNotExist());
    }

    // ---------- Giờ mở đăng ký ----------

    @Test
    void registrationOpen_followsWindow() throws Exception {
        long id = publishedEvent("Đêm nhạc", "PUBLIC", false, DEC_5);

        setNow("2026-11-24T23:59");  // trước giờ mở
        expectRegistrationOpen(id, false);
        setNow("2026-11-25T00:00");  // đúng giờ mở
        expectRegistrationOpen(id, true);
        setNow("2026-12-04T23:59");  // đúng giờ đóng -> đã đóng
        expectRegistrationOpen(id, false);
    }

    @Test
    void registrationOpen_falseWhenCompleted() throws Exception {
        long id = publishedEvent("Đêm nhạc", "PUBLIC", false, DEC_5);
        setNow("2026-12-06T10:00");

        mockMvc.perform(get("/api/public/events/" + id))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.registrationOpen").value(false));
    }

    // ---------- Lọc, tìm kiếm, sắp xếp ----------

    @Test
    void list_upcomingFirst_soonestFirst_thenCompleted() throws Exception {
        publishedEvent("Ngày 20", "PUBLIC", false, DEC_20);
        publishedEvent("Ngày 5", "PUBLIC", false, DEC_5);
        mockMvc.perform(get("/api/public/events"))
                .andExpect(jsonPath("$[*].name", contains("Ngày 5", "Ngày 20")));

        setNow("2026-12-10T10:00");  // Ngày 5 đã kết thúc -> xuống cuối
        mockMvc.perform(get("/api/public/events"))
                .andExpect(jsonPath("$[*].name", contains("Ngày 20", "Ngày 5")));
    }

    @Test
    void list_filterByStatusAndKeyword() throws Exception {
        publishedEvent("Đêm nhạc mùa đông", "PUBLIC", false, DEC_20);
        publishedEvent("Hội thảo", "PUBLIC", false, DEC_5);
        setNow("2026-12-10T10:00");

        mockMvc.perform(get("/api/public/events").param("status", "COMPLETED"))
                .andExpect(jsonPath("$[*].name", contains("Hội thảo")));
        mockMvc.perform(get("/api/public/events").param("q", "  NHẠC "))
                .andExpect(jsonPath("$[*].name", contains("Đêm nhạc mùa đông")));
    }

    @Test
    void list_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/public/events").param("status", "DONE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Cờ publiclyListed ở API quản lý ----------

    @Test
    void admin_publicEvent_alwaysListed() throws Exception {
        long id = createEvent("Đêm nhạc", "PUBLIC", false);

        mockMvc.perform(get("/api/events/" + id).header("Authorization", "Bearer " + hrToken))
                .andExpect(jsonPath("$.publiclyListed").value(true));
    }

    @Test
    void admin_internalEvent_listedOnlyWhenChosen() throws Exception {
        long hidden = createEvent("Sinh hoạt", "INTERNAL", false);
        long shown = createEvent("Tổng kết kỳ", "INTERNAL", true);

        mockMvc.perform(get("/api/events/" + hidden).header("Authorization", "Bearer " + hrToken))
                .andExpect(jsonPath("$.publiclyListed").value(false));
        mockMvc.perform(get("/api/events/" + shown).header("Authorization", "Bearer " + hrToken))
                .andExpect(jsonPath("$.publiclyListed").value(true));
    }

    // ---------- Helpers ----------

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    private void expectRegistrationOpen(long id, boolean open) throws Exception {
        mockMvc.perform(get("/api/public/events/" + id))
                .andExpect(jsonPath("$.registrationOpen").value(open));
    }

    private long createEvent(String name, String visibility, boolean publiclyListed) throws Exception {
        String body = asHr(post("/api/events"), """
                {"name": "%s", "visibility": "%s", "publiclyListed": %s, "capacity": 100,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """.formatted(name, visibility, publiclyListed))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long publishedEvent(String name, String visibility, boolean publiclyListed, String schedule)
            throws Exception {
        long id = createEvent(name, visibility, publiclyListed);
        asHr(put("/api/events/" + id + "/phases"), schedule).andExpect(status().isOk());
        asHr(post("/api/events/" + id + "/publish"), null).andExpect(status().isOk());
        return id;
    }

    private void cancel(long id) throws Exception {
        asHr(post("/api/events/" + id + "/cancel"), null).andExpect(status().isOk());
    }

    private ResultActions asHr(MockHttpServletRequestBuilder request, String json) throws Exception {
        request.header("Authorization", "Bearer " + hrToken);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }
}
