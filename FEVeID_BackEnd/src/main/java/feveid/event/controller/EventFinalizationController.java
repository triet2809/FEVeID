package feveid.event.controller;

import feveid.event.dto.FinalizationResponse;
import feveid.event.dto.FinalizeEventRequest;
import feveid.event.service.EventFinalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Chốt sự kiện (requirements.md 11.1): cộng điểm hàng loạt và khóa sự kiện
@RestController
@RequestMapping("/api/events/{eventId}/finalization")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
@Tag(name = "Event finalization", description = "Chốt sự kiện: tổ chức +4, hỗ trợ +2, tham gia (check-in) +1")
public class EventFinalizationController {
    private final EventFinalizationService eventFinalizationService;

    @GetMapping
    @Operation(summary = "Xem trước / kết quả chốt",
            description = "Chưa chốt: team mặc định, người đã check-in, điểm dự kiến. Đã chốt: điểm đã ghi trong sổ cái")
    public ResponseEntity<FinalizationResponse> getFinalization(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventFinalizationService.getFinalization(eventId));
    }

    @PostMapping
    @Operation(summary = "Chốt sự kiện",
            description = "Chỉ khi COMPLETED và chưa chốt. Mỗi người một vai trò. Không hoàn tác được")
    public ResponseEntity<FinalizationResponse> finalizeEvent(@PathVariable Long eventId,
                                                              @Valid @RequestBody FinalizeEventRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(
                eventFinalizationService.finalizeEvent(authentication.getName(), eventId, request));
    }
}
