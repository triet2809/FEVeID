package feveid.registration.repository;

import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // SELECT count(*) FROM registrations WHERE event_id = ? AND status IN (...)
    long countByEventIdAndStatusIn(Long eventId, Collection<RegistrationStatus> statuses);

    boolean existsByEventIdAndMemberIdAndStatusIn(Long eventId, Long memberId,
                                                  Collection<RegistrationStatus> statuses);

    // IgnoreCase: "An@Gmail.com" và "an@gmail.com" là cùng một email (khớp unique index lower(guest_email) ở V10)
    boolean existsByEventIdAndGuestEmailIgnoreCaseAndStatusIn(Long eventId, String guestEmail,
                                                             Collection<RegistrationStatus> statuses);

    // "Sự kiện của tôi": lấy kèm sự kiện + lịch (để tính trạng thái) trong 1 câu SELECT, tránh N+1
    @EntityGraph(attributePaths = {"event", "event.phases"})
    List<Registration> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    // Danh sách cho HR: lấy kèm hồ sơ + tài khoản (email) của member; dòng của khách thì member = null
    @EntityGraph(attributePaths = {"member", "member.account"})
    List<Registration> findByEventIdOrderByCreatedAtAscIdAsc(Long eventId);

    // Chỉ tìm đăng ký của chính member đó: của người khác thì coi như không có (404)
    Optional<Registration> findByIdAndMemberId(Long id, Long memberId);

    // Đăng ký còn hiệu lực của member ở sự kiện: tối đa 1 dòng nhờ unique index một phần ở V10
    Optional<Registration> findByEventIdAndMemberIdAndStatusIn(Long eventId, Long memberId,
                                                                Collection<RegistrationStatus> statuses);

    // Check-in tay: đăng ký phải thuộc đúng sự kiện trên URL, không thì coi như không có (404)
    Optional<Registration> findByIdAndEventId(Long id, Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
