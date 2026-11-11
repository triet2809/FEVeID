package feveid.member.dto;

import feveid.member.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberStatusRequest(
        @NotNull
        MemberStatus status
) {
}
