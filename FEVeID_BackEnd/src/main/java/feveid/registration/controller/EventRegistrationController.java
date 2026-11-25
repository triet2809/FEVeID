package feveid.registration.controller;

import feveid.registration.dto.EventRegistrationResponse;
import feveid.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}
