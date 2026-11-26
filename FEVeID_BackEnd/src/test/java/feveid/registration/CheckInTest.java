package feveid.registration;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.common.exception.ConflictException;
import feveid.member.entity.Member;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static feveid.registration.service.impl.RegistrationServiceImpl.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Check-in (requirements.md 12.3, Bài 12.2b). Cùng lịch với RegistrationTest:
// mở đăng ký 25/11 -> 04/12, ONGOING 05/12 08:00 -> 17:00 (giờ VN)
class CheckInTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    @Autowired
    private RegistrationService registrationService;

    private Member hr;
    private String hrToken;
    private String anToken;
    private String binhToken;

    private static final String REGISTRATION_OPEN = "2026-11-26T10:00";
    private static final String ONGOING = "2026-12-05T09:00";

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
        hr = createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Binh");
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
        binhToken = tokenOf("binh@feveid.test");
    }

    // ---------- Mã QR cho màn hình chiếu ----------

    @Test
    void getCode_ongoing_returnsCodeExpiryAndCounts() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        register(eventId, binhToken);
        setNow(ONGOING);
        checkIn(eventId, codeOf(eventId), anToken).andExpect(status().isOk());

        getCode(eventId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.expiresAt").value("2026-12-05T02:00:30Z")) // 09:00:30 giờ VN
                .andExpect(jsonPath("$.checkedInCount").value(1))
                .andExpect(jsonPath("$.expectedCount").value(2));
    }

    @Test
    void getCode_beforeOngoing_returns409() throws Exception {
        long eventId = publishedEvent(100);
        setNow("2026-12-05T07:59");

        getCode(eventId, hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_ONGOING));
    }

    @Test
    void getCode_member_returns403() throws Exception {
        long eventId = publishedEvent(100);
        setNow(ONGOING);

        getCode(eventId, anToken).andExpect(status().isForbidden());
    }

    @Test
    void getCode_unknownEvent_returns404() throws Exception {
        setNow(ONGOING);

        getCode(999_999L, hrToken).andExpect(status().isNotFound());
    }

    // ---------- Member tự quét QR ----------

    @Test
    void checkIn_registered_validCode_returns200_checkedIn() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow(ONGOING);

        checkIn(eventId, codeOf(eventId), anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.checkedInAt").value("2026-12-05T02:00:00Z"));

        Registration saved = registrationRepository.findById(registrationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(saved.getCheckedInBy()).isNull(); // tự quét, không phải HR bấm
    }

    @Test
    void checkIn_twice_returns200_keepsFirstTime() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        setNow(ONGOING);
        checkIn(eventId, codeOf(eventId), anToken).andExpect(status().isOk());
        setNow("2026-12-05T09:10");

        checkIn(eventId, codeOf(eventId), anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedInAt").value("2026-12-05T02:00:00Z"));
        assertThat(registrationRepository.count()).isEqualTo(1);
    }

    @Test
    void checkIn_codeFromPreviousWindow_returns200() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        setNow(ONGOING);
        String code = codeOf(eventId);
        setNow("2026-12-05T09:00:45"); // quét lúc mã sắp đổi, request tới server khi đã sang ô mới

        checkIn(eventId, code, anToken).andExpect(status().isOk());
    }

    @Test
    void checkIn_oldCode_returns400() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        setNow(ONGOING);
        String code = codeOf(eventId);
        setNow("2026-12-05T09:01"); // ảnh chụp mã gửi cho bạn ở nhà, 1 phút sau mới dùng

        checkIn(eventId, code, anToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_CODE));
    }

    @Test
    void checkIn_codeOfAnotherEvent_returns400() throws Exception {
        long eventId = publishedEvent(100);
        long otherEventId = publishedEvent(100);
        register(eventId, anToken);
        setNow(ONGOING);

        checkIn(eventId, codeOf(otherEventId), anToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_CODE));
    }

    @Test
    void checkIn_madeUpCode_returns400() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        setNow(ONGOING);

        checkIn(eventId, "AAAAAAAAAAAAAAAA", anToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_CODE));
    }

    @Test
    void checkIn_missingCode_returns400() throws Exception {
        long eventId = publishedEvent(100);
        setNow(ONGOING);

        asUser(post("/api/check-ins"), "{\"eventId\": " + eventId + "}", anToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkIn_withoutLogin_returns401() throws Exception {
        long eventId = publishedEvent(100);
        setNow(ONGOING);
        String code = codeOf(eventId);

        mockMvc.perform(post("/api/check-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInJson(eventId, code)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkIn_afterCompleted_returns409() throws Exception {
        long eventId = publishedEvent(100);
        register(eventId, anToken);
        setNow("2026-12-05T16:59:50");
        String code = codeOf(eventId);
        setNow("2026-12-05T17:00:05"); // mã vẫn trong 60 giây nhưng sự kiện đã COMPLETED

        checkIn(eventId, code, anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_ONGOING));
    }

    @Test
    void checkIn_draftEvent_returns404() throws Exception {
        long eventId = createEvent(100);
        setNow(ONGOING);

        checkIn(eventId, "AAAAAAAAAAAAAAAA", anToken).andExpect(status().isNotFound());
    }

    // ---------- Chưa đăng ký mà có mặt (chốt 25/11/2026) ----------

    @Test
    void checkIn_notRegistered_seatLeft_createsCheckedInRegistration() throws Exception {
        long eventId = publishedEvent(100);
        setNow(ONGOING); // đã quá hạn đăng ký nhưng vẫn check-in được nếu còn chỗ

        checkIn(eventId, codeOf(eventId), anToken).andExpect(status().isOk());

        List<Registration> all = registrationRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(all.get(0).getCheckedInAt()).isEqualTo(vn(ONGOING));
    }

    @Test
    void checkIn_notRegistered_full_returns409() throws Exception {
        long eventId = publishedEvent(1);
        register(eventId, binhToken);
        setNow(ONGOING);

        checkIn(eventId, codeOf(eventId), anToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(FULL));
    }

    @Test
    void checkIn_afterCancelling_createsNewRegistration() throws Exception {
        long eventId = publishedEvent(100);
        long cancelledId = idOf(register(eventId, anToken));
        asUser(post("/api/registrations/" + cancelledId + "/cancel"), null, anToken).andExpect(status().isOk());
        setNow(ONGOING);

        checkIn(eventId, codeOf(eventId), anToken).andExpect(status().isOk());

        // Đăng ký đã hủy giữ nguyên làm lịch sử, check-in tạo dòng mới
        assertThat(registrationRepository.count()).isEqualTo(2);
        assertThat(registrationRepository.findById(cancelledId).orElseThrow().getStatus())
                .isEqualTo(RegistrationStatus.CANCELLED);
    }

    // ---------- Check-in tay (HR/BCN) ----------

    @Test
    void manual_confirmedMember_returns200_recordsStaff() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow(ONGOING);

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInAt").value("2026-12-05T02:00:00Z"));

        Registration saved = registrationRepository.findById(registrationId).orElseThrow();
        assertThat(saved.getCheckedInBy().getId()).isEqualTo(hr.getId());
    }

    @Test
    void manual_guest_returns200() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(mockMvc.perform(post("/api/public/events/" + eventId + "/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName": "Le Khach", "email": "khach@gmail.com",
                         "phone": "0912345678", "school": "SE190000"}
                        """)));
        setNow(ONGOING);

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(false))
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    void manual_alreadyCheckedIn_returns200_keepsSelfCheckIn() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow(ONGOING);
        checkIn(eventId, codeOf(eventId), anToken).andExpect(status().isOk());
        setNow("2026-12-05T09:30");

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedInAt").value("2026-12-05T02:00:00Z"));
        assertThat(registrationRepository.findById(registrationId).orElseThrow().getCheckedInBy()).isNull();
    }

    @Test
    void manual_cancelledRegistration_returns409() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        asUser(post("/api/registrations/" + registrationId + "/cancel"), null, anToken).andExpect(status().isOk());
        setNow(ONGOING);

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_CHECKABLE));
    }

    @Test
    void manual_registrationOfAnotherEvent_returns404() throws Exception {
        long eventId = publishedEvent(100);
        long otherEventId = publishedEvent(100);
        long registrationId = idOf(register(otherEventId, anToken));
        setNow(ONGOING);

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(REGISTRATION_NOT_FOUND));
    }

    @Test
    void manual_beforeOngoing_returns409() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow("2026-12-05T07:00");

        manual(eventId, registrationId, hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_ONGOING));
    }

    @Test
    void manual_member_returns403() throws Exception {
        long eventId = publishedEvent(100);
        long registrationId = idOf(register(eventId, anToken));
        setNow(ONGOING);

        manual(eventId, registrationId, binhToken).andExpect(status().isForbidden());
    }

    // ---------- Nhiều request cùng lúc ----------

    @Test
    void concurrent_sameMemberScansFiveTimes_oneRegistration_allSucceed() throws Exception {
        long eventId = publishedEvent(100);
        setNow(ONGOING);
        String code = codeOf(eventId);

        // Chưa đăng ký, bấm 5 lần liền (mạng chậm). Không khóa -> 5 luồng cùng thấy "chưa có" -> INSERT trùng -> 500
        List<Boolean> results = raceToCheckIn(List.of("an@feveid.test", "an@feveid.test", "an@feveid.test",
                "an@feveid.test", "an@feveid.test"), eventId, code);

        assertThat(results).containsOnly(true);
        assertThat(registrationRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrent_walkInsForLastSeat_onlyOneGetsIt() throws Exception {
        long eventId = publishedEvent(1);
        setNow(ONGOING);
        String code = codeOf(eventId);
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String email = "racer" + i + "@feveid.test";
            createMember(email, AccountRole.MEMBER, "Racer " + i);
            emails.add(email);
        }

        List<Boolean> results = raceToCheckIn(emails, eventId, code);

        assertThat(results).filteredOn(ok -> ok).hasSize(1);
        assertThat(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CHECKED_IN))
                .isEqualTo(1);
    }

    // ---------- Helpers ----------

    // true = check-in được, false = bị từ chối bằng ConflictException (409). Lỗi khác làm test ĐỎ
    private List<Boolean> raceToCheckIn(List<String> emails, long eventId, String code) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(emails.size());
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (String email : emails) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    try {
                        registrationService.checkIn(email, eventId, code);
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

    // Mã đúng lúc "bây giờ" của test, lấy qua API như màn hình chiếu thật
    private String codeOf(long eventId) throws Exception {
        String body = getCode(eventId, hrToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    private static Instant vn(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant();
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(vn(vnLocalDateTime));
    }

    private ResultActions getCode(long eventId, String token) throws Exception {
        return asUser(get("/api/events/" + eventId + "/check-in-code"), null, token);
    }

    private ResultActions checkIn(long eventId, String code, String token) throws Exception {
        return asUser(post("/api/check-ins"), checkInJson(eventId, code), token);
    }

    private static String checkInJson(long eventId, String code) {
        return "{\"eventId\": " + eventId + ", \"code\": \"" + code + "\"}";
    }

    private ResultActions manual(long eventId, long registrationId, String token) throws Exception {
        return asUser(post("/api/events/" + eventId + "/registrations/" + registrationId + "/check-in"),
                null, token);
    }

    // Đăng ký trong thời gian mở đăng ký (26/11). Gọi trước setNow(ONGOING)
    private ResultActions register(long eventId, String token) throws Exception {
        setNow(REGISTRATION_OPEN);
        return asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", token)
                .andExpect(status().isCreated());
    }

    private long createEvent(Integer capacity) throws Exception {
        setNow("2026-11-25T10:00");
        return idOf(asUser(post("/api/events"), """
                {"name": "Đêm nhạc", "visibility": "PUBLIC", "capacity": %s,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """.formatted(capacity), hrToken));
    }

    private long publishedEvent(Integer capacity) throws Exception {
        long id = createEvent(capacity);
        asUser(put("/api/events/" + id + "/phases"), SCHEDULE, hrToken).andExpect(status().isOk());
        asUser(post("/api/events/" + id + "/publish"), null, hrToken).andExpect(status().isOk());
        setNow(REGISTRATION_OPEN);
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
