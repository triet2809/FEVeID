package feveid.member.repository;

import feveid.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAccountId(Long accountId);
    boolean existsByStudentCode(String studentCode);
    Optional<Member> findByAccountEmail(String email);
}
