package feveid.term.controller;

import feveid.term.dto.ShopStatusRequest;
import feveid.term.dto.TermRequest;
import feveid.term.dto.TermResponse;
import feveid.term.service.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
@Tag(name = "Term", description = "Kỳ hoạt động (Spring/Summer/Fall): điểm và gian hàng đổi quà tính riêng theo kỳ")
public class TermController {
    private final TermService termService;

    @GetMapping
    @Operation(summary = "Danh sách kỳ", description = "Kỳ mới nhất trước")
    public ResponseEntity<List<TermResponse>> getTerms() {
        return ResponseEntity.ok(termService.getTerms());
    }

    @GetMapping("/{id}")
    @Operation (summary = "Xem một kỳ")
    public ResponseEntity<TermResponse> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(termService.getTermById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Tạo kỳ mới", description = "Không được chồng lên kỳ khác; gian hàng mặc định đóng")
    public ResponseEntity<TermResponse> createTerm(@Valid  @RequestBody TermRequest request) {
        return new ResponseEntity<>(termService.createTerm(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")
    @Operation(summary = "Sửa tên/thời gian kỳ", description = "Không thay đổi trạng thái gian hàng")
    public ResponseEntity<TermResponse> updateTerm(@PathVariable Long id, @Valid  @RequestBody TermRequest request) {
        return ResponseEntity.ok(termService.updateTerm(id, request));
    }

    @PatchMapping("/{id}/shop")
    @PreAuthorize("hasAnyRole('HR','PRESIDENT')")
    @Operation(summary = "Mở/đóng gian hàng đổi quà của kỳ")
    public ResponseEntity<TermResponse> changeShopStatus(@PathVariable Long id, @Valid @RequestBody ShopStatusRequest request){
        return ResponseEntity.ok(termService.changeShopStatus(id, request));
    }

}
