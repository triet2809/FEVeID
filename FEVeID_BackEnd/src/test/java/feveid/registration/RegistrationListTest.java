package feveid.registration;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationListTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    private String hrToken;
    private String presidentToken;
    private String anToken;
    private String binhToken;

    private static final String SCHEDULE = """
            {"phases": [
              {"name": "Coming soon", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
              {"name": "Check-in",    "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
              {"name": "Done",        "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
            ]}
            """;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("president@feveid.test", AccountRole.PRESIDENT, "Chu Nhiem");
        Member an = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        an.setStudentCode("SE170001");
        an.setPhone("0911111111");
        memberRepository.save(an);
        createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Binh");
        hrToken = tokenOf("hr@feveid.test");
        presidentToken = tokenOf("president@feveid.test");
        anToken = tokenOf("an@feveid.test");
        binhToken = tokenOf("binh@feveid.test");
        setNow("2026-11-26T10:00");
    }

    // ---------- Sự kiện của tôi ----------

    @Test
    void me_returnsOnlyMyRegistrations_newestFirst() throws Exception {
        long first = publishedEvent("Đêm nhạc");
        long second = publishedEvent("Hội thảo");
        registerMember(first, anToken);
        registerMember(second, anToken);
        registerMember(first, binhToken);   // của người khác
        registerGuest(first, "khach@gmail.com");

        getAs("/api/registrations/me", anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].event.name", contains("Hội thảo", "Đêm nhạc")))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].cancellable").value(true))
                .andExpect(jsonPath("$[0].event.status").value("UPCOMING"))
                .andExpect(jsonPath("$[0].event.startsAt").value("2026-12-05T01:00:00Z"))
                .andExpect(jsonPath("$[0].event.endsAt").value("2026-12-05T10:00:00Z"));
    }

    @Test
    void me_includesCancelled_notCancellable() throws Exception {
        long eventId = publishedEvent("Đêm nhạc");
        long registrationId = registerMember(eventId, anToken);
        asUser(post("/api/registrations/" + registrationId + "/cancel"), null, anToken)
                .andExpect(status().isOk());

        getAs("/api/registrations/me", anToken)
                .andExpect(jsonPath("$[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].cancelledAt").exists())
                .andExpect(jsonPath("$[0].cancellable").value(false));
    }

    @Test
    void me_afterRegistrationCloses_notCancellable() throws Exception {
        registerMember(publishedEvent("Đêm nhạc"), anToken);
        setNow("2026-12-05T09:00");  // sự kiện đang diễn ra, đã đóng đăng ký

        getAs("/api/registrations/me", anToken)
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].cancellable").value(false))
                .andExpect(jsonPath("$[0].event.status").value("ONGOING"));
    }

    @Test
    void me_eventCancelledByBcn_showsCancelledEvent() throws Exception {
        long eventId = publishedEvent("Đêm nhạc");
        registerMember(eventId, anToken);
        asUser(post("/api/events/" + eventId + "/cancel"), null, hrToken).andExpect(status().isOk());

        getAs("/api/registrations/me", anToken)
                .andExpect(jsonPath("$[0].event.status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].cancellable").value(false));
    }

    @Test
    void me_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/registrations/me")).andExpect(status().isUnauthorized());
    }

    // ---------- Danh sách đăng ký cho HR/BCN ----------

    @Test
    void eventList_asHr_showsMembersAndGuests_oldestFirst() throws Exception {
        long eventId = publishedEvent("Đêm nhạc");
        registerMember(eventId, anToken);
        registerGuest(eventId, "khach@gmail.com");

        getAs("/api/events/" + eventId + "/registrations", hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].member").value(true))
                .andExpect(jsonPath("$[0].memberId").exists())
                .andExpect(jsonPath("$[0].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$[0].email").value("an@feveid.test"))
                .andExpect(jsonPath("$[0].phone").value("0911111111"))
                .andExpect(jsonPath("$[0].studentCodeOrSchool").value("SE170001"))
                .andExpect(jsonPath("$[1].member").value(false))
                .andExpect(jsonPath("$[1].memberId").doesNotExist())
                .andExpect(jsonPath("$[1].fullName").value("Le Khach"))
                .andExpect(jsonPath("$[1].email").value("khach@gmail.com"))
                .andExpect(jsonPath("$[1].studentCodeOrSchool").value("FPT University"));
    }

    @Test
    void eventList_onlyThatEvent() throws Exception {
        long first = publishedEvent("Đêm nhạc");
        long second = publishedEvent("Hội thảo");
        registerMember(first, anToken);
        registerMember(second, binhToken);

        getAs("/api/events/" + first + "/registrations", presidentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fullName", contains("Nguyen Van An")));
    }

    @Test
    void eventList_asMember_returns403() throws Exception {
        long eventId = publishedEvent("Đêm nhạc");

        getAs("/api/events/" + eventId + "/registrations", anToken).andExpect(status().isForbidden());
    }

    @Test
    void eventList_unknownEvent_returns404() throws Exception {
        getAs("/api/events/999999/registrations", hrToken).andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    private ResultActions getAs(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private long registerMember(long eventId, String token) throws Exception {
        return idOf(asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", token));
    }

    private void registerGuest(long eventId, String email) throws Exception {
        mockMvc.perform(post("/api/public/events/" + eventId + "/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Le Khach", "email": "%s", "phone": "0912345678",
                                 "school": "FPT University"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private long publishedEvent(String name) throws Exception {
        setNow("2026-11-25T10:00");
        long id = idOf(asUser(post("/api/events"), """
                {"name": "%s", "visibility": "PUBLIC", "capacity": 100,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """.formatted(name), hrToken));
        asUser(put("/api/events/" + id + "/phases"), SCHEDULE, hrToken).andExpect(status().isOk());
        asUser(post("/api/events/" + id + "/publish"), null, hrToken).andExpect(status().isOk());
        setNow("2026-11-26T10:00");
        return id;
    }

    private static long idOf(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
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
