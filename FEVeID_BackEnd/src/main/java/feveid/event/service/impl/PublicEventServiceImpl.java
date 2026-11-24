package feveid.event.service.impl;

import feveid.common.exception.NotFoundException;
import feveid.event.dto.PublicEventResponse;
import feveid.event.entity.Event;
import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;
import feveid.event.repository.EventRepository;
import feveid.event.schedule.CurrentStatus;
import feveid.event.schedule.EventStatusCalculator;
import feveid.event.schedule.RegistrationWindow;
import feveid.event.service.PublicEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {

    // Chưa kết thúc lên trước (sắp diễn ra sớm nhất đứng đầu); đã kết thúc xếp sau (mới kết thúc đứng đầu)
    private static final Comparator<PublicEventResponse> DISPLAY_ORDER =
            Comparator.comparing((PublicEventResponse e) -> e.status() == EventStatus.COMPLETED)
                    .thenComparing((a, b) -> a.status() == EventStatus.COMPLETED
                            ? b.startsAt().compareTo(a.startsAt())
                            : a.startsAt().compareTo(b.startsAt()));

    private final EventRepository eventRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<PublicEventResponse> getEvents(boolean signedIn, EventStatus status, String keyword) {
        Instant now = clock.instant();
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        // Lọc trong Java vì trạng thái tính lúc đọc (không có cột status để WHERE).
        // CLB ít sự kiện nên chấp nhận được; nhiều lên thì thêm phân trang (đã ghi ở "Còn treo")
        return eventRepository.findAllByPublication(EventPublication.PUBLISHED).stream()
                .filter(event -> signedIn || event.isVisibleToGuests())
                .filter(event -> event.getName().toLowerCase(Locale.ROOT).contains(needle))
                .map(event -> toResponse(event, now))
                .filter(event -> status == null || event.status() == status)
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicEventResponse getEvent(Long id, boolean signedIn) {
        // Bản nháp, đã hủy, hoặc khách mở sự kiện nội bộ không hiện công khai: đều 404 như không tồn tại,
        // để người ngoài không dò được id nào là sự kiện nội bộ
        Event event = eventRepository.findById(id)
                .filter(e -> e.getPublication() == EventPublication.PUBLISHED)
                .filter(e -> signedIn || e.isVisibleToGuests())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        return toResponse(event, clock.instant());
    }

    private PublicEventResponse toResponse(Event event, Instant now) {
        CurrentStatus current = EventStatusCalculator.compute(event.getPublication(), event.phaseSlots(), now);
        boolean open = RegistrationWindow.isOpen(current.status(),
                event.getRegistrationOpensAt(), event.getRegistrationClosesAt(), now);
        return PublicEventResponse.from(event, current, open);
    }
}
