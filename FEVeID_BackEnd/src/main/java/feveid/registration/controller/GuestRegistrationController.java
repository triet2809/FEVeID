package feveid.registration.controller;

import feveid.common.exception.BadRequestException;
import feveid.common.security.Authentications;
import feveid.registration.dto.GuestRegistrationRequest;
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

// Khách đăng ký không cần tài khoản (requirements.md 12.1, 12.2). SecurityConfig mở (permitAll) cho POST này
@RestController
@RequestMapping("/api/public/events/{eventId}/registrations")
@RequiredArgsConstructor
@Tag(name = "Registration", description = "Đăng ký tham gia sự kiện (requirements.md 12.2)")
public class GuestRegistrationController {
    private final RegistrationService registrationService;

    @PostMapping
    @Operation(summary = "Khách đăng ký sự kiện",
            description = "Chỉ sự kiện PUBLIC. 403 nếu sự kiện chỉ dành cho thành viên; "
                    + "409 nếu ngoài giờ đăng ký, email đã đăng ký, hết chỗ")
    public ResponseEntity<RegistrationResponse> register(Authentication authentication,
                                                         @PathVariable Long eventId,
                                                         @Valid @RequestBody GuestRegistrationRequest request) {
        // Đã đăng nhập mà đăng ký như khách thì không bị chặn trùng theo member_id -> bắt dùng API của member
        if (Authentications.isSignedIn(authentication)) {
            throw new BadRequestException("Bạn đã đăng nhập, hãy đăng ký bằng tài khoản thành viên");
        }
        return new ResponseEntity<>(registrationService.registerGuest(eventId, request), HttpStatus.CREATED);
    }
}
