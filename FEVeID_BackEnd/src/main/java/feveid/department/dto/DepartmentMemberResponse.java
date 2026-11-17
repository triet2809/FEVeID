package feveid.department.dto;

import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;

import java.time.LocalDate;

public record DepartmentMemberResponse(
        Long memberId,
        String fullName,
        String email,
        DepartmentPosition position,
        LocalDate startDate
) {
    public static DepartmentMemberResponse from(DepartmentMembership m) {
        return new DepartmentMemberResponse(
                m.getMember().getId(),
                m.getMember().getFullName(),
                m.getMember().getAccount().getEmail(),
                m.getPosition(),
                m.getStartDate()
        );
    }
}