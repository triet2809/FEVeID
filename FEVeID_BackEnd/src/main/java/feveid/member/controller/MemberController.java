package feveid.member.controller;

import feveid.member.dto.MemberResponse;
import feveid.member.dto.UpdateMyProfileRequest;
import feveid.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "Hồ sơ thành viên CLB")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "Xem hồ sơ của tôi")
    public ResponseEntity<MemberResponse> getMyProfile(Authentication authentication) {
        // authentication.getName() là email (username lấy từ JWT)
        return ResponseEntity.ok(memberService.getMyProfile(authentication.getName()));
    }
    @PatchMapping("/me")
    @Operation(summary = "cập nhật hồ sơ của tôi", description = "member chỉ được sửa phone và avatarUrl")
    public ResponseEntity<MemberResponse> updateProfile(Authentication authentication, @Valid @RequestBody UpdateMyProfileRequest request) {
        return ResponseEntity.ok(memberService.updateMyProfile(authentication.getName(), request));
    }

}
