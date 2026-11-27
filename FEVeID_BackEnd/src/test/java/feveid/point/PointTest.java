package feveid.point;

import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.common.exception.ConflictException;
import feveid.event.entity.Event;
import feveid.member.entity.Member;
import feveid.point.dto.PointAdjustmentRequest;
import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import feveid.point.repository.PointTransactionRepository;
import feveid.point.service.PointService;
import feveid.support.ApiTestSupport;
import feveid.term.entity.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static feveid.point.service.impl.PointServiceImpl.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sổ cái điểm (requirements.md 11, Bài 12.4). Hôm nay (giờ test) = 27/11/2026, thuộc kỳ Fall 2026
class PointTest extends ApiTestSupport {

    @MockitoBean
    private Clock clock;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    // Gọi thẳng service cho test nhiều luồng
    @Autowired
    private PointService pointService;

    private Member hr;
    private Member an;
    private String hrToken;
    private String anToken;
    private Term summer;
    private Term fall;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        hr = createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        an = createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        hrToken = tokenOf("hr@feveid.test");
        anToken = tokenOf("an@feveid.test");
        summer = createTerm("Summer 2026", "2026-05-01", "2026-08-31");
        fall = createTerm("Fall 2026", "2026-09-01", "2026-12-31");
        setNow("2026-11-27T10:00");
    }

    // ---------- Xem điểm ----------

    @Test
    void me_noTransactions_returnsZero() throws Exception {
        asUser(get("/api/points/me"), null, anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(an.getId()))
                .andExpect(jsonPath("$.currentTermName").value("Fall 2026"))
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.lifetimePoints").value(0))
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    void me_balanceCountsOnlyCurrentTerm_lifetimeCountsAllTerms() throws Exception {
        seed(an, summer, 5, PointTransactionType.ADJUSTMENT, null);
        seed(an, fall, 3, PointTransactionType.ADJUSTMENT, null);
        seed(an, fall, -1, PointTransactionType.ADJUSTMENT, null);

        asUser(get("/api/points/me"), null, anToken)
                .andExpect(jsonPath("$.currentBalance").value(2))  // 3 - 1, kỳ Summer đã reset
                .andExpect(jsonPath("$.lifetimePoints").value(7)); // 5 + 3 - 1
    }

    @Test
    void me_lifetimeIgnoresShopRedemptions() throws Exception {
        seed(an, fall, 10, PointTransactionType.ADJUSTMENT, null);
        seed(an, fall, -4, PointTransactionType.REDEEM, null);

        asUser(get("/api/points/me"), null, anToken)
                .andExpect(jsonPath("$.currentBalance").value(6))
                .andExpect(jsonPath("$.lifetimePoints").value(10)); // đổi quà không làm mất "điểm đã kiếm được"
    }

    @Test
    void me_historyNewestFirst_showsSource() throws Exception {
        Event event = eventRepository.save(Event.builder().name("Đêm nhạc").build());
        seed(an, fall, 1, PointTransactionType.ATTEND, event);
        seed(an, fall, 2, PointTransactionType.ADJUSTMENT, hr);

        asUser(get("/api/points/me"), null, anToken)
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.history[0].createdByName").value("Le Thi HR"))
                .andExpect(jsonPath("$.history[1].type").value("ATTEND"))
                .andExpect(jsonPath("$.history[1].eventName").value("Đêm nhạc"))
                .andExpect(jsonPath("$.history[1].termName").value("Fall 2026"));
    }

    @Test
    void me_betweenTerms_noCurrentTerm_balanceZero() throws Exception {
        seed(an, fall, 4, PointTransactionType.ADJUSTMENT, null);
        setNow("2027-01-05T10:00"); // chưa tạo kỳ Spring 2027: chỉ xem điểm thì không tự tạo kỳ

        asUser(get("/api/points/me"), null, anToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTermId").doesNotExist())
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.lifetimePoints").value(4));
    }

    @Test
    void memberSummary_hr_returns200() throws Exception {
        seed(an, fall, 3, PointTransactionType.ADJUSTMENT, null);

        asUser(get("/api/members/" + an.getId() + "/points"), null, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$.currentBalance").value(3));
    }

    @Test
    void memberSummary_member_returns403() throws Exception {
        asUser(get("/api/members/" + hr.getId() + "/points"), null, anToken).andExpect(status().isForbidden());
    }

    @Test
    void memberSummary_unknownMember_returns404() throws Exception {
        asUser(get("/api/members/999999/points"), null, hrToken).andExpect(status().isNotFound());
    }

    // ---------- Điều chỉnh tay ----------

    @Test
    void adjust_add_returns201_inCurrentTerm() throws Exception {
        adjust(an.getId(), "{\"amount\": 5, \"reason\": \"Bù điểm hỗ trợ bị sót\"}", hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.amount").value(5))
                .andExpect(jsonPath("$.termId").value(fall.getId()))
                .andExpect(jsonPath("$.reason").value("Bù điểm hỗ trợ bị sót"))
                .andExpect(jsonPath("$.createdByName").value("Le Thi HR"));

        assertThat(balance(an, fall)).isEqualTo(5);
    }

    @Test
    void adjust_subtractWithinBalance_returns201() throws Exception {
        seed(an, fall, 5, PointTransactionType.ADJUSTMENT, null);

        adjust(an.getId(), "{\"amount\": -3, \"reason\": \"Cộng nhầm\"}", hrToken).andExpect(status().isCreated());

        assertThat(balance(an, fall)).isEqualTo(2);
    }

    @Test
    void adjust_subtractToExactlyZero_returns201() throws Exception {
        seed(an, fall, 5, PointTransactionType.ADJUSTMENT, null);

        adjust(an.getId(), "{\"amount\": -5, \"reason\": \"Cộng nhầm\"}", hrToken).andExpect(status().isCreated());

        assertThat(balance(an, fall)).isZero();
    }

    @Test
    void adjust_subtractBelowZero_returns409_nothingSaved() throws Exception {
        seed(an, fall, 3, PointTransactionType.ADJUSTMENT, null);

        adjust(an.getId(), "{\"amount\": -4, \"reason\": \"Cộng nhầm\"}", hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NEGATIVE_BALANCE));

        assertThat(pointTransactionRepository.count()).isEqualTo(1);
    }

    @Test
    void adjust_balanceOfOtherTermDoesNotCount() throws Exception {
        seed(an, summer, 10, PointTransactionType.ADJUSTMENT, null); // điểm kỳ cũ không dùng cho kỳ này

        adjust(an.getId(), "{\"amount\": -1, \"reason\": \"Thử\"}", hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(NEGATIVE_BALANCE));
    }

    @Test
    void adjust_pastTermById_returns201_currentTermUnchanged() throws Exception {
        adjust(an.getId(), "{\"termId\": " + summer.getId() + ", \"amount\": 2, \"reason\": \"Khiếu nại kỳ hè\"}",
                hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.termName").value("Summer 2026"));

        assertThat(balance(an, summer)).isEqualTo(2);
        assertThat(balance(an, fall)).isZero();
    }

    @Test
    void adjust_unknownTerm_returns404() throws Exception {
        adjust(an.getId(), "{\"termId\": 999999, \"amount\": 2, \"reason\": \"Thử\"}", hrToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(TERM_NOT_FOUND));
    }

    @Test
    void adjust_termNotCreatedYet_withoutTermId_autoCreatesTerm() throws Exception {
        setNow("2027-01-05T10:00"); // chưa có dòng Spring 2027 trong DB

        adjust(an.getId(), "{\"amount\": 2, \"reason\": \"Thử\"}", hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.termName").value("Spring 2027"));
        assertThat(termRepository.count()).isEqualTo(3); // Summer, Fall 2026 + Spring 2027 vừa tạo
    }

    @Test
    void adjust_unknownMember_returns404() throws Exception {
        adjust(999_999L, "{\"amount\": 2, \"reason\": \"Thử\"}", hrToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(MEMBER_NOT_FOUND));
    }

    @Test
    void adjust_zeroAmount_returns400() throws Exception {
        adjust(an.getId(), "{\"amount\": 0, \"reason\": \"Thử\"}", hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Số điểm phải khác 0"));
    }

    @Test
    void adjust_blankReason_returns400() throws Exception {
        adjust(an.getId(), "{\"amount\": 2, \"reason\": \"   \"}", hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bắt buộc ghi lý do điều chỉnh"));
    }

    @Test
    void adjust_member_returns403() throws Exception {
        adjust(an.getId(), "{\"amount\": 100, \"reason\": \"Tự cộng\"}", anToken).andExpect(status().isForbidden());
    }

    // ---------- Sổ cái chỉ thêm ----------

    @Test
    void ledgerRows_cannotBeUpdatedOrDeleted_evenWithRawSql() {
        seed(an, fall, 5, PointTransactionType.ADJUSTMENT, null);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE point_transactions SET amount = 1000"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM point_transactions"))
                .isInstanceOf(DataAccessException.class);
        assertThat(balance(an, fall)).isEqualTo(5);
    }

    // ---------- Nhiều request cùng lúc ----------

    @Test
    void concurrent_eightDeductionsFromFivePoints_onlyFiveSucceed() throws Exception {
        seed(an, fall, 5, PointTransactionType.ADJUSTMENT, null);

        // 8 lần trừ 1 điểm cùng lúc. Không khóa: cả 8 cùng đọc số dư 5 -> cả 8 lọt -> số dư -3
        List<Boolean> results = raceToDeduct(8, an.getId());

        assertThat(results).filteredOn(ok -> ok).hasSize(5);
        assertThat(balance(an, fall)).isZero();
    }

    // ---------- Helpers ----------

    // true = trừ được, false = bị từ chối vì không đủ điểm (409). Lỗi khác làm test ĐỎ
    private List<Boolean> raceToDeduct(int times, Long memberId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(times);
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < times; i++) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    try {
                        pointService.adjust("hr@feveid.test", memberId,
                                new PointAdjustmentRequest(null, -1, "Trừ thử"));
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

    // Ghi thẳng vào sổ cái (Bài 13 mới có API phát điểm từ sự kiện). source: Event cho ATTEND, Member = người ghi
    private void seed(Member member, Term term, int amount, PointTransactionType type, Object source) {
        pointTransactionRepository.save(PointTransaction.builder()
                .member(member)
                .term(term)
                .amount(amount)
                .type(type)
                .event(source instanceof Event e ? e : null)
                .createdBy(source instanceof Member m ? m : null)
                .reason(type == PointTransactionType.ADJUSTMENT ? "Dữ liệu test" : null)
                .build());
    }

    private long balance(Member member, Term term) {
        return pointTransactionRepository.balanceOf(member.getId(), term.getId());
    }

    private Term createTerm(String name, String start, String end) {
        return termRepository.save(Term.builder()
                .name(name)
                .startDate(LocalDate.parse(start))
                .endDate(LocalDate.parse(end))
                .build());
    }

    private void setNow(String vnLocalDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(vnLocalDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    private ResultActions adjust(Long memberId, String json, String token) throws Exception {
        return asUser(post("/api/members/" + memberId + "/points/adjustments"), json, token);
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
