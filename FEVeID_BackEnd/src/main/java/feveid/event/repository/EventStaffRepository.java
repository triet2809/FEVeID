package feveid.event.repository;

import feveid.event.entity.EventStaff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventStaffRepository extends JpaRepository<EventStaff, Long> {
    // Người tổ chức trước (ORGANIZER < SUPPORTER theo chữ cái), rồi theo tên.
    // EntityGraph: JOIN luôn member để response có tên, tránh N+1
    @EntityGraph(attributePaths = "member")
    List<EventStaff> findByEventIdOrderByRoleAscMemberFullNameAsc(Long eventId);

    // Câu DELETE chạy NGAY khi gọi. Không dùng deleteAll(list) vì Hibernate xếp INSERT trước DELETE lúc flush:
    // đổi vai trò của cùng một người (xóa dòng cũ, thêm dòng mới) sẽ đụng unique (event_id, member_id)
    @Modifying
    @Query("DELETE FROM EventStaff s WHERE s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
