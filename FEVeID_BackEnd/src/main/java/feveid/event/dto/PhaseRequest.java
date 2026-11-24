package feveid.event.dto;

import feveid.event.enums.PhaseKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record PhaseRequest(
        @Size(max = 100)
        @NotBlank
        String name,

        @NotNull
        Instant startsAt,

        @NotNull
        PhaseKind kind
) {
    public PhaseRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}
