package feveid.term;

import feveid.account.enums.AccountRole;
import feveid.common.config.TimeConfig;
import feveid.support.ApiTestSupport;
import feveid.term.entity.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentTermTest extends ApiTestSupport {

    // Thay bean Clock thật bằng mock: mỗi test tự đặt "bây giờ"
    @MockitoBean
    private Clock clock;

    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        memberToken = tokenOf("an@feveid.test");

        termRepository.save(Term.builder()
                .name("Summer 2026")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .shopOpen(true)     // tổng kết Summer: gian hàng đang mở
                .build());
        termRepository.save(Term.builder()
                .name("Fall 2026")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build());
    }

    // LocalDate.now(clock) chỉ gọi clock.getZone() và clock.instant() → mock 2 method này là đủ
    private void setNow(String localDateTime) {
        when(clock.getZone()).thenReturn(TimeConfig.CLUB_ZONE);
        when(clock.instant()).thenReturn(
                LocalDateTime.parse(localDateTime).atZone(TimeConfig.CLUB_ZONE).toInstant());
    }

    // ---------- Kỳ hiện tại ----------

    @Test
    void current_midTerm_returnsFall() throws Exception {
        setNow("2026-11-19T10:00");

        getAsMember("/api/terms/current")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fall 2026"));
    }

    @Test
    void current_lastMinuteOfTerm_stillFall() throws Exception {
        setNow("2026-12-31T23:30");

        getAsMember("/api/terms/current")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fall 2026"));
    }

    @Test
    void current_justAfterMidnightVietnam_returns404() throws Exception {
        // 00:30 ngày 01/01 giờ VN = 17:30 ngày 31/12 giờ UTC → nếu dùng nhầm UTC sẽ trả Fall
        setNow("2027-01-01T00:30");

        getAsMember("/api/terms/current").andExpect(status().isNotFound());
    }

    @Test
    void current_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/terms/current")).andExpect(status().isUnauthorized());
    }

    // ---------- Lọc gian hàng ----------

    @Test
    void list_shopOpenTrue_onlyOpenTerms() throws Exception {
        getAsMember("/api/terms?shopOpen=true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Summer 2026"));
    }

    @Test
    void list_shopOpenFalse_onlyClosedTerms() throws Exception {
        getAsMember("/api/terms?shopOpen=false")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fall 2026"));
    }

    @Test
    void list_noFilter_returnsAll() throws Exception {
        getAsMember("/api/terms")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Fall 2026"));
    }

    @Test
    void list_invalidShopOpen_returns400() throws Exception {
        getAsMember("/api/terms?shopOpen=abc").andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private ResultActions getAsMember(String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + memberToken));
    }
}