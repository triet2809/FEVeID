package feveid.department.repository;

import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembership, Long> {
    // Ban hiện tại của một thành viên (dòng đang mở)
    Optional<DepartmentMembership> findByMemberIdAndEndDateIsNull(Long memberId);

    boolean existsByDepartmentIdAndPositionAndEndDateIsNull(Long departmentId, DepartmentPosition position);

    //Lấy luôn member + account trong 1 query (chống N+1)
    @EntityGraph(attributePaths = {"member", "member.account"})
    List<DepartmentMembership> findByDepartmentIdAndEndDateIsNull(Long departmentId);
    // Ban hiện tại của người đang đăng nhập (authentication.name = email)
    Optional<DepartmentMembership> findByMemberAccountEmailAndEndDateIsNull(String email);
}
