package feveid.event.dto;

import feveid.event.entity.EventStaff;
import feveid.event.enums.StaffRole;
import feveid.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

// Một người trong team sự kiện
public record EventStaffResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long memberId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        String studentCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StaffRole role
) {
    public static EventStaffResponse from(EventStaff staff) {
        Member member = staff.getMember();
        return new EventStaffResponse(member.getId(), member.getFullName(), member.getStudentCode(), staff.getRole());
    }
}
