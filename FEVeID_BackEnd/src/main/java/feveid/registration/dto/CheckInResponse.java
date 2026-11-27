package feveid.registration.dto;

import feveid.registration.entity.Registration;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Kết quả member tự check-in. Quét lại lần 2 vẫn trả về đúng giờ check-in lần đầu
public record CheckInResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long registrationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long eventId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant checkedInAt
) {
    public static CheckInResponse from(Registration registration) {
        return new CheckInResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getEvent().getName(),
                registration.getCheckedInAt()
        );
    }
}
