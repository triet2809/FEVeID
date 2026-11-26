package feveid.registration.controller;

import feveid.registration.dto.CheckInCodeResponse;
import feveid.registration.dto.CheckInRequest;
import feveid.registration.dto.CheckInResponse;
import feveid.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Check-in bằng mã QR xoay vòng (requirements.md 12.3). Check-in tay của HR nằm ở EventRegistrationController
@RestController
@RequiredArgsConstructor
@Tag(name = "Check-in", description = "Check-in sự kiện bằng QR (requirements.md 12.3)")
public class CheckInController {
    private final RegistrationService registrationService;

    // GET chỉ đọc: mã được tính ra, không ghi gì vào DB (CSRF không kiểm tra GET, xem handoff Bài 9)
    @GetMapping("/api/events/{eventId}/check-in-code")
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Mã QR check-in hiện tại",
            description = "Cho màn hình chiếu. Mã đổi mỗi 30 giây, gọi lại sau expiresAt. 409 nếu sự kiện không ONGOING")
    public ResponseEntity<CheckInCodeResponse> getCheckInCode(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getCheckInCode(eventId));
    }

    @PostMapping("/api/check-ins")
    @Operation(summary = "Member tự check-in bằng mã QR",
            description = "Quét lại vẫn 200. Chưa đăng ký mà còn chỗ thì tự tạo đăng ký. "
                    + "400 nếu mã sai/hết hạn, 409 nếu sự kiện không ONGOING hoặc hết chỗ")
    public ResponseEntity<CheckInResponse> checkIn(Authentication authentication,
                                                   @Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(registrationService.checkIn(authentication.getName(), request.eventId(),
                request.code()));
    }
}
