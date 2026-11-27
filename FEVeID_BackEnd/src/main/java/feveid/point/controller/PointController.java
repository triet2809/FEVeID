package feveid.point.controller;

import feveid.point.dto.PointAdjustmentRequest;
import feveid.point.dto.PointSummaryResponse;
import feveid.point.dto.PointTransactionResponse;
import feveid.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Điểm hoạt động (requirements.md 11.3, 11.4). Điểm từ sự kiện được phát khi chốt sự kiện (Bài 13)
@RestController
@RequiredArgsConstructor
@Tag(name = "Point", description = "Điểm hoạt động (requirements.md 11)")
public class PointController {
    private final PointService pointService;

    @GetMapping("/api/points/me")
    @Operation(summary = "Điểm của tôi", description = "Số dư kỳ hiện tại, tổng trọn đời, lịch sử cộng/trừ")
    public ResponseEntity<PointSummaryResponse> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(pointService.getMySummary(authentication.getName()));
    }

    @GetMapping("/api/members/{memberId}/points")
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Điểm của một thành viên")
    public ResponseEntity<PointSummaryResponse> getMemberSummary(@PathVariable Long memberId) {
        return ResponseEntity.ok(pointService.getMemberSummary(memberId));
    }

    @PostMapping("/api/members/{memberId}/points/adjustments")
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Điều chỉnh điểm thủ công",
            description = "Bắt buộc lý do. Bỏ trống termId = kỳ hiện tại. 409 nếu số dư kỳ sẽ âm hoặc không có kỳ hiện tại")
    public ResponseEntity<PointTransactionResponse> adjust(Authentication authentication,
                                                           @PathVariable Long memberId,
                                                           @Valid @RequestBody PointAdjustmentRequest request) {
        return new ResponseEntity<>(pointService.adjust(authentication.getName(), memberId, request),
                HttpStatus.CREATED);
    }
}
