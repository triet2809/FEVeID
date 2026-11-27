package feveid.term;

import feveid.account.enums.AccountRole;
import feveid.support.ApiTestSupport;
import feveid.term.entity.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static feveid.term.service.impl.TermServiceImpl.TERM_EXISTS;
import static feveid.term.service.impl.TermServiceImpl.TERM_OVERLAP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermTest extends ApiTestSupport {

    private String hrToken;
    private String presidentToken;
    private String memberToken;
    private Term fall;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("president@feveid.test", AccountRole.PRESIDENT, "Chu Nhiem");
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        hrToken = tokenOf("hr@feveid.test");
        presidentToken = tokenOf("president@feveid.test");
        memberToken = tokenOf("an@feveid.test");
        fall = termRepository.save(Term.builder()
                .name("Fall 2026")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build());
    }

    // ---------- Tạo (chọn mùa + năm, ngày theo lịch cố định) ----------

    @Test
    void create_asHr_returns201_datesFromCalendar_shopClosed() throws Exception {
        create(term("SPRING", 2027), hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring 2027"))
                .andExpect(jsonPath("$.startDate").value("2027-01-01"))
                .andExpect(jsonPath("$.endDate").value("2027-04-30"))
                .andExpect(jsonPath("$.shopOpen").value(false));

        assertThat(termRepository.count()).isEqualTo(2);
    }

    @Test
    void create_summer_endsOn31August() throws Exception {
        create(term("SUMMER", 2027), presidentToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Summer 2027"))
                .andExpect(jsonPath("$.startDate").value("2027-05-01"))
                .andExpect(jsonPath("$.endDate").value("2027-08-31"));
    }

    @Test
    void create_asMember_returns403() throws Exception {
        create(term("SPRING", 2027), memberToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void create_existing_returns409() throws Exception {
        create(term("FALL", 2026), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(TERM_EXISTS));
    }

    @Test
    void create_overlapsLegacyTermWithOddDates_returns409() throws Exception {
        // Kỳ cũ tạo tay trước khi chốt lịch cố định, lấn sang tháng 1/2027
        termRepository.save(Term.builder()
                .name("Winter 2026")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 1, 15))
                .build());

        create(term("SPRING", 2027), hrToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(TERM_OVERLAP));
    }

    @Test
    void create_missingSeasonOrYear_returns400() throws Exception {
        create("""
                {"year": 2027}
                """, hrToken).andExpect(status().isBadRequest());
        create("""
                {"season": "SPRING"}
                """, hrToken).andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownSeason_returns400() throws Exception {
        create(term("WINTER", 2027), hrToken).andExpect(status().isBadRequest());
    }

    @Test
    void create_yearOutOfRange_returns400() throws Exception {
        create(term("SPRING", 2019), hrToken).andExpect(status().isBadRequest());
        create(term("SPRING", 2101), hrToken).andExpect(status().isBadRequest());
    }

    @Test
    void update_endpointRemoved_returns405() throws Exception {
        // Ngày đã cố định nên không còn API sửa kỳ
        mockMvc.perform(put("/api/terms/{id}", fall.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(term("FALL", 2026)))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- Mở/đóng gian hàng ----------

    @Test
    void openShop_twice_returns200() throws Exception {
        changeShop(fall.getId(), """
                {"open": true}
                """, hrToken).andExpect(status().isOk());
        changeShop(fall.getId(), """
                {"open": true}
                """, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopOpen").value(true));

        assertThat(termRepository.findById(fall.getId()).orElseThrow().isShopOpen()).isTrue();
    }

    @Test
    void openShop_asMember_returns403() throws Exception {
        changeShop(fall.getId(), """
                {"open": true}
                """, memberToken).andExpect(status().isForbidden());
    }

    @Test
    void changeShop_missingOpen_returns400() throws Exception {
        changeShop(fall.getId(), "{}", hrToken).andExpect(status().isBadRequest());
    }

    // ---------- Xem ----------

    @Test
    void list_asMember_newestFirst() throws Exception {
        termRepository.save(Term.builder()
                .name("Spring 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 4, 30))
                .build());

        mockMvc.perform(get("/api/terms").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Spring 2027"));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/terms")).andExpect(status().isUnauthorized());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/terms/{id}", 999999).header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private String term(String season, int year) {
        return """
                {"season": "%s", "year": %d}
                """.formatted(season, year);
    }

    private ResultActions create(String json, String token) throws Exception {
        return mockMvc.perform(post("/api/terms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions changeShop(Long id, String json, String token) throws Exception {
        return mockMvc.perform(patch("/api/terms/{id}/shop", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}