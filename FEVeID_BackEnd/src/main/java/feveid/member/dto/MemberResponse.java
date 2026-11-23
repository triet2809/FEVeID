package feveid.member.dto;


import feveid.account.enums.AccountRole;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

// REQUIRED = field luôn có giá trị → kiểu TS sinh ra (npm run gen:api) không có dấu "?".
// Field không đánh dấu (studentCode, phone, avatarUrl) có thể null.
public record MemberResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        String studentCode,
        String phone,
        String avatarUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MemberStatus memberStatus,
        // role toàn CLB, frontend dùng để hiện menu theo quyền
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AccountRole role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate joinedAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getAccount().getEmail(),
                member.getFullName(),
                member.getStudentCode(),
                member.getPhone(),
                member.getAvatarUrl(),
                member.getMemberStatus(),
                member.getAccount().getRole(),
                member.getJoinedAt()
        );
    }
}
