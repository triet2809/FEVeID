package feveid.registration.controller;

import feveid.registration.dto.MyRegistrationResponse;
import feveid.registration.dto.RegistrationRequest;
import feveid.registration.dto.RegistrationResponse;
import feveid.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Member đăng ký sự kiện (requirements.md 12.2). Mọi role đều là member nên chỉ cần đăng nhập
// (SecurityConfig: anyRequest().authenticated()). Khách đăng ký qua API riêng ở Bài 11.3
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@Tag(name = "Registration", description = "Đăng ký tham gia sự kiện (requirements.md 12.2)")
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping
    @Operation(summary = "Đăng ký sự kiện",
            description = "Sự kiện miễn phí: đăng ký xong là CONFIRMED. 409 nếu ngoài giờ đăng ký, đã đăng ký, hết chỗ")
    public ResponseEntity<RegistrationResponse> register(Authentication authentication,
                                                         @Valid @RequestBody RegistrationRequest request) {
        return new ResponseEntity<>(registrationService.register(authentication.getName(), request.eventId()),
                HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Sự kiện của tôi", description = "Đăng ký mới nhất trước, gồm cả đăng ký đã hủy")
    public ResponseEntity<List<MyRegistrationResponse>> getMyRegistrations(Authentication authentication) {
        return ResponseEntity.ok(registrationService.getMyRegistrations(authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đăng ký của tôi", description = "Chỉ hủy được khi sự kiện còn đang mở đăng ký")
    public ResponseEntity<RegistrationResponse> cancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(registrationService.cancel(authentication.getName(), id));
    }
}
