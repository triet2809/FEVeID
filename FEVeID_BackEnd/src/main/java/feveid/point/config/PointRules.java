package feveid.point.config;

import feveid.event.enums.StaffRole;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Mức điểm khi chốt sự kiện (requirements.md 11.1: "mức điểm cấu hình được"). Đổi trong application.properties;
// điểm đã ghi vào sổ cái giữ nguyên vì mỗi dòng lưu số điểm cụ thể (amount), không tính lại theo mức mới
@Getter
@Component
public class PointRules {
    private final int organize;
    private final int support;
    private final int attend;

    public PointRules(@Value("${app.points.organize:4}") int organize,
                      @Value("${app.points.support:2}") int support,
                      @Value("${app.points.attend:1}") int attend) {
        // Sổ cái chặn amount = 0 (V12) -> cấu hình sai thì báo ngay lúc khởi động, không đợi tới lúc chốt
        if (organize <= 0 || support <= 0 || attend <= 0) {
            throw new IllegalStateException("app.points.* phải là số dương");
        }
        this.organize = organize;
        this.support = support;
        this.attend = attend;
    }

    public int pointsFor(StaffRole role) {
        return switch (role) {
            case ORGANIZER -> organize;
            case SUPPORTER -> support;
        };
    }
}
