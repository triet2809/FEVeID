package feveid.registration.dto;

import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record RegistrationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long eventId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        Instant cancelledAt
) {
    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getEvent().getName(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getCancelledAt()
        );
    }
}
