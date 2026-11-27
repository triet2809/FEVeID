package feveid.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Danh sách HR đã tick ở màn chốt (mặc định = team sự kiện, HR được thêm/bỏ). Người tham gia không gửi lên:
// lấy từ check-in trong DB, để không ai "tự thêm" người không đến
public record FinalizeEventRequest(
        @NotNull
        @Size(max = 200)
        List<@NotNull Long> organizerIds,

        @NotNull
        @Size(max = 200)
        List<@NotNull Long> supporterIds
) {
}
