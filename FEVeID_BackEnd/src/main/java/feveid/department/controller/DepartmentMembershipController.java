package feveid.department.controller;

import feveid.department.dto.AssignDepartmentRequest;
import feveid.department.dto.DepartmentMemberResponse;
import feveid.department.dto.MembershipResponse;
import feveid.department.service.DepartmentMembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Department Membership", description = "Gán thành viên vào ban và chức vụ trong ban")
public class DepartmentMembershipController {

    private final DepartmentMembershipService membershipService;

    @PutMapping("/api/members/{memberId}/department")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Gán/chuyển ban/chuyển đổi chức vụ", description = "Đóng ban hiện tại (nếu có) và mở ban mới. Gửi lại đúng ban + chức vụ hiện tại thì không đổi gì")
    public ResponseEntity<MembershipResponse> assign(@PathVariable Long memberId, @Valid @RequestBody AssignDepartmentRequest request){
        return ResponseEntity.ok(membershipService.assign(memberId, request));
    }

    @DeleteMapping("/api/members/{memberId}/department")
    @PreAuthorize("hasRole('HR')")
    @Operation(summary = "Rút thành viên khỏi ban")
    public ResponseEntity<Void> remove(@PathVariable Long memberId){
        membershipService.remove(memberId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/departments/{departmentId}/members")
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Thành viên hiện tại của ban", description = "Trưởng ban -> Phó ban -> Thành viên, rồi theo tên")
    public ResponseEntity<List<DepartmentMemberResponse>> getCurrentMembers(@PathVariable Long departmentId){
        return ResponseEntity.ok(membershipService.getCurrentMembers(departmentId));
    }
}
