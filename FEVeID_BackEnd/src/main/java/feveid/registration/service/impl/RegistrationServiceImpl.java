package feveid.registration.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.ForbiddenException;
import feveid.common.exception.NotFoundException;
import feveid.event.entity.Event;
import feveid.event.enums.EventStatus;
import feveid.event.enums.EventVisibility;
import feveid.event.repository.EventRepository;
import feveid.event.schedule.EventStatusCalculator;
import feveid.event.schedule.RegistrationWindow;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import feveid.registration.dto.EventRegistrationResponse;
import feveid.registration.dto.GuestRegistrationRequest;
import feveid.registration.dto.MyRegistrationResponse;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    // Thông báo lỗi dùng chung với test (RegistrationTest kiểm tra đúng lý do bị từ chối)
    public static final String EVENT_NOT_FOUND = "Không tìm thấy sự kiện";
    public static final String NOT_OPEN = "Sự kiện chưa mở hoặc đã đóng đăng ký";
    public static final String ALREADY_REGISTERED = "Bạn đã đăng ký sự kiện này rồi";
    public static final String FULL = "Sự kiện đã hết chỗ";
    public static final String MEMBERS_ONLY = "Sự kiện chỉ dành cho thành viên CLB";
    public static final String EMAIL_ALREADY_REGISTERED = "Email này đã đăng ký sự kiện";

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Override
    @Transactional
    public RegistrationResponse register(String email, Long eventId) {
        Instant now = clock.instant();
        Member member = findMember(email);
        Event event = lockPublishedEvent(eventId, now);   // bước 1, 2
        ensureRegistrationOpen(event, now);               // bước 3
        // 4. Đã có đăng ký còn hiệu lực (CONFIRMED...). Đăng ký đã hủy thì không tính
        if (registrationRepository.existsByEventIdAndMemberIdAndStatusIn(eventId, member.getId(), RegistrationStatus.ACTIVE)) {
            throw new ConflictException(ALREADY_REGISTERED);
        }
        ensureSeatAvailable(event);                       // bước 5
        // 6. Sự kiện miễn phí: đăng ký xong là CONFIRMED luôn (có phí thì PENDING_PAYMENT, Bài 18)
        Registration registration = Registration.builder()
                .event(event)
                .member(member)
                .status(RegistrationStatus.CONFIRMED)
                .build();
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    // Khách (requirements 12.1, 12.2): cùng thứ tự khóa -> kiểm tra -> ghi như member. Member và khách cùng khóa
    // một dòng sự kiện nên dùng chung số chỗ mà không chen nhau
    @Override
    @Transactional
    public RegistrationResponse registerGuest(Long eventId, GuestRegistrationRequest request) {
        Instant now = clock.instant();
        Event event = lockPublishedEvent(eventId, now);
        // Khách không được thấy sự kiện này -> 404 giống trang công khai (PublicEventServiceImpl.getEvent)
        if (!event.isVisibleToGuests()) {
            throw new NotFoundException(EVENT_NOT_FOUND);
        }
        // Khách thấy được (INTERNAL được chọn hiện công khai) nhưng không đăng ký được
        if (event.getVisibility() != EventVisibility.PUBLIC) {
            throw new ForbiddenException(MEMBERS_ONLY);
        }
        ensureRegistrationOpen(event, now);
        if (registrationRepository.existsByEventIdAndGuestEmailIgnoreCaseAndStatusIn(
                eventId, request.email(), RegistrationStatus.ACTIVE)) {
            throw new ConflictException(EMAIL_ALREADY_REGISTERED);
        }
        ensureSeatAvailable(event);
        Registration registration = Registration.builder()
                .event(event)
                .guestName(request.fullName())
                .guestEmail(request.email())
                .guestPhone(request.phone())
                .guestSchool(request.school())
                .status(RegistrationStatus.CONFIRMED)
                .build();
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    // 1. Lấy sự kiện và KHÓA dòng của nó (SELECT ... FOR UPDATE).
    //    Transaction khác gọi cùng eventId sẽ đứng đợi ở đây cho tới khi transaction của mình commit.
    // 2. Bản nháp: không được biết là có -> 404 như không tồn tại
    private Event lockPublishedEvent(Long eventId, Instant now) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        if (statusOf(event, now) == EventStatus.DRAFT) {
            throw new NotFoundException(EVENT_NOT_FOUND);
        }
        return event;
    }

    // 3. Ngoài giờ mở đăng ký. isOpen đã tự từ chối CANCELLED và COMPLETED
    private static void ensureRegistrationOpen(Event event, Instant now) {
        if (!RegistrationWindow.isOpen(statusOf(event, now), event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(), now)) {
            throw new ConflictException(NOT_OPEN);
        }
    }

    // 5. Hết chỗ. capacity null = không giới hạn nên bỏ qua bước đếm.
    //    Chỉ đúng khi đã khóa dòng sự kiện trước (lockPublishedEvent)
    private void ensureSeatAvailable(Event event) {
        if (event.getCapacity() != null) {
            long used = registrationRepository.countByEventIdAndStatusIn(event.getId(), RegistrationStatus.ACTIVE);
            if (used >= event.getCapacity()) {
                throw new ConflictException(FULL);
            }
        }
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
        if (!canCancel(registration, now)) {
            throw new ConflictException("Đã hết hạn hủy đăng ký");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(now);
        return RegistrationResponse.from(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyRegistrationResponse> getMyRegistrations(String email) {
        Instant now = clock.instant();
        Member member = findMember(email);
        return registrationRepository.findByMemberIdOrderByCreatedAtDescIdDesc(member.getId()).stream()
                .map(registration -> MyRegistrationResponse.from(registration,
                        statusOf(registration.getEvent(), now), canCancel(registration, now)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getEventRegistrations(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND);
        }
        return registrationRepository.findByEventIdOrderByCreatedAtAscIdAsc(eventId).stream()
                .map(EventRegistrationResponse::from)
                .toList();
    }

    // Dùng chung cho cancel() và cờ cancellable ở "Sự kiện của tôi": nút Hủy hiện ra thì bấm phải được
    private static boolean canCancel(Registration registration, Instant now) {
        Event event = registration.getEvent();
        return RegistrationStatus.ACTIVE.contains(registration.getStatus())
                && RegistrationWindow.isOpen(statusOf(event, now),
                event.getRegistrationOpensAt(), event.getRegistrationClosesAt(), now);
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
