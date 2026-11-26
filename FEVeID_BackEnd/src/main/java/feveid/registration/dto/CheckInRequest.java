package feveid.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Member quét QR: link trong QR có eventId + code, trang /app/check-in gửi lại nguyên 2 giá trị này
public record CheckInRequest(
        @NotNull
        Long eventId,
        @NotBlank(message = "Thiếu mã check-in")
        String code
) {
}
