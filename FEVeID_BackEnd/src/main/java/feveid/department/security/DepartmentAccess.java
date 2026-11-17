package feveid.department.security;


import feveid.department.repository.DepartmentMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//Dùng trong @PreAuthorize: Trưởng ban/ phó ban chỉ xem được ban mình
@Component("departmentAccess")
@RequiredArgsConstructor
public class DepartmentAccess {
    private final DepartmentMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public boolean isManagerOf(Long departmentId, String email) {
        return membershipRepository.findByMemberAccountEmailAndEndDateIsNull(email)
                .filter(m -> m.getPosition().isManager())
                .map(m -> m.getDepartment().getId().equals(departmentId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isManagerOfMember(Long memberId, String email) {
        return membershipRepository.findByMemberIdAndEndDateIsNull(memberId)
                .map(m -> isManagerOf(m.getDepartment().getId(), email))
                .orElse(false);
    }
}
