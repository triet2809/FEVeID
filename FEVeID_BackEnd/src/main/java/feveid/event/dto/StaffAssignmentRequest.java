package feveid.event.dto;

import feveid.event.enums.StaffRole;
import jakarta.validation.constraints.NotNull;

// Một người trong team gửi lên khi thay cả team
public record StaffAssignmentRequest(
        @NotNull
        Long memberId,

        @NotNull
        StaffRole role
) {
}
