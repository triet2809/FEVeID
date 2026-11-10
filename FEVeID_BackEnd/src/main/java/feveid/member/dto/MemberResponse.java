package feveid.member.dto;


import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;

import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String email,
        String fullName,
        String studentCode,
        String phone,
        String avatarUrl,
        MemberStatus memberStatus,
        LocalDate joinedAt
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
                member.getJoinedAt()
        );
    }
}
