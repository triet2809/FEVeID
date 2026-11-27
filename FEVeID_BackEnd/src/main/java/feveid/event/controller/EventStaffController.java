package feveid.event.controller;

import feveid.event.dto.EventStaffRequest;
import feveid.event.dto.EventStaffResponse;
import feveid.event.service.EventStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Team tổ chức/hỗ trợ của sự kiện (requirements.md 5.2, 11.1). Dùng để tick sẵn khi chốt sự kiện (Bài 13.2)
@RestController
@RequestMapping("/api/events/{eventId}/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
@Tag(name = "Event staff", description = "Phân công thành viên (từ nhiều ban) vào team sự kiện")
public class EventStaffController {
    private final EventStaffService eventStaffService;

    @GetMapping
    @Operation(summary = "Xem team sự kiện", description = "Người tổ chức trước, rồi theo tên")
    public ResponseEntity<List<EventStaffResponse>> getStaff(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventStaffService.getStaff(eventId));
    }

    @PutMapping
    @Operation(summary = "Thay cả team",
            description = "Gửi danh sách đầy đủ; ai không có trong list bị gỡ. Mỗi người một vai trò. Khóa khi đã hủy")
    public ResponseEntity<List<EventStaffResponse>> replaceStaff(@PathVariable Long eventId,
                                                                 @Valid @RequestBody EventStaffRequest request) {
        return ResponseEntity.ok(eventStaffService.replaceStaff(eventId, request));
    }
}
