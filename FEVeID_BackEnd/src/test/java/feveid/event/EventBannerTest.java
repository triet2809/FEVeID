package feveid.event;

import com.jayway.jsonpath.JsonPath;
import feveid.account.enums.AccountRole;
import feveid.common.upload.ImageStorage;
import feveid.common.upload.ImageStorageException;
import feveid.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventBannerTest extends ApiTestSupport {

    // Bản giả thay cho CloudinaryImageStorage: không gọi mạng, CI không cần key.
    // Mỗi test tự dặn bản giả trả gì (when...thenReturn) và kiểm tra service đã gọi nó ra sao (verify)
    @MockitoBean
    private ImageStorage imageStorage;

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    private static final String URL_V1 = "https://res.cloudinary.com/demo/image/upload/v1/feveid/events/1/banner.png";
    private static final String URL_V2 = "https://res.cloudinary.com/demo/image/upload/v2/feveid/events/1/banner.png";

    private String hrToken;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();
        createMember("hr@feveid.test", AccountRole.HR, "Le Thi HR");
        createMember("an@feveid.test", AccountRole.MEMBER, "Nguyen Van An");
        hrToken = tokenOf("hr@feveid.test");
        memberToken = tokenOf("an@feveid.test");
    }

    // ---------- Upload ----------

    @Test
    void upload_validPng_savesUrlWithFixedPublicId() throws Exception {
        long id = createEvent();
        when(imageStorage.upload(any(), eq(publicIdOf(id)))).thenReturn(URL_V1);

        uploadBanner(id, PNG, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerUrl").value(URL_V1));

        // Đã lưu vào DB chứ không chỉ trả về trong response
        getEvent(id).andExpect(jsonPath("$.bannerUrl").value(URL_V1));
    }

    @Test
    void upload_again_replacesUrl() throws Exception {
        long id = createEvent();
        when(imageStorage.upload(any(), eq(publicIdOf(id)))).thenReturn(URL_V1, URL_V2);  // lần 1 trả V1, lần 2 trả V2

        uploadBanner(id, PNG, hrToken);
        uploadBanner(id, PNG, hrToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerUrl").value(URL_V2));
    }

    @Test
    void upload_notAnImage_returns400_andNeverCallsStorage() throws Exception {
        long id = createEvent();

        uploadBanner(id, "<svg onload=alert(1)>".getBytes(), hrToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ nhận ảnh JPG, PNG hoặc WebP"));

        // Kiểm tra trước, gửi đi sau: file xấu không bao giờ tới Cloudinary
        verify(imageStorage, never()).upload(any(), anyString());
    }

    @Test
    void upload_withoutFilePart_returns400() throws Exception {
        long id = createEvent();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/events/" + id + "/banner")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_asMember_returns403() throws Exception {
        long id = createEvent();

        uploadBanner(id, PNG, memberToken)
                .andExpect(status().isForbidden());
        verify(imageStorage, never()).upload(any(), anyString());
    }

    @Test
    void upload_unknownEvent_returns404() throws Exception {
        uploadBanner(999_999L, PNG, hrToken)
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_cancelledEvent_returns409() throws Exception {
        long id = createEvent();
        mockMvc.perform(post("/api/events/" + id + "/cancel").header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());

        uploadBanner(id, PNG, hrToken)
                .andExpect(status().isConflict());
        verify(imageStorage, never()).upload(any(), anyString());
    }

    @Test
    void upload_storageFails_returns502_andKeepsOldBanner() throws Exception {
        long id = createEvent();
        when(imageStorage.upload(any(), anyString()))
                .thenReturn(URL_V1)
                .thenThrow(new ImageStorageException("Không tải được ảnh lên, vui lòng thử lại sau"));
        uploadBanner(id, PNG, hrToken);

        uploadBanner(id, PNG, hrToken)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Không tải được ảnh lên, vui lòng thử lại sau"));

        getEvent(id).andExpect(jsonPath("$.bannerUrl").value(URL_V1));
    }

    // ---------- Xóa banner ----------

    @Test
    void deleteBanner_removesUrlAndFile() throws Exception {
        long id = createEventWithBanner();

        deleteBanner(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerUrl").doesNotExist());

        verify(imageStorage).delete(publicIdOf(id));
    }

    @Test
    void deleteBanner_withoutBanner_returns200_andNeverCallsStorage() throws Exception {
        long id = createEvent();

        deleteBanner(id).andExpect(status().isOk());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void deleteBanner_storageFails_returns502_andKeepsUrl() throws Exception {
        long id = createEventWithBanner();
        doThrow(new ImageStorageException("Không xóa được ảnh, vui lòng thử lại sau"))
                .when(imageStorage).delete(anyString());

        deleteBanner(id).andExpect(status().isBadGateway());

        getEvent(id).andExpect(jsonPath("$.bannerUrl").value(URL_V1));
    }

    // ---------- Xóa sự kiện nháp ----------

    @Test
    void deleteDraftEvent_withBanner_alsoDeletesFile() throws Exception {
        long id = createEventWithBanner();

        deleteEvent(id).andExpect(status().isNoContent());
        verify(imageStorage).delete(publicIdOf(id));
    }

    @Test
    void deleteDraftEvent_storageFails_stillDeletesEvent() throws Exception {
        long id = createEventWithBanner();
        doThrow(new ImageStorageException("Không xóa được ảnh, vui lòng thử lại sau"))
                .when(imageStorage).delete(anyString());

        deleteEvent(id).andExpect(status().isNoContent());
        getEvent(id).andExpect(status().isNotFound());
    }

    // ---------- helper ----------

    private static String publicIdOf(long eventId) {
        return "feveid/events/" + eventId + "/banner";
    }

    private long createEvent() throws Exception {
        String body = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Đêm nhạc", "visibility": "PUBLIC"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createEventWithBanner() throws Exception {
        long id = createEvent();
        when(imageStorage.upload(any(), eq(publicIdOf(id)))).thenReturn(URL_V1);
        uploadBanner(id, PNG, hrToken).andExpect(status().isOk());
        return id;
    }

    // Giống form HTML gửi file: multipart/form-data, part tên "file"
    private ResultActions uploadBanner(long id, byte[] data, String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", data);
        return mockMvc.perform(multipart(HttpMethod.PUT, "/api/events/" + id + "/banner")
                .file(file)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions deleteBanner(long id) throws Exception {
        return mockMvc.perform(delete("/api/events/" + id + "/banner").header("Authorization", "Bearer " + hrToken));
    }

    private ResultActions getEvent(long id) throws Exception {
        return mockMvc.perform(get("/api/events/" + id).header("Authorization", "Bearer " + hrToken));
    }

    private ResultActions deleteEvent(long id) throws Exception {
        return mockMvc.perform(delete("/api/events/" + id).header("Authorization", "Bearer " + hrToken));
    }
}
