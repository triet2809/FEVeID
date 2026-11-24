package feveid.event.controller;

import feveid.event.dto.PublicEventResponse;
import feveid.event.enums.EventStatus;
import feveid.event.service.PublicEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Trang sự kiện cho member và khách (requirements.md 1, 2.4). SecurityConfig mở (permitAll) cho GET.
// Không có @PreAuthorize: ai cũng gọi được, nội dung trả về khác nhau tùy đã đăng nhập hay chưa
@RestController
@RequestMapping("/api/public/events")
@RequiredArgsConstructor
@Tag(name = "Public event", description = "Sự kiện đã công bố cho member/khách xem (requirements.md 1, 2.4)")
public class PublicEventController {
    private final PublicEventService publicEventService;

    @GetMapping
    @Operation(summary = "Danh sách sự kiện đã công bố",
            description = "Khách chỉ thấy sự kiện PUBLIC hoặc INTERNAL được chọn hiện công khai; "
                    + "thành viên đã đăng nhập thấy tất cả. Sắp diễn ra lên trước, đã kết thúc xếp sau")
    public ResponseEntity<List<PublicEventResponse>> getEvents(
            Authentication authentication,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(publicEventService.getEvents(isSignedIn(authentication), status, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sự kiện", description = "404 nếu chưa công bố, đã hủy, hoặc khách không được xem")
    public ResponseEntity<PublicEventResponse> getEvent(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(publicEventService.getEvent(id, isSignedIn(authentication)));
    }

    // Route permitAll: chưa đăng nhập thì Spring Security đặt AnonymousAuthenticationToken (thường không phải null)
    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
