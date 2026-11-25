package feveid.registration.dto;

import feveid.event.entity.Event;
import feveid.event.enums.EventStatus;
import feveid.event.enums.PhaseKind;
import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Một dòng trong "Sự kiện của tôi" (requirements.md 2.4): đăng ký + tóm tắt sự kiện để hiện thẻ sự kiện
public record MyRegistrationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        Instant cancelledAt,
        // Đăng ký còn hiệu lực và sự kiện còn mở đăng ký -> hiện nút "Hủy đăng ký"
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean cancellable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventSummary event
) {
    public record EventSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            String bannerUrl,
            String location,
            // UPCOMING / ONGOING / COMPLETED, hoặc CANCELLED nếu BCN hủy sau khi mình đã đăng ký
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventStatus status,
            // Chỉ đăng ký được sự kiện đã công bố, mà công bố bắt buộc có lịch -> luôn có
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant startsAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant endsAt
    ) {
    }

    public static MyRegistrationResponse from(Registration registration, EventStatus eventStatus, boolean cancellable) {
        Event event = registration.getEvent();
        return new MyRegistrationResponse(
                registration.getId(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getCancelledAt(),
                cancellable,
                new EventSummary(
                        event.getId(),
                        event.getName(),
                        event.getBannerUrl(),
                        event.getLocation(),
                        eventStatus,
                        event.firstStartOf(PhaseKind.ONGOING),
                        event.firstStartOf(PhaseKind.COMPLETED))
        );
    }
}
