package feveid.registration.controller;

import feveid.registration.dto.EventRegistrationResponse;
import feveid.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Cùng tiền tố /api/events với EventController nhưng nằm ở module registration (dữ liệu là đăng ký).
// Spring cho nhiều controller dùng chung tiền tố miễn là đường dẫn đầy đủ không trùng
@RestController
@RequestMapping("/api/events/{eventId}/registrations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
@Tag(name = "Registration", description = "Đăng ký tham gia sự kiện (requirements.md 12.2)")
public class EventRegistrationController {
    private final RegistrationService registrationService;

    @GetMapping
    @Operation(summary = "Danh sách đăng ký của sự kiện",
            description = "Gồm cả member và khách, cả đăng ký đã hủy. Đăng ký trước đứng trước")
    public ResponseEntity<List<EventRegistrationResponse>> getEventRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getEventRegistrations(eventId));
    }

    @PostMapping("/{registrationId}/check-in")
    @Operation(summary = "Check-in tay",
            description = "Dự phòng khi không quét được QR, và cho khách. Đã check-in rồi vẫn 200. "
                    + "409 nếu sự kiện không ONGOING hoặc đăng ký đã hủy")
    public ResponseEntity<EventRegistrationResponse> checkInManually(Authentication authentication,
                                                                     @PathVariable Long eventId,
                                                                     @PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.checkInManually(authentication.getName(), eventId,
                registrationId));
    }
}
