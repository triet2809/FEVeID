package feveid.event.controller;

import feveid.event.dto.EventRequest;
import feveid.event.dto.EventResponse;
import feveid.event.dto.PhaseScheduleRequest;
import feveid.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// API quản lý sự kiện cho HR/BCN. Trang sự kiện cho member/khách làm ở Bài 11
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PRESIDENT')")  // đặt ở class: áp dụng cho mọi method bên dưới
@Tag(name = "Event", description = "Quản lý sự kiện: thông tin, lịch giai đoạn, công bố/hủy (requirements.md 5.2, 6.1)")
public class EventController {
    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Danh sách sự kiện", description = "Mới tạo trước, gồm cả bản nháp và đã hủy")
    public ResponseEntity<List<EventResponse>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem một sự kiện")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    @Operation(summary = "Tạo sự kiện", description = "Tạo ở trạng thái DRAFT, chưa có lịch giai đoạn")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return new ResponseEntity<>(eventService.createEvent(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa thông tin sự kiện", description = "Bị khóa khi đã COMPLETED hoặc CANCELLED")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PutMapping("/{id}/phases")
    @Operation(summary = "Thay cả lịch giai đoạn",
            description = "Giai đoạn đầu UPCOMING, giờ tăng dần, kind không đi lùi, có ONGOING và COMPLETED")
    public ResponseEntity<EventResponse> replacePhases(@PathVariable Long id,
                                                       @Valid @RequestBody PhaseScheduleRequest request) {
        return ResponseEntity.ok(eventService.replacePhases(id, request));
    }

    @PostMapping("/{id}/phases/{phaseId}/start-now")
    @Operation(summary = "Chuyển sang giai đoạn này ngay", description = "Đặt giờ bắt đầu giai đoạn = bây giờ")
    public ResponseEntity<EventResponse> startPhaseNow(@PathVariable Long id, @PathVariable Long phaseId) {
        return ResponseEntity.ok(eventService.startPhaseNow(id, phaseId));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Công bố sự kiện", description = "Cần lịch giai đoạn + giờ mở/đóng đăng ký")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy sự kiện", description = "Vĩnh viễn, không mở lại được")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bản nháp", description = "Chỉ xóa được khi DRAFT; đã công bố thì dùng Hủy")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
