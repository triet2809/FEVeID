package feveid.event.dto;

import feveid.event.entity.Event;
import feveid.event.enums.EventStatus;
import feveid.event.enums.EventVisibility;
import feveid.event.enums.PhaseKind;
import feveid.event.schedule.CurrentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// Sự kiện cho member/khách xem. Khác EventResponse: không có owner, publication, publiclyListed
// (thông tin nội bộ), có thêm registrationOpen
public record PublicEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String description,
        String bannerUrl,
        String location,
        // INTERNAL: chỉ thành viên đăng ký được; PUBLIC: cả khách
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventVisibility visibility,
        // Chỉ UPCOMING / ONGOING / COMPLETED (API công khai không trả DRAFT, CANCELLED)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String currentPhaseName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt,
        // null = không giới hạn
        Integer capacity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationOpensAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant registrationClosesAt,
        // Đang trong giờ mở đăng ký và sự kiện chưa kết thúc. Chưa xét số chỗ còn lại (Bài 11.2)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean registrationOpen,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PhaseResponse> phases
) {
    // Các field REQUIRED ở trên chắc chắn có giá trị vì chỉ gọi với sự kiện PUBLISHED:
    // công bố bắt buộc có lịch hợp lệ + giờ mở/đóng đăng ký (EventServiceImpl.publishEvent)
    public static PublicEventResponse from(Event event, CurrentStatus current, boolean registrationOpen) {
        return new PublicEventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getBannerUrl(),
                event.getLocation(),
                event.getVisibility(),
                current.status(),
                current.phaseName(),
                event.firstStartOf(PhaseKind.ONGOING),
                event.firstStartOf(PhaseKind.COMPLETED),
                event.getCapacity(),
                event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(),
                registrationOpen,
                event.getPhases().stream().map(PhaseResponse::from).toList()
        );
    }
}
