package feveid.event;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.common.exception.ConflictException;
import feveid.event.dto.FinalizeEventRequest;
import feveid.event.service.EventFinalizationService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import feveid.point.repository.PointTransactionRepository;
import feveid.registration.enums.RegistrationStatus;
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
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static feveid.event.service.impl.EventFinalizationServiceImpl.ALREADY_FINALIZED;
import static feveid.event.service.impl.EventFinalizationServiceImpl.NOT_COMPLETED;
import static feveid.event.service.impl.EventStaffServiceImpl.*;
import static feveid.registration.service.impl.RegistrationServiceImpl.FINALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Chốt sự kiện (requirements.md 11.1, Bài 13.2). Lịch giống CheckInTest:
// mở đăng ký 25/11 -> 04/12, ONGOING 05/12 08:00 -> 17:00 (giờ VN), chốt ngày 06/12
//
// Kịch bản chung (prepareEvent): team = An tổ chức, Bình hỗ trợ.
//   An  : đăng ký + check-in   -> chỉ ORGANIZE +4 (không cộng thêm ATTEND)
//   Bình: không đăng ký         -> SUPPORT +2
//   Chi : đăng ký + check-in   -> ATTEND +1
//   Dũng: đăng ký, không đến   -> NO_SHOW, không điểm
//   Em  : đăng ký rồi hủy      -> giữ CANCELLED
//   Khách 1: check-in -> có mặt nhưng không điểm; Khách 2: không đến -> NO_SHOW
class FinalizeEventTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    @Autowired
    private EventFinalizationService finalizationService;
    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    private static final String REGISTRATION_OPEN = "2026-11-26T10:00";
    private static final String ONGOING = "2026-12-05T09:00";
    private static final String AFTER = "2026-12-06T10:00";

    private static final String SCHEDULE = """
            {"phases": [
              {"name": "Coming soon", "startsAt": "2026-12-01T00:00:00+07:00", "kind": "UPCOMING"},
              {"name": "Check-in",    "startsAt": "2026-12-05T08:00:00+07:00", "kind": "ONGOING"},
              {"name": "Done",        "startsAt": "2026-12-05T17:00:00+07:00", "kind": "COMPLETED"}
            ]}
            """;

    private String hrToken;
    private String anToken;
    private Member hr;
    private Member an;
    private Member binh;
    private Member chi;
    private Member dung;
    private Member em;

    // Id đăng ký trong kịch bản chung
    private long dungRegistration;
    private long emRegistration;
    private long anRegistration;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        hr = createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        an = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        binh = createMember("binh@feveid.test", AccountRole.MEMBER, "Tran Binh");
        chi = createMember("chi@feveid.test", AccountRole.MEMBER, "Pham Chi");
        dung = createMember("dung@feveid.test", AccountRole.MEMBER, "Le Dung");
        em = createMember("em@feveid.test", AccountRole.MEMBER, "Vo Em");
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
    }

    // ---------- Xem trước ----------

    @Test
    void preview_completed_defaultsFromTeamAndCheckIns_writesNothing() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        getFinalization(eventId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalized").value(false))
                .andExpect(jsonPath("$.finalizable").value(true))
                .andExpect(jsonPath("$.termName").value("Fall 2026"))
                .andExpect(jsonPath("$.organizers[*].fullName", contains("Nguyen Van An")))
                .andExpect(jsonPath("$.supporters[*].fullName", contains("Tran Binh")))
                .andExpect(jsonPath("$.attendees[*].fullName", contains("Pham Chi")))
                .andExpect(jsonPath("$.guestAttendeeCount").value(1))
                .andExpect(jsonPath("$.noShowCount").value(2))
                .andExpect(jsonPath("$.totalPoints").value(7));

        // GET không được ghi gì: chưa tạo kỳ, chưa có điểm
        assertThat(termRepository.count()).isZero();
        assertThat(pointTransactionRepository.count()).isZero();
    }

    @Test
    void preview_ongoing_notFinalizable() throws Exception {
        long eventId = prepareEvent();
        setNow(ONGOING);

        getFinalization(eventId, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalizable").value(false))
                .andExpect(jsonPath("$.blockedReason").value(NOT_COMPLETED));
    }

    // ---------- Chốt ----------

    @Test
    void finalize_awardsOnePointRowPerPerson_marksNoShow_locksEvent() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        finalizeEvent(eventId, ids(an), ids(binh), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalized").value(true))
                .andExpect(jsonPath("$.finalizedByName").value("Le Thi HR"))
                .andExpect(jsonPath("$.termName").value("Fall 2026"))
                .andExpect(jsonPath("$.organizers[*].fullName", contains("Nguyen Van An")))
                .andExpect(jsonPath("$.supporters[*].fullName", contains("Tran Binh")))
                .andExpect(jsonPath("$.attendees[*].fullName", contains("Pham Chi")))
                .andExpect(jsonPath("$.noShowCount").value(2))
                .andExpect(jsonPath("$.totalPoints").value(7));

        // Sổ cái: đúng 3 dòng, mỗi người 1 vai trò. An có check-in nhưng chỉ nhận ORGANIZE
        Map<Long, PointTransaction> byMember = ledgerByMember(eventId);
        assertThat(byMember).hasSize(3);
        assertThat(byMember.get(an.getId()).getType()).isEqualTo(PointTransactionType.ORGANIZE);
        assertThat(byMember.get(an.getId()).getAmount()).isEqualTo(4);
        assertThat(byMember.get(binh.getId()).getAmount()).isEqualTo(2);
        assertThat(byMember.get(chi.getId()).getType()).isEqualTo(PointTransactionType.ATTEND);
        assertThat(byMember.get(chi.getId()).getAmount()).isEqualTo(1);
        assertThat(byMember.get(chi.getId()).getCreatedBy().getId()).isEqualTo(hr.getId()); // người bấm chốt

        // Kỳ Fall 2026 được tự tạo khi chốt
        assertThat(termRepository.findAll()).extracting("name").containsExactly("Fall 2026");

        // Đăng ký: không đến -> NO_SHOW (cả khách), đã hủy giữ nguyên, đã check-in giữ nguyên
        assertThat(statusOf(dungRegistration)).isEqualTo(RegistrationStatus.NO_SHOW);
        assertThat(statusOf(emRegistration)).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(statusOf(anRegistration)).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.NO_SHOW)).isEqualTo(2);

        // Trang quản lý sự kiện thấy đã chốt
        asUser(get("/api/events/" + eventId), null, hrToken)
                .andExpect(jsonPath("$.finalizedAt").value("2026-12-06T03:00:00Z"))
                .andExpect(jsonPath("$.finalizedByName").value("Le Thi HR"));

        // Xem lại sau khi chốt: đọc từ sổ cái
        getFinalization(eventId, hrToken)
                .andExpect(jsonPath("$.finalized").value(true))
                .andExpect(jsonPath("$.finalizable").value(false))
                .andExpect(jsonPath("$.blockedReason").value(ALREADY_FINALIZED))
                .andExpect(jsonPath("$.totalPoints").value(7));
    }

    @Test
    void finalize_hrChangesTeam_checkedInMemberMovedToSupporter() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        // HR bỏ An và Bình khỏi team, cho Chi (có check-in) làm hỗ trợ
        finalizeEvent(eventId, ids(), ids(chi), hrToken).andExpect(status().isOk());

        Map<Long, PointTransaction> byMember = ledgerByMember(eventId);
        assertThat(byMember).hasSize(2);
        assertThat(byMember.get(chi.getId()).getType()).isEqualTo(PointTransactionType.SUPPORT); // không thêm ATTEND
        assertThat(byMember.get(an.getId()).getType()).isEqualTo(PointTransactionType.ATTEND);  // vẫn có check-in
        assertThat(byMember).doesNotContainKey(binh.getId());
    }

    @Test
    void finalize_lateIntoNextTerm_pointsGoToTermOfEndDate() throws Exception {
        // Sự kiện kết thúc 30/04/2027 (ngày cuối Spring), 05/05 (đã sang Summer) HR mới chốt
        setNow("2027-04-01T10:00");
        long eventId = idOf(asUser(post("/api/events"), """
                {"name": "Hội trại", "visibility": "INTERNAL",
                 "registrationOpensAt": "2027-04-01T00:00:00+07:00",
                 "registrationClosesAt": "2027-04-29T23:59:00+07:00"}
                """, hrToken));
        asUser(put("/api/events/" + eventId + "/phases"), """
                {"phases": [
                  {"name": "Coming soon", "startsAt": "2027-04-01T00:00:00+07:00", "kind": "UPCOMING"},
                  {"name": "Trại",        "startsAt": "2027-04-30T08:00:00+07:00", "kind": "ONGOING"},
                  {"name": "Done",        "startsAt": "2027-04-30T17:00:00+07:00", "kind": "COMPLETED"}
                ]}
                """, hrToken).andExpect(status().isOk());
        asUser(post("/api/events/" + eventId + "/publish"), null, hrToken).andExpect(status().isOk());
        setNow("2027-04-10T10:00");
        long registrationId = idOf(asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", anToken));
        setNow("2027-04-30T09:00");
        manualCheckIn(eventId, registrationId).andExpect(status().isOk());
        setNow("2027-05-05T10:00");

        finalizeEvent(eventId, ids(), ids(), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termName").value("Spring 2027"));

        assertThat(ledgerByMember(eventId).get(an.getId()).getTerm().getName()).isEqualTo("Spring 2027");
    }

    @Test
    void manualCheckIn_afterCompletedBeforeFinalize_isAllowed_andEarnsAttend() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        // Dũng có đến nhưng quên quét mã -> HR check-in bù
        manualCheckIn(eventId, dungRegistration).andExpect(status().isOk());
        finalizeEvent(eventId, ids(an), ids(binh), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noShowCount").value(1)); // chỉ còn khách 2

        assertThat(ledgerByMember(eventId).get(dung.getId()).getType()).isEqualTo(PointTransactionType.ATTEND);
    }

    // ---------- Bị chặn ----------

    @Test
    void finalize_beforeCompleted_returns409_nothingWritten() throws Exception {
        long eventId = prepareEvent();
        setNow(ONGOING);

        finalizeEvent(eventId, ids(an), ids(binh), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NOT_COMPLETED));
        assertThat(pointTransactionRepository.count()).isZero();
    }

    @Test
    void finalize_twice_returns409() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);
        finalizeEvent(eventId, ids(an), ids(binh), hrToken).andExpect(status().isOk());

        finalizeEvent(eventId, ids(an), ids(binh), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ALREADY_FINALIZED));
        assertThat(pointTransactionRepository.count()).isEqualTo(3);
    }

    @Test
    void finalize_sameMemberInBothLists_returns400_nothingWritten() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        finalizeEvent(eventId, ids(an), ids(an), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(DUPLICATE_MEMBER));
        assertNothingWritten(eventId);
    }

    @Test
    void finalize_unknownMember_returns400() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        finalizeEvent(eventId, "[999999]", ids(), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MEMBER_NOT_FOUND));
        assertNothingWritten(eventId);
    }

    @Test
    void finalize_disqualifiedMember_returns400() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);
        binh.setMemberStatus(MemberStatus.DISQUALIFIED);
        memberRepository.save(binh);

        finalizeEvent(eventId, ids(an), ids(binh), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MEMBER_DISQUALIFIED));
        assertNothingWritten(eventId);
    }

    @Test
    void afterFinalize_teamAndManualCheckInLocked() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);
        finalizeEvent(eventId, ids(an), ids(binh), hrToken).andExpect(status().isOk());

        asUser(put("/api/events/" + eventId + "/staff"), """
                {"staff": [{"memberId": %d, "role": "ORGANIZER"}]}
                """.formatted(chi.getId()), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(EVENT_FINALIZED));
        manualCheckIn(eventId, dungRegistration)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(FINALIZED));
    }

    @Test
    void member_returns403() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        getFinalization(eventId, anToken).andExpect(status().isForbidden());
        finalizeEvent(eventId, ids(an), ids(), anToken).andExpect(status().isForbidden());
    }

    @Test
    void unknownEvent_returns404() throws Exception {
        setNow(AFTER);

        getFinalization(999_999L, hrToken).andExpect(status().isNotFound());
        finalizeEvent(999_999L, ids(), ids(), hrToken).andExpect(status().isNotFound());
    }

    @Test
    void missingLists_returns400() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);

        asUser(post("/api/events/" + eventId + "/finalization"), "{}", hrToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Đồng thời ----------

    @Test
    void finalize_fiveHrsAtOnce_exactlyOneSucceeds_pointsAwardedOnce() throws Exception {
        long eventId = prepareEvent();
        setNow(AFTER);
        FinalizeEventRequest request = new FinalizeEventRequest(List.of(an.getId()), List.of(binh.getId()));

        // Bỏ khóa dòng sự kiện (findByIdForUpdate -> findById) thì cả 5 cùng thấy "chưa chốt": 1 người thắng,
        // 4 người còn lại đụng unique index V12 -> DataIntegrityViolationException (500) thay vì 409 -> test đỏ
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Boolean> results = new ArrayList<>();
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    try {
                        finalizationService.finalizeEvent("hr@feveid.test", eventId, request);
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                }));
            }
            startGun.countDown();
            for (Future<Boolean> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS)); // lỗi khác (500) -> ExecutionException -> đỏ
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(results).containsOnlyOnce(true);
        assertThat(pointTransactionRepository.count()).isEqualTo(3);
    }

    // ---------- Helpers ----------

    // Sự kiện đã công bố + team + các đăng ký theo kịch bản ở đầu file. Kết thúc ở thời điểm ONGOING
    private long prepareEvent() throws Exception {
        setNow("2026-11-25T10:00");
        long eventId = idOf(asUser(post("/api/events"), """
                {"name": "Đêm nhạc", "visibility": "PUBLIC", "capacity": 100,
                 "registrationOpensAt": "2026-11-25T00:00:00+07:00",
                 "registrationClosesAt": "2026-12-04T23:59:00+07:00"}
                """, hrToken));
        asUser(put("/api/events/" + eventId + "/phases"), SCHEDULE, hrToken).andExpect(status().isOk());
        asUser(post("/api/events/" + eventId + "/publish"), null, hrToken).andExpect(status().isOk());
        asUser(put("/api/events/" + eventId + "/staff"), """
                {"staff": [{"memberId": %d, "role": "ORGANIZER"}, {"memberId": %d, "role": "SUPPORTER"}]}
                """.formatted(an.getId(), binh.getId()), hrToken).andExpect(status().isOk());

        setNow(REGISTRATION_OPEN);
        anRegistration = register(eventId, "an@feveid.test");
        long chiRegistration = register(eventId, "chi@feveid.test");
        dungRegistration = register(eventId, "dung@feveid.test");
        emRegistration = register(eventId, "em@feveid.test");
        asUser(post("/api/registrations/" + emRegistration + "/cancel"), null, tokenOf("em@feveid.test"))
                .andExpect(status().isOk());
        long guestCame = registerGuest(eventId, "khach1@gmail.com");
        registerGuest(eventId, "khach2@gmail.com");

        setNow(ONGOING);
        manualCheckIn(eventId, anRegistration).andExpect(status().isOk());
        manualCheckIn(eventId, chiRegistration).andExpect(status().isOk());
        manualCheckIn(eventId, guestCame).andExpect(status().isOk());
        return eventId;
    }

    private long register(long eventId, String email) throws Exception {
        return idOf(asUser(post("/api/registrations"), "{\"eventId\": " + eventId + "}", tokenOf(email)));
    }

    private long registerGuest(long eventId, String email) throws Exception {
        return idOf(mockMvc.perform(post("/api/public/events/" + eventId + "/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName": "Khach", "email": "%s", "phone": "0912345678", "school": "SE190000"}
                        """.formatted(email))));
    }

    private ResultActions manualCheckIn(long eventId, long registrationId) throws Exception {
        return asUser(post("/api/events/" + eventId + "/registrations/" + registrationId + "/check-in"),
                null, hrToken);
    }

    private ResultActions getFinalization(long eventId, String token) throws Exception {
        return asUser(get("/api/events/" + eventId + "/finalization"), null, token);
    }

    private ResultActions finalizeEvent(long eventId, String organizerIds, String supporterIds, String token)
            throws Exception {
        return asUser(post("/api/events/" + eventId + "/finalization"), """
                {"organizerIds": %s, "supporterIds": %s}
                """.formatted(organizerIds, supporterIds), token);
    }

    // ids(an, binh) -> "[1, 2]"
    private static String ids(Member... members) {
        return "[" + java.util.Arrays.stream(members).map(m -> String.valueOf(m.getId()))
                .collect(Collectors.joining(", ")) + "]";
    }

    // Ngoài transaction nên chỉ đọc được field đã JOIN sẵn (member, term) hoặc id của quan hệ lazy
    private Map<Long, PointTransaction> ledgerByMember(long eventId) {
        return pointTransactionRepository.findByEventIdOrderByIdAsc(eventId).stream()
                .collect(Collectors.toMap(t -> t.getMember().getId(), t -> t));
    }

    private RegistrationStatus statusOf(long registrationId) {
        return registrationRepository.findById(registrationId).orElseThrow().getStatus();
    }

    private void assertNothingWritten(long eventId) {
        assertThat(pointTransactionRepository.count()).isZero();
        assertThat(eventRepository.findById(eventId).orElseThrow().isFinalized()).isFalse();
        assertThat(statusOf(dungRegistration)).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
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
