package feveid.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Điểm của một thành viên (requirements 11.3): số dư kỳ hiện tại, tổng trọn đời, lịch sử
public record PointSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long memberId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        // null khi hôm nay không thuộc kỳ nào (khoảng trống giữa 2 kỳ)
        Long currentTermId,
        String currentTermName,
        // 0 khi không có kỳ hiện tại
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long currentBalance,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long lifetimePoints,
        // Mới nhất trước, gồm mọi kỳ
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PointTransactionResponse> history
) {
}
