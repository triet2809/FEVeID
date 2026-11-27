package feveid.point.repository;

import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

// Chỉ có hàm thêm (save) và đọc. Đừng viết hàm update/delete ở đây: DB sẽ báo lỗi (trigger V12)
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // Số dư của một kỳ = tổng mọi giao dịch của kỳ đó. COALESCE: chưa có giao dịch nào thì SUM trả NULL -> 0
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM PointTransaction t
            WHERE t.member.id = :memberId AND t.term.id = :termId
            """)
    long balanceOf(@Param("memberId") Long memberId, @Param("termId") Long termId);

    // Tổng điểm trọn đời: mọi kỳ, bỏ giao dịch shop (đổi quà không làm giảm "điểm đã kiếm được")
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM PointTransaction t
            WHERE t.member.id = :memberId AND t.type NOT IN :excluded
            """)
    long lifetimeOf(@Param("memberId") Long memberId,
                    @Param("excluded") Collection<PointTransactionType> excluded);

    // Lịch sử, mới nhất trước. Lấy kèm kỳ, sự kiện, người ghi trong 1 câu SELECT (tránh N+1)
    @EntityGraph(attributePaths = {"term", "event", "createdBy"})
    List<PointTransaction> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    // Điểm đã phát khi chốt một sự kiện (ORGANIZE/SUPPORT/ATTEND), kèm member để hiện tên
    @EntityGraph(attributePaths = {"member", "term"})
    List<PointTransaction> findByEventIdOrderByIdAsc(Long eventId);
}
