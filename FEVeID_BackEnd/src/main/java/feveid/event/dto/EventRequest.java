package feveid.event.dto;

import feveid.event.enums.EventVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

// Thông tin chung của sự kiện (không gồm lịch giai đoạn, banner và trạng thái: mỗi thứ có API riêng)
public record EventRequest(
        @Size(max = 200)
        @NotBlank
        String name,

        @Size(max = 5000)
        String description,

        @Size(max = 300)
        String location,

        @NotNull
        EventVisibility visibility,

        // null = không giới hạn
        @Positive
        Integer capacity,

        // Nhận dạng ISO có múi giờ, vd "2026-12-01T08:00:00+07:00" hoặc "2026-12-01T01:00:00Z"
        Instant registrationOpensAt,
        Instant registrationClosesAt,

        // Người phụ trách, có thể gán sau (BCN chọn leader khi đã có team)
        Long ownerMemberId
) {
    public EventRequest {
        if (name != null) {
            name = name.trim();
        }
    }

    @AssertTrue(message = "Phải nhập cả giờ mở và giờ đóng đăng ký, hoặc để trống cả hai")
    public boolean isRegistrationWindowComplete() {
        return (registrationOpensAt == null) == (registrationClosesAt == null);
    }

    @AssertTrue(message = "Giờ đóng đăng ký phải sau giờ mở đăng ký")
    public boolean isRegistrationWindowOrdered() {
        if (registrationOpensAt == null || registrationClosesAt == null) {
            return true;
        }
        return registrationClosesAt.isAfter(registrationOpensAt);
    }
}
