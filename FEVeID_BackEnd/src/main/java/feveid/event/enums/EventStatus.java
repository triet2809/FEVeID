package feveid.event.enums;

// Trạng thái hiển thị (requirements.md mục 6.1): không lưu DB, tính từ EventPublication + lịch giai đoạn + now
public enum EventStatus {
    DRAFT,
    UPCOMING,
    ONGOING,
    COMPLETED,
    CANCELLED
}
