package feveid.registration.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.event.entity.Event;
import feveid.event.enums.EventStatus;
import feveid.event.repository.EventRepository;
import feveid.event.schedule.EventStatusCalculator;
import feveid.event.schedule.RegistrationWindow;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import feveid.registration.dto.RegistrationResponse;
import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import feveid.registration.repository.RegistrationRepository;
import feveid.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    // Thông báo lỗi dùng chung với test (RegistrationTest kiểm tra đúng lý do bị từ chối)
    public static final String EVENT_NOT_FOUND = "Không tìm thấy sự kiện";
    public static final String NOT_OPEN = "Sự kiện chưa mở hoặc đã đóng đăng ký";
    public static final String ALREADY_REGISTERED = "Bạn đã đăng ký sự kiện này rồi";
    public static final String FULL = "Sự kiện đã hết chỗ";

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Override
    @Transactional
    public RegistrationResponse register(String email, Long eventId) {
        Instant now = clock.instant();
        Member member = findMember(email);

        // ===================== PHẦN LÕI (Bài 11.2) =====================
        // TODO 1. Lấy sự kiện VÀ khóa dòng của nó: eventRepository.findByIdForUpdate(eventId).
        //         Không có -> NotFoundException(EVENT_NOT_FOUND).
        //         Hỏi bản thân: vì sao bước khóa phải đứng TRƯỚC các bước kiểm tra 3, 4, 5?
        //
        // TODO 2. Tính trạng thái hiển thị: statusOf(event, now).
        //         Bản nháp (DRAFT) -> NotFoundException(EVENT_NOT_FOUND)  (member không được biết có bản nháp)
        //
        // TODO 3. Ngoài giờ mở đăng ký -> ConflictException(NOT_OPEN).
        //         Dùng lại RegistrationWindow.isOpen(...) từ 11.1 (nó đã từ chối CANCELLED, COMPLETED)
        //
        // TODO 4. Đã có đăng ký còn hiệu lực -> ConflictException(ALREADY_REGISTERED).
        //         registrationRepository.existsByEventIdAndMemberIdAndStatusIn(..., RegistrationStatus.ACTIVE)
        //
        // TODO 5. Hết chỗ -> ConflictException(FULL).
        //         capacity null = không giới hạn. Số chỗ đã dùng: countByEventIdAndStatusIn(..., ACTIVE)
        //
        // TODO 6. Tạo Registration (event, member, status CONFIRMED vì sự kiện miễn phí), save,
        //         trả RegistrationResponse.from(...)
        // ================================================================
        throw new UnsupportedOperationException("TODO Bài 11.2");
    }

    // Hủy được khi đăng ký còn hiệu lực và sự kiện còn đang mở đăng ký (requirements 12.2: "hủy trước hạn")
    @Override
    @Transactional
    public RegistrationResponse cancel(String email, Long registrationId) {
        Instant now = clock.instant();
        Member member = findMember(email);
        // Đăng ký của người khác -> 404 như không tồn tại (không để lộ id nào là của ai)
        Registration registration = registrationRepository.findByIdAndMemberId(registrationId, member.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đăng ký"));
        if (!RegistrationStatus.ACTIVE.contains(registration.getStatus())) {
            throw new ConflictException("Đăng ký này đã hủy hoặc không còn hiệu lực");
        }
        Event event = registration.getEvent();
        if (!RegistrationWindow.isOpen(statusOf(event, now),
                event.getRegistrationOpensAt(), event.getRegistrationClosesAt(), now)) {
            throw new ConflictException("Đã hết hạn hủy đăng ký");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(now);
        return RegistrationResponse.from(registration);
    }

    private Member findMember(String email) {
        // Đã qua bộ lọc JWT nên tài khoản chắc chắn tồn tại; không có hồ sơ member là lỗi dữ liệu
        return memberRepository.findByAccountEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ thành viên"));
    }

    private static EventStatus statusOf(Event event, Instant now) {
        return EventStatusCalculator.compute(event.getPublication(), event.phaseSlots(), now).status();
    }
}
