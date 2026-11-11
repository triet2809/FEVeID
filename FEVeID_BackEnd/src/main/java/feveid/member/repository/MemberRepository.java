package feveid.member.repository;

import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAccountId(Long accountId);
    boolean existsByStudentCode(String studentCode);
    Optional<Member> findByAccountEmail(String email);

    @EntityGraph(attributePaths = "account")
    List<Member> findAllByOrderByFullNameAsc();
    @EntityGraph(attributePaths = "account")
    List<Member> findByMemberStatusOrderByFullName(MemberStatus status);
}
