package feveid.department;

import feveid.account.enums.AccountRole;
import feveid.department.entity.Department;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentTest extends ApiTestSupport {

    private String presidentToken;
    private String hrToken;
    private Department media;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("president@feveid.test", AccountRole.PRESIDENT, "Chu Nhiem");
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        presidentToken = tokenOf("president@feveid.test");
        hrToken = tokenOf("hr@feveid.test");
        media = departmentRepository.save(Department.builder()
                .name("Ban Truyền Thông")
                .description("Truyền thông cho CLB")
                .build());
    }

    // ---------- Xem (công khai) ----------

    @Test
    void list_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ban Truyền Thông"));
    }

    @Test
    void getById_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/departments/{id}", media.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ban Truyền Thông"))
                .andExpect(jsonPath("$.description").value("Truyền thông cho CLB"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/departments/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    // ---------- Tạo ----------

    @Test
    void create_asPresident_returns201() throws Exception {
        create("""
                {"name": "Ban Sự Kiện", "description": "Tổ chức sự kiện"}
                """, presidentToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ban Sự Kiện"));

        assertThat(departmentRepository.count()).isEqualTo(2);
    }

    @Test
    void create_trimsName() throws Exception {
        create("""
                {"name": "   Ban Sự Kiện   "}
                """, presidentToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ban Sự Kiện"));
    }

    @Test
    void create_asHr_returns403() throws Exception {
        create("""
                {"name": "Ban Sự Kiện"}
                """, hrToken)
                .andExpect(status().isForbidden());

        assertThat(departmentRepository.count()).isEqualTo(1);
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ban Sự Kiện"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_duplicateNameIgnoreCase_returns409() throws Exception {
        create("""
                {"name": "ban truyền thông"}
                """, presidentToken)
                .andExpect(status().isConflict());
    }

    @Test
    void create_blankName_returns400() throws Exception {
        create("""
                {"name": "   "}
                """, presidentToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- Sửa ----------

    @Test
    void update_asPresident_returns200() throws Exception {
        update(media.getId(), """
                {"name": "Ban Truyền Thông & Thiết Kế", "description": "Mô tả mới"}
                """, presidentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ban Truyền Thông & Thiết Kế"));

        Department saved = departmentRepository.findById(media.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Ban Truyền Thông & Thiết Kế");
        assertThat(saved.getDescription()).isEqualTo("Mô tả mới");
    }

    @Test
    void update_keepSameName_returns200() throws Exception {
        update(media.getId(), """
                {"name": "Ban Truyền Thông", "description": "Chỉ đổi mô tả"}
                """, presidentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Chỉ đổi mô tả"));
    }

    @Test
    void update_toExistingName_returns409() throws Exception {
        departmentRepository.save(Department.builder().name("Ban Sự Kiện").build());

        update(media.getId(), """
                {"name": "Ban Sự Kiện"}
                """, presidentToken)
                .andExpect(status().isConflict());

        assertThat(departmentRepository.findById(media.getId()).orElseThrow().getName())
                .isEqualTo("Ban Truyền Thông");
    }

    @Test
    void update_withoutDescription_setsNull() throws Exception {
        update(media.getId(), """
                {"name": "Ban Truyền Thông"}
                """, presidentToken)
                .andExpect(status().isOk());

        assertThat(departmentRepository.findById(media.getId()).orElseThrow().getDescription()).isNull();
    }

    @Test
    void update_notFound_returns404() throws Exception {
        update(999999L, """
                {"name": "Ban Ma"}
                """, presidentToken)
                .andExpect(status().isNotFound());
    }

    @Test
    void update_asHr_returns403() throws Exception {
        update(media.getId(), """
                {"name": "Ban Bị Đổi"}
                """, hrToken)
                .andExpect(status().isForbidden());
    }

    // ---------- Helper ----------

    private ResultActions create(String json, String token) throws Exception {
        return mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions update(Long id, String json, String token) throws Exception {
        return mockMvc.perform(put("/api/departments/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}