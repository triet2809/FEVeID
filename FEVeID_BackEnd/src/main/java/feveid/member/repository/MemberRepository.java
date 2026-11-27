package feveid.member.repository;

import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAccountId(Long accountId);
    boolean existsByStudentCode(String studentCode);
    Optional<Member> findByAccountEmail(String email);

    // SELECT ... FOR UPDATE trên dòng thành viên. Sổ cái điểm không có dòng "ví" để khóa, nên mọi thao tác
    // có thể TRỪ điểm (điều chỉnh, đổi quà) khóa dòng member trước rồi mới tính số dư (Bài 12.4)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "account")
    List<Member> findAllByOrderByFullNameAsc();
    @EntityGraph(attributePaths = "account")
    List<Member> findByMemberStatusOrderByFullName(MemberStatus status);
}
