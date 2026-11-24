package feveid.event.repository;

import feveid.event.entity.Event;
import feveid.event.enums.EventPublication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    // EntityGraph: lấy luôn lịch và người phụ trách trong 1 câu SELECT (JOIN),
    // tránh N+1 (mỗi sự kiện lại SELECT thêm phases/owner)
    @EntityGraph(attributePaths = {"phases", "owner"})
    List<Event> findAllByOrderByCreatedAtDesc();

    // Trang công khai không cần owner nên chỉ JOIN lịch
    @EntityGraph(attributePaths = "phases")
    List<Event> findAllByPublication(EventPublication publication);

    // SELECT ... FOR UPDATE: khóa dòng sự kiện tới khi transaction kết thúc. Transaction khác gọi hàm này
    // với cùng id phải ĐỢI. Dùng khi đăng ký để "đếm chỗ rồi thêm" không bị 2 người chen nhau (Bài 11.2).
    // Không kèm EntityGraph: Postgres không cho FOR UPDATE trên LEFT JOIN; lịch giai đoạn tự tải sau (lazy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
