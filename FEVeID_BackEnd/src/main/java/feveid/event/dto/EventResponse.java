package feveid.event.dto;

import feveid.event.entity.Event;
import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;
import feveid.event.enums.EventVisibility;
import feveid.event.enums.PhaseKind;
import feveid.event.schedule.CurrentStatus;
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PhaseResponse> phases,
        // Chốt sự kiện (Bài 13.2): null = chưa chốt
        Instant finalizedAt,
        String finalizedByName
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
                event.firstStartOf(PhaseKind.ONGOING),
                event.firstStartOf(PhaseKind.COMPLETED),
                event.getCapacity(),
                event.getRegistrationOpensAt(),
                event.getRegistrationClosesAt(),
                event.getOwner() == null ? null
                        : new OwnerRef(event.getOwner().getId(), event.getOwner().getFullName()),
                event.getPhases().stream().map(PhaseResponse::from).toList(),
                event.getFinalizedAt(),
                event.getFinalizedBy() == null ? null : event.getFinalizedBy().getFullName()
        );
    }
}
