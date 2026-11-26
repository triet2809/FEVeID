package feveid.registration.service.impl;

import feveid.common.exception.BadRequestException;
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
import feveid.registration.checkin.CheckInCodes;
import feveid.registration.dto.CheckInCodeResponse;
import feveid.registration.dto.CheckInResponse;
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
    public static final String NOT_ONGOING = "Chỉ check-in được khi sự kiện đang diễn ra";
    public static final String INVALID_CODE = "Mã check-in không đúng hoặc đã hết hạn, hãy quét lại mã trên màn hình";
    public static final String REGISTRATION_NOT_FOUND = "Không tìm thấy đăng ký";
    public static final String NOT_CHECKABLE = "Đăng ký này không còn hiệu lực để check-in";

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

    // ---------- Check-in (Bài 12) ----------

    // Chỉ đọc nên không cần khóa. Mã tính lại mỗi lần gọi (CheckInCodes), không ghi gì vào DB
    @Override
    @Transactional(readOnly = true)
    public CheckInCodeResponse getCheckInCode(Long eventId) {
        Instant now = clock.instant();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        if (statusOf(event, now) != EventStatus.ONGOING) {
            throw new ConflictException(NOT_ONGOING);
        }
        return new CheckInCodeResponse(
                CheckInCodes.codeAt(event.getCheckInSecret(), now),
                CheckInCodes.expiresAt(now),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CHECKED_IN),
                registrationRepository.countByEventIdAndStatusIn(eventId, RegistrationStatus.ACTIVE));
    }

    // LÕI – chủ dự án tự viết. Thứ tự giống register(): khóa -> kiểm tra -> ghi
    @Override
    @Transactional
    public CheckInResponse checkIn(String email, Long eventId, String code) {
        // TODO 1. now từ clock, member từ email (findMember)
        // TODO 2. Khóa dòng sự kiện (lockPublishedEvent: không có / bản nháp -> 404)
        // TODO 3. Không ONGOING -> ConflictException(NOT_ONGOING)
        // TODO 4. Mã sai / hết hạn -> BadRequestException(INVALID_CODE)   (CheckInCodes.verify)
        // TODO 5. Tìm đăng ký còn hiệu lực của member (findByEventIdAndMemberIdAndStatusIn ... ACTIVE):
        //         - CHECKED_IN       -> trả về luôn, không ghi gì (quét lần 2 vẫn 200)
        //         - CONFIRMED        -> chuyển CHECKED_IN, checkedInAt = now, checkedInBy = null (tự quét)
        //         - PENDING_PAYMENT  -> ConflictException(NOT_CHECKABLE) (chưa thanh toán, Bài 18)
        //         - không có         -> còn chỗ (ensureSeatAvailable) thì tạo đăng ký mới CHECKED_IN
        // TODO 6. return CheckInResponse.from(...) (đăng ký mới thì nhớ save)
        throw new UnsupportedOperationException("TODO: Bài 12.2b");
    }

    // LÕI – chủ dự án tự viết
    @Override
    @Transactional
    public EventRegistrationResponse checkInManually(String staffEmail, Long eventId, Long registrationId) {
        // TODO 1. now, staff = findMember(staffEmail)
        // TODO 2. Khóa dòng sự kiện (cùng khóa với checkIn: member tự quét + HR bấm tay cùng lúc không chen nhau)
        // TODO 3. Không ONGOING -> ConflictException(NOT_ONGOING)
        // TODO 4. Đăng ký phải thuộc sự kiện này (findByIdAndEventId), không thì NotFoundException(REGISTRATION_NOT_FOUND)
        // TODO 5. CHECKED_IN -> trả về luôn; CONFIRMED -> CHECKED_IN, checkedInAt = now, checkedInBy = staff;
        //         trạng thái khác -> ConflictException(NOT_CHECKABLE)
        // TODO 6. return EventRegistrationResponse.from(...)
        throw new UnsupportedOperationException("TODO: Bài 12.2b");
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
