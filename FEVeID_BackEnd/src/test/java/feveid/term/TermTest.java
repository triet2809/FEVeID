package feveid.term;

import feveid.account.enums.AccountRole;
import feveid.support.ApiTestSupport;
import feveid.term.entity.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

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

    // ---------- Tạo ----------

    @Test
    void create_asHr_returns201_shopClosed() throws Exception {
        create(term("Spring 2027", "2027-01-01", "2027-04-30"), hrToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring 2027"))
                .andExpect(jsonPath("$.startDate").value("2027-01-01"))
                .andExpect(jsonPath("$.shopOpen").value(false));

        assertThat(termRepository.count()).isEqualTo(2);
    }

    @Test
    void create_asPresident_returns201() throws Exception {
        create(term("Summer 2027", "2027-05-01", "2027-08-31"), presidentToken)
                .andExpect(status().isCreated());
    }

    @Test
    void create_asMember_returns403() throws Exception {
        create(term("Spring 2027", "2027-01-01", "2027-04-30"), memberToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void create_endBeforeStart_returns400() throws Exception {
        create(term("Spring 2027", "2027-04-30", "2027-01-01"), hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingDates_returns400() throws Exception {
        // isDateRangeValid() phải chịu được null, nếu không sẽ thành 500
        create("""
                {"name": "Spring 2027"}
                """, hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_overlapping_returns409() throws Exception {
        create(term("Winter", "2026-12-01", "2027-03-31"), hrToken)
                .andExpect(status().isConflict());
    }

    @Test
    void create_startOnPreviousEndDate_returns409() throws Exception {
        // '[]': ngày cuối của Fall vẫn thuộc Fall
        create(term("Spring 2027", "2026-12-31", "2027-04-30"), hrToken)
                .andExpect(status().isConflict());
    }

    @Test
    void create_duplicateNameIgnoreCase_returns409() throws Exception {
        create(term("fall 2026", "2027-01-01", "2027-04-30"), hrToken)
                .andExpect(status().isConflict());
    }
    @Test
    void create_blankName_returns400() throws Exception {
        create(term("   ", "2027-01-01", "2027-04-30"), hrToken)
                .andExpect(status().isBadRequest());
    }
    @Test
    void create_blankStartDate_returns400() throws Exception {
        create(term("Spring 2027", "   ", "2027-04-30"), hrToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidDateFormat_returns400() throws Exception {
        create(term("Spring 2027", "01/01/2027", "2027-04-30"), hrToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Sửa ----------

    @Test
    void update_renameKeepDates_returns200() throws Exception {
        update(fall.getId(), term("Fall 2026 (K20)", "2026-09-01", "2026-12-31"), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fall 2026 (K20)"));
    }

    @Test
    void update_overlapOtherTerm_returns409() throws Exception {
        Term spring = termRepository.save(Term.builder()
                .name("Spring 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 4, 30))
                .build());

        update(spring.getId(), term("Spring 2027", "2026-12-15", "2027-04-30"), hrToken)
                .andExpect(status().isConflict());
    }

    @Test
    void update_keepsShopOpen() throws Exception {
        changeShop(fall.getId(), """
                {"open": true}
                """, hrToken).andExpect(status().isOk());

        update(fall.getId(), term("Fall 2026 (K20)", "2026-09-01", "2026-12-31"), hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopOpen").value(true));
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

    private String term(String name, String start, String end) {
        return """
                {"name": "%s", "startDate": "%s", "endDate": "%s"}
                """.formatted(name, start, end);
    }

    private ResultActions create(String json, String token) throws Exception {
        return mockMvc.perform(post("/api/terms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions update(Long id, String json, String token) throws Exception {
        return mockMvc.perform(put("/api/terms/{id}", id)
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