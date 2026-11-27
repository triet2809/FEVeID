package feveid.point;

import feveid.event.enums.StaffRole;
import feveid.point.config.PointRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Mức điểm cấu hình (app.points.*): test thuần Java, không cần Spring
class PointRulesTest {

    @Test
    void customValues_used() {
        PointRules rules = new PointRules(5, 3, 2);

        assertThat(rules.pointsFor(StaffRole.ORGANIZER)).isEqualTo(5);
        assertThat(rules.pointsFor(StaffRole.SUPPORTER)).isEqualTo(3);
        assertThat(rules.getAttend()).isEqualTo(2);
    }

    @Test
    void zeroOrNegative_failsAtStartup() {
        // Sổ cái chặn amount = 0 (V12): cấu hình sai phải báo ngay lúc khởi động, không đợi tới lúc chốt
        assertThatThrownBy(() -> new PointRules(4, 0, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PointRules(4, 2, -1)).isInstanceOf(IllegalStateException.class);
    }
}
