package feveid.event.schedule;

import feveid.event.enums.EventStatus;

// Kết quả tính trạng thái: phaseName = tên giai đoạn hiện tại để hiển thị, null khi DRAFT/CANCELLED
public record CurrentStatus(EventStatus status, String phaseName) {
}
