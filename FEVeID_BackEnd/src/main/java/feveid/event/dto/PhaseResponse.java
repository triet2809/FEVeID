package feveid.event.dto;

import feveid.event.entity.EventPhase;
import feveid.event.enums.PhaseKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PhaseResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PhaseKind kind
) {
    public static PhaseResponse from(EventPhase phase) {
        return new PhaseResponse(phase.getId(), phase.getName(), phase.getStartsAt(), phase.getKind());
    }
}
