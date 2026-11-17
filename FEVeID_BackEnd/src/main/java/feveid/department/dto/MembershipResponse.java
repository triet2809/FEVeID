package feveid.department.dto;

import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;

import java.time.LocalDate;

public record MembershipResponse(
        Long memberId,
        Long departmentId,
        String departmentName,
        DepartmentPosition position,
        LocalDate startDate
) {
    public static MembershipResponse from(DepartmentMembership m) {
        return new MembershipResponse(
                m.getMember().getId(),
                m.getDepartment().getId(),
                m.getDepartment().getName(),
                m.getPosition(),
                m.getStartDate()
        );
    }

}
