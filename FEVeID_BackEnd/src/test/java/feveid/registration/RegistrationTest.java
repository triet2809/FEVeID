package feveid.registration;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.common.exception.ConflictException;
import feveid.registration.enums.RegistrationStatus;
import feveid.registration.service.RegistrationService;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static feveid.registration.service.impl.RegistrationServiceImpl.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    // Gọi thẳng service (không qua MockMvc) cho test nhiều luồng
    @Autowired
    private RegistrationService registrationService;

    private String hrToken;
    private String anToken;
    private String binhToken;

    // Đăng ký 25/11 00:00 -> 04/12 23:59; diễn ra 05/12 (giờ VN)
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
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Binh");
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
        binhToken = tokenOf("binh@feveid.test");
        setNow("2026-11-26T10:00");  // đang mở đăng ký
    }

    // ---------- Đăng ký ----------

    @Test
    void register_openEvent_returns201_confirmed() throws Exception {
        long eventId = publishedEvent(100);

        register(eventId, anToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.eventName").value("Đêm nhạc"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void register_internalEvent_member_returns201() throws Exception {
        long eventId = publishedEvent(100, "INTERNAL");

        register(eventId, anToken).andExpect(status().isCreated());
    }

    @Test
    void register_withoutLogin_returns401() throws Exception {
        long eventId = publishedEvent(100);

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_missingEventId_returns400() throws Exception {
        asUser(post("/api/registrations"), "{}", anToken).andExpect(status().isBadRequest());
    }

    @Test
    void register_unknownEvent_returns404() throws Exception {
        register(999_999L, anToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(EVENT_NOT_FOUND));
    }

    @Test
    void register_draftEvent_returns404() throws Exception {
        long eventId = createEvent(100, "PUBLIC");

        register(eventId, anToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(EVENT_NOT_FOUND));
    }

    @Test
    void register_beforeOpens_returns409() throws Exception {
        long eventId = publishedEvent(100);
        setNow("2026-11-24T23:59");

        register(eventId, anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_OPEN));
    }

    @Test
    void register_atClosingTime_returns409() throws Exception {
        long eventId = publishedEvent(100);
        setNow("2026-12-04T23:59");

        register(eventId, anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_OPEN));
    }

    @Test
    void register_cancelledEvent_returns409() throws Exception {
        long eventId = publishedEvent(100);
        asUser(post("/api/events/" + eventId + "/cancel"), null, hrToken).andExpect(status().isOk());

        register(eventId, anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_OPEN));
    }

    @Test
    void register_twice_returns409() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken).andExpect(status().isCreated());

        register(eventId, anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ALREADY_REGISTERED));
    }

    @Test
    void register_full_returns409() throws Exception {
        long eventId = publishedEvent(1);
        register(eventId, anToken).andExpect(status().isCreated());

        register(eventId, binhToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(FULL));
    }

    @Test
    void register_noCapacity_isUnlimited() throws Exception {
        long eventId = publishedEvent(null);

        register(eventId, anToken).andExpect(status().isCreated());
        register(eventId, binhToken).andExpect(status().isCreated());
    }

    // ---------- Hủy ----------

    @Test
    void cancel_own_returns200_andFreesSeat() throws Exception {
        long eventId = publishedEvent(1);
        long registrationId = idOf(register(eventId, anToken));

        cancel(registrationId, anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
        // Đăng ký đã hủy không giữ chỗ nữa
        register(eventId, binhToken).andExpect(status().isCreated());
    }

    @Test
    void cancel_thenRegisterAgain_returns201() throws Exception {
        long eventId = publishedEvent(100);
        cancel(idOf(register(eventId, anToken)), anToken).andExpect(status().isOk());

        register(eventId, anToken).andExpect(status().isCreated());
    }

    @Test
    void cancel_someoneElses_returns404() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));

        cancel(registrationId, binhToken).andExpect(status().isNotFound());
    }

    @Test
    void cancel_twice_returns409() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        cancel(registrationId, anToken).andExpect(status().isOk());

        cancel(registrationId, anToken).andExpect(status().isConflict());
    }

    @Test
    void cancel_afterRegistrationCloses_returns409() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow("2026-12-05T00:00");

        cancel(registrationId, anToken).andExpect(status().isConflict());
    }

    // ---------- Nhiều người bấm cùng lúc ----------

    @Test
    void concurrent_lastSeat_onlyOneGetsIt() throws Exception {
        long eventId = publishedEvent(1);
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String email = "racer" + i + "@feveid.test";
            createMember(email, AccountRole.MEMBER, "Racer " + i);
            emails.add(email);
        }

        List<Boolean> results = raceToRegister(emails, eventId);

        assertThat(results).filteredOn(ok -> ok).hasSize(1);
        assertThat(activeRegistrations(eventId)).isEqualTo(1);
    }

    @Test
    void concurrent_sameMemberDoubleClick_onlyOneRegistration() throws Exception {
        long eventId = publishedEvent(100);

        // Cùng một người bấm 5 lần gần như cùng lúc (mạng chậm, bấm liên tục)
        List<Boolean> results = raceToRegister(List.of("an@feveid.test", "an@feveid.test", "an@feveid.test",
                "an@feveid.test", "an@feveid.test"), eventId);

        assertThat(results).filteredOn(ok -> ok).hasSize(1);
        assertThat(activeRegistrations(eventId)).isEqualTo(1);
    }

    // ---------- Helpers ----------

    /**
     * Mỗi email chạy register() trên một luồng riêng, tất cả cùng xuất phát khi "bắn súng" (latch).
     * Trả về true = đăng ký được, false = bị từ chối bằng ConflictException (409).
     * Lỗi khác (vd vi phạm unique index -> DataIntegrityViolationException) làm test ĐỎ ở future.get()
     */
    private List<Boolean> raceToRegister(List<String> emails, long eventId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(emails.size());
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (String email : emails) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    try {
                        registrationService.register(email, eventId);
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                }));
            }
            startGun.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private long activeRegistrations(long eventId) {
        return registrationRepository.countByEventIdAndStatusIn(eventId, RegistrationStatus.ACTIVE);
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    private ResultActions register(long eventId, String token) throws Exception {
        return asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", token);
    }

    private ResultActions cancel(long registrationId, String token) throws Exception {
        return asUser(post("/api/registrations/" + registrationId + "/cancel"), null, token);
    }

    private long createEvent(Integer capacity, String visibility) throws Exception {
        return idOf(asUser(post("/api/events"), """
                {"name": "Đêm nhạc", "visibility": "%s", "capacity": %s,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """.formatted(visibility, capacity), hrToken));
    }

    private long publishedEvent(Integer capacity) throws Exception {
        return publishedEvent(capacity, "PUBLIC");
    }

    private long publishedEvent(Integer capacity, String visibility) throws Exception {
        setNow("2026-11-25T10:00");  // công bố khi sự kiện chưa kết thúc, rồi trả lại giờ của test
        long id = createEvent(capacity, visibility);
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
