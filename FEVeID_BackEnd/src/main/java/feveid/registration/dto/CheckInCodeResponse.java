package feveid.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Màn hình chiếu QR của HR/BCN gọi lại định kỳ: lấy mã mới + số người đã check-in
public record CheckInCodeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        // Sau thời điểm này mã đổi -> frontend gọi lại để lấy mã mới
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long checkedInCount,
        // Số đăng ký còn hiệu lực (RegistrationStatus.ACTIVE), gồm cả người đã check-in
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expectedCount
) {
}
