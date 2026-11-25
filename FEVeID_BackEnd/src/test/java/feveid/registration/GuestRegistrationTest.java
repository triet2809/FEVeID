package feveid.registration;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.common.exception.ConflictException;
import feveid.registration.dto.GuestRegistrationRequest;
import feveid.registration.entity.Registration;
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

class GuestRegistrationTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    @Autowired
    private RegistrationService registrationService;

    private String hrToken;
    private String anToken;

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
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
        setNow("2026-11-26T10:00");
    }

    // ---------- Đăng ký thành công ----------

    @Test
    void register_publicEvent_returns201_andStoresGuestInfo() throws Exception {
        long eventId = publishedEvent(100, "PUBLIC", false);

        registerGuest(eventId, guest("Le Khach", " Khach@Gmail.com ", "0912 345 678"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.eventName").value("Đêm nhạc"));

        Registration saved = registrationRepository.findAll().getFirst();
        assertThat(saved.getMember()).isNull();
        assertThat(saved.getGuestName()).isEqualTo("Le Khach");
        assertThat(saved.getGuestEmail()).isEqualTo("Khach@Gmail.com");  // đã bỏ khoảng trắng 2 đầu
        assertThat(saved.getGuestPhone()).isEqualTo("0912345678");
        assertThat(saved.getGuestSchool()).isEqualTo("SE170001");
    }

    @Test
    void register_withoutCsrfOrToken_isAllowed() throws Exception {
        // Khách không có cookie access_token -> không bị bắt CSRF (SecurityConfig)
        long eventId = publishedEvent(100, "PUBLIC", false);

        mockMvc.perform(post("/api/public/events/" + eventId + "/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guest("Le Khach", "khach@gmail.com", "0912345678")))
                .andExpect(status().isCreated());
    }

    // ---------- Sự kiện khách không đăng ký được ----------

    @Test
    void register_listedInternalEvent_returns403() throws Exception {
        long eventId = publishedEvent(100, "INTERNAL", true);

        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(MEMBERS_ONLY));
    }

    @Test
    void register_unlistedInternalEvent_returns404() throws Exception {
        long eventId = publishedEvent(100, "INTERNAL", false);

        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_draftEvent_returns404() throws Exception {
        long eventId = createEvent(100, "PUBLIC", false);

        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_afterClose_returns409() throws Exception {
        long eventId = publishedEvent(100, "PUBLIC", false);
        setNow("2026-12-05T00:00");

        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_OPEN));
    }

    // ---------- Trùng, hết chỗ ----------

    @Test
    void register_sameEmailDifferentCase_returns409() throws Exception {
        long eventId = publishedEvent(100, "PUBLIC", false);
        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isCreated());

        registerGuest(eventId, guest("Le Khach", "KHACH@gmail.com", "0987654321"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void register_sameEmailOtherEvent_returns201() throws Exception {
        long first = publishedEvent(100, "PUBLIC", false);
        long second = publishedEvent(100, "PUBLIC", false);
        registerGuest(first, guest("Le Khach", "khach@gmail.com", "0912345678")).andExpect(status().isCreated());

        registerGuest(second, guest("Le Khach", "khach@gmail.com", "0912345678")).andExpect(status().isCreated());
    }

    @Test
    void membersAndGuests_shareCapacity() throws Exception {
        long eventId = publishedEvent(1, "PUBLIC", false);
        asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", anToken)
                .andExpect(status().isCreated());

        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "0912345678"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(FULL));
    }

    // ---------- Dữ liệu gửi lên ----------

    @Test
    void register_invalidInput_returns400() throws Exception {
        long eventId = publishedEvent(100, "PUBLIC", false);

        registerGuest(eventId, guest("Le Khach", "khong-phai-email", "0912345678"))
                .andExpect(status().isBadRequest());
        registerGuest(eventId, guest("Le Khach", "khach@gmail.com", "12345"))
                .andExpect(status().isBadRequest());
        registerGuest(eventId, guest("   ", "khach@gmail.com", "0912345678"))
                .andExpect(status().isBadRequest());
        registerGuest(eventId, """
                {"fullName": "Le Khach", "email": "khach@gmail.com", "phone": "0912345678"}
                """)  // thiếu school
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_signedIn_returns400() throws Exception {
        long eventId = publishedEvent(100, "PUBLIC", false);

        asUser(post("/api/public/events/" + eventId + "/registrations"),
                guest("Nguyen Van An", "an@feveid.test", "0912345678"), anToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Nhiều người bấm cùng lúc ----------

    @Test
    void concurrent_membersAndGuests_lastSeat_onlyOneGetsIt() throws Exception {
        long eventId = publishedEvent(1, "PUBLIC", false);
        createMember("racer@feveid.test", AccountRole.MEMBER, "Racer");

        List<Callable<Void>> attempts = new ArrayList<>();
        attempts.add(() -> {
            registrationService.register("racer@feveid.test", eventId);
            return null;
        });
        for (int i = 0; i < 5; i++) {
            GuestRegistrationRequest request =
                    new GuestRegistrationRequest("Khach " + i, "khach" + i + "@gmail.com", "0912345678", "FPT");
            attempts.add(() -> {
                registrationService.registerGuest(eventId, request);
                return null;
            });
        }

        assertThat(race(attempts)).isEqualTo(1);
        assertThat(registrationRepository.countByEventIdAndStatusIn(eventId, RegistrationStatus.ACTIVE))
                .isEqualTo(1);
    }

    // ---------- Helpers ----------

    // Chạy tất cả cùng lúc; trả về số lần thành công. ConflictException = bị từ chối; lỗi khác làm test đỏ
    private long race(List<Callable<Void>> attempts) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Void> attempt : attempts) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    try {
                        attempt.call();
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                }));
            }
            startGun.countDown();
            long ok = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    ok++;
                }
            }
            return ok;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String guest(String fullName, String email, String phone) {
        return """
                {"fullName": "%s", "email": "%s", "phone": "%s", "school": "SE170001"}
                """.formatted(fullName, email, phone);
    }

    private ResultActions registerGuest(long eventId, String json) throws Exception {
        return mockMvc.perform(post("/api/public/events/" + eventId + "/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    private long createEvent(Integer capacity, String visibility, boolean publiclyListed) throws Exception {
        String body = asUser(post("/api/events"), """
                {"name": "Đêm nhạc", "visibility": "%s", "publiclyListed": %s, "capacity": %s,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """.formatted(visibility, publiclyListed, capacity), hrToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long publishedEvent(Integer capacity, String visibility, boolean publiclyListed) throws Exception {
        setNow("2026-11-25T10:00");
        long id = createEvent(capacity, visibility, publiclyListed);
        asUser(put("/api/events/" + id + "/phases"), SCHEDULE, hrToken).andExpect(status().isOk());
        asUser(post("/api/events/" + id + "/publish"), null, hrToken).andExpect(status().isOk());
        setNow("2026-11-26T10:00");
        return id;
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
