package feveid.event.service.impl;

import feveid.common.exception.BadRequestException;
import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.event.dto.EventRequest;
import feveid.event.dto.EventResponse;
import feveid.event.dto.PhaseRequest;
import feveid.event.dto.PhaseScheduleRequest;
import feveid.event.entity.Event;
import feveid.event.entity.EventPhase;
import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;
import feveid.event.enums.PhaseKind;
import feveid.event.repository.EventRepository;
import feveid.event.schedule.CurrentStatus;
import feveid.event.schedule.EventStatusCalculator;
import feveid.event.schedule.PhaseScheduleValidator;
import feveid.event.schedule.PhaseSlot;
import feveid.event.service.EventService;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEvents() {
        Instant now = clock.instant();
        return eventRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(event -> toResponse(event, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        return toResponse(findById(id), clock.instant());
    }

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event();  // publication mặc định DRAFT, lịch rỗng
        applyInfo(event, request);
        return toResponse(eventRepository.save(event), clock.instant());
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Instant now = clock.instant();
        Event event = findById(id);
        ensureScheduleEditable(event, now);
        if (event.getPublication() == EventPublication.PUBLISHED && request.registrationOpensAt() == null) {
            throw new BadRequestException("Sự kiện đã công bố phải có giờ mở/đóng đăng ký");
        }
        applyInfo(event, request);
        // Không cần gọi save(): event lấy từ DB trong transaction nên là "managed",
        // Hibernate tự so sánh và UPDATE khi transaction commit (dirty checking)
        return toResponse(event, now);
    }

    @Override
    @Transactional
    public EventResponse replacePhases(Long id, PhaseScheduleRequest request) {
        Instant now = clock.instant();
        Event event = findById(id);
        ensureScheduleEditable(event, now);

        List<PhaseRequest> phases = request.phases();
        PhaseScheduleValidator.validate(phases.stream()
                .map(phase -> new PhaseSlot(phase.name(), phase.startsAt(), phase.kind()))
                .toList());

        event.replacePhases(phases.stream()
                .map(phase -> EventPhase.builder()
                        .name(phase.name())
                        .startsAt(phase.startsAt())
                        .kind(phase.kind())
                        .build())
                .toList());
        // flush ngay để INSERT các giai đoạn mới -> có id để trả về trong response
        eventRepository.flush();
        return toResponse(event, now);
    }

    @Override
    @Transactional
    public EventResponse startPhaseNow(Long id, Long phaseId) {
        Instant now = clock.instant();
        Event event = findById(id);
        if (event.getPublication() != EventPublication.PUBLISHED) {
            throw new ConflictException("Chỉ chuyển giai đoạn được khi sự kiện đã công bố");
        }
        ensureScheduleEditable(event, now);

        EventPhase phase = event.getPhases().stream()
                .filter(p -> p.getId().equals(phaseId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        if (!phase.getStartsAt().isAfter(now)) {
            throw new ConflictException("Giai đoạn \"" + phase.getName() + "\" đã bắt đầu rồi");
        }
        phase.setStartsAt(now);
        // Kéo mốc về now có thể làm sai thứ tự (vd bấm "Done" khi chưa tới "Check-in") -> validator báo 400.
        // Exception làm transaction rollback nên thay đổi setStartsAt ở trên không được lưu
        PhaseScheduleValidator.validate(event.phaseSlots());
        return toResponse(event, now);
    }

    @Override
    @Transactional
    public EventResponse publishEvent(Long id) {
        Instant now = clock.instant();
        Event event = findById(id);
        if (event.getPublication() != EventPublication.DRAFT) {
            throw new ConflictException("Chỉ công bố được sự kiện đang là bản nháp");
        }
        if (event.getPhases().isEmpty()) {
            throw new BadRequestException("Cần lập lịch giai đoạn trước khi công bố");
        }
        if (event.getRegistrationOpensAt() == null) {
            throw new BadRequestException("Cần đặt giờ mở/đóng đăng ký trước khi công bố");
        }
        // Lịch đã qua validator nên chắc chắn có giai đoạn COMPLETED
        Instant endsAt = event.getPhases().stream()
                .filter(phase -> phase.getKind() == PhaseKind.COMPLETED)
                .findFirst().orElseThrow().getStartsAt();
        if (!endsAt.isAfter(now)) {
            throw new BadRequestException("Theo lịch, sự kiện đã kết thúc. Hãy dời lịch trước khi công bố");
        }
        if (event.getRegistrationClosesAt().isAfter(endsAt)) {
            throw new BadRequestException("Giờ đóng đăng ký không được sau giờ kết thúc sự kiện");
        }
        event.setPublication(EventPublication.PUBLISHED);
        return toResponse(event, now);
    }

    @Override
    @Transactional
    public EventResponse cancelEvent(Long id) {
        Instant now = clock.instant();
        Event event = findById(id);
        EventStatus status = statusOf(event, now).status();
        if (status == EventStatus.CANCELLED) {
            throw new ConflictException("Sự kiện đã bị hủy trước đó");
        }
        if (status == EventStatus.COMPLETED) {
            throw new ConflictException("Sự kiện đã kết thúc, không thể hủy");
        }
        event.setPublication(EventPublication.CANCELLED);  // vĩnh viễn: không có API nào đổi ngược lại
        return toResponse(event, now);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        Event event = findById(id);
        if (event.getPublication() != EventPublication.DRAFT) {
            throw new ConflictException("Chỉ xóa được bản nháp. Sự kiện đã công bố thì dùng Hủy");
        }
        eventRepository.delete(event);  // cascade xóa luôn các giai đoạn
    }

    private Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
    }

    private CurrentStatus statusOf(Event event, Instant now) {
        return EventStatusCalculator.compute(event.getPublication(), event.phaseSlots(), now);
    }

    private EventResponse toResponse(Event event, Instant now) {
        return EventResponse.from(event, statusOf(event, now));
    }

    // Sau khi COMPLETED hoặc CANCELLED thì khóa (requirements.md 6.1)
    private void ensureScheduleEditable(Event event, Instant now) {
        EventStatus status = statusOf(event, now).status();
        if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED) {
            throw new ConflictException("Sự kiện đã kết thúc hoặc đã hủy, không thể chỉnh sửa");
        }
    }

    private void applyInfo(Event event, EventRequest request) {
        event.setName(request.name());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setVisibility(request.visibility());
        event.setCapacity(request.capacity());
        event.setRegistrationOpensAt(request.registrationOpensAt());
        event.setRegistrationClosesAt(request.registrationClosesAt());
        event.setOwner(findMember(request.ownerMemberId()));
    }

    // id nằm trong body (không phải trên URL) nên sai id là dữ liệu gửi lên sai -> 400, không phải 404
    private Member findMember(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException("Người phụ trách không tồn tại"));
    }
}
