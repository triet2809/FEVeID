package feveid.registration.repository;

import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // SELECT count(*) FROM registrations WHERE event_id = ? AND status IN (...)
    long countByEventIdAndStatusIn(Long eventId, Collection<RegistrationStatus> statuses);

    boolean existsByEventIdAndMemberIdAndStatusIn(Long eventId, Long memberId,
                                                  Collection<RegistrationStatus> statuses);

    // IgnoreCase: "An@Gmail.com" và "an@gmail.com" là cùng một email (khớp unique index lower(guest_email) ở V10)
    boolean existsByEventIdAndGuestEmailIgnoreCaseAndStatusIn(Long eventId, String guestEmail,
                                                             Collection<RegistrationStatus> statuses);

    // Chỉ tìm đăng ký của chính member đó: của người khác thì coi như không có (404)
    Optional<Registration> findByIdAndMemberId(Long id, Long memberId);
}
