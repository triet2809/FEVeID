package feveid.point.dto;

import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Một dòng lịch sử điểm (requirements 11.3: nguồn là sự kiện nào, vai trò gì, đổi quà gì)
public record PointTransactionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long termId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String termName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PointTransactionType type,
        Long eventId,
        String eventName,
        String reason,
        // null = hệ thống tự ghi
        String createdByName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {
    public static PointTransactionResponse from(PointTransaction t) {
        return new PointTransactionResponse(
                t.getId(),
                t.getTerm().getId(),
                t.getTerm().getName(),
                t.getAmount(),
                t.getType(),
                t.getEvent() == null ? null : t.getEvent().getId(),
                t.getEvent() == null ? null : t.getEvent().getName(),
                t.getReason(),
                t.getCreatedBy() == null ? null : t.getCreatedBy().getFullName(),
                t.getCreatedAt()
        );
    }
}
