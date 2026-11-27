package feveid.point.service;

import feveid.point.dto.PointAdjustmentRequest;
import feveid.point.dto.PointSummaryResponse;
import feveid.point.dto.PointTransactionResponse;

public interface PointService {
    // Member xem điểm của chính mình (email = authentication.getName())
    PointSummaryResponse getMySummary(String email);

    // HR/BCN xem điểm của một thành viên
    PointSummaryResponse getMemberSummary(Long memberId);

    // HR/BCN cộng/trừ tay. Không để số dư của kỳ bị âm, kể cả khi nhiều request cùng lúc
    PointTransactionResponse adjust(String staffEmail, Long memberId, PointAdjustmentRequest request);
}
