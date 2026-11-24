package feveid.event.schedule;

import feveid.event.enums.EventStatus;

import java.time.Instant;

// Sự kiện có đang nhận đăng ký không (requirements.md 6.1, 12.1). Hàm thuần như EventStatusCalculator.
// Chưa xét số chỗ còn lại và người đăng ký là ai: phần đó do service đăng ký kiểm tra (Bài 11.2)
public final class RegistrationWindow {

    private RegistrationWindow() {
    }

    /**
     * @param status   trạng thái hiển thị đã tính bằng EventStatusCalculator
     * @param opensAt  giờ mở đăng ký, null = chưa đặt
     * @param closesAt giờ đóng đăng ký (đúng lúc closesAt là đã đóng)
     */
    public static boolean isOpen(EventStatus status, Instant opensAt, Instant closesAt, Instant now) {
        // Chỉ sự kiện đã công bố và chưa kết thúc (DRAFT/CANCELLED/COMPLETED đều không nhận)
        if (status != EventStatus.UPCOMING && status != EventStatus.ONGOING) {
            return false;
        }
        if (opensAt == null || closesAt == null) {
            return false;
        }
        return !now.isBefore(opensAt) && now.isBefore(closesAt);
    }
}
