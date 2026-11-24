package feveid.event.dto;

import feveid.event.entity.Event;
import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;
import feveid.event.enums.EventVisibility;
import feveid.event.enums.PhaseKind;
import feveid.event.schedule.CurrentStatus;
import feveid.event.entity.EventPhase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// REQUIRED = luôn có giá trị. Field không đánh dấu có thể null
public record EventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String description,
        String bannerUrl,
        String location,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventVisibility visibility,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean publiclyListed,
        // Trạng thái do người bấm (DRAFT/PUBLISHED/CANCELLED)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventPublication publication,
        // Trạng thái hiển thị, tính lúc đọc (requirements.md 6.1)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventStatus status,
        // Tên giai đoạn hiện tại, null khi DRAFT/CANCELLED
        String currentPhaseName,
        // Suy ra từ lịch: giai đoạn ONGOING đầu tiên / COMPLETED đầu tiên. null khi chưa có lịch
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Instant registrationOpensAt,
        Instant registrationClosesAt,
        OwnerRef owner,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PhaseResponse> phases
) {
    public record OwnerRef(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName) {
    }

    public static EventResponse from(Event event, CurrentStatus current) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getBannerUrl(),
                event.getLocation(),
                event.getVisibility(),
                event.isPubliclyListed(),
                event.getPublication(),
                current.status(),
                current.phaseName(),
                firstStartOf(event, PhaseKind.ONGOING),
                firstStartOf(event, PhaseKind.COMPLETED),
                event.getCapacity(),
                event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(),
                event.getOwner() == null ? null
                        : new OwnerRef(event.getOwner().getId(), event.getOwner().getFullName()),
                event.getPhases().stream().map(PhaseResponse::from).toList()
        );
    }

    // Lịch đã xếp tăng dần (@OrderBy) nên phần tử đầu tiên có kind này là mốc sớm nhất
    private static Instant firstStartOf(Event event, PhaseKind kind) {
        return event.getPhases().stream()
                .filter(phase -> phase.getKind() == kind)
                .map(EventPhase::getStartsAt)
                .findFirst()
                .orElse(null);
    }
}
