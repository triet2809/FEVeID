package feveid.event.repository;

import feveid.event.entity.Event;
import feveid.event.enums.EventPublication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    // EntityGraph: lấy luôn lịch và người phụ trách trong 1 câu SELECT (JOIN),
    // tránh N+1 (mỗi sự kiện lại SELECT thêm phases/owner)
    @EntityGraph(attributePaths = {"phases", "owner"})
    List<Event> findAllByOrderByCreatedAtDesc();

    // Trang công khai không cần owner nên chỉ JOIN lịch
    @EntityGraph(attributePaths = "phases")
    List<Event> findAllByPublication(EventPublication publication);
}
