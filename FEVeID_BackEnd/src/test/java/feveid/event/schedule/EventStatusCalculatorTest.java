package feveid.event.schedule;

import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;
import feveid.event.enums.PhaseKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit test thuần: không có @SpringBootTest nên không bật Spring, không cần Docker, chạy rất nhanh
class EventStatusCalculatorTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // Giờ viết theo giờ Việt Nam cho dễ đọc, đổi sang Instant (mốc tuyệt đối) giống cách DB lưu timestamptz
    private static Instant vn(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(VN).toInstant();
    }

    // Lịch mẫu: 2 giai đoạn ONGOING liền nhau (Check-in rồi Biểu diễn)
    private static final List<PhaseSlot> SCHEDULE = List.of(
            new PhaseSlot("Coming soon", vn("2026-12-01T00:00"), PhaseKind.UPCOMING),
            new PhaseSlot("Check-in", vn("2026-12-05T08:00"), PhaseKind.ONGOING),
            new PhaseSlot("Biểu diễn", vn("2026-12-05T09:00"), PhaseKind.ONGOING),
            new PhaseSlot("Done", vn("2026-12-05T17:00"), PhaseKind.COMPLETED)
    );

    @Test
    void draftIgnoresSchedule() {
        CurrentStatus result = EventStatusCalculator.compute(EventPublication.DRAFT, SCHEDULE, vn("2026-12-05T10:00"));

        assertThat(result).isEqualTo(new CurrentStatus(EventStatus.DRAFT, null));
    }

    @Test
    void draftWithoutScheduleIsAllowed() {
        CurrentStatus result = EventStatusCalculator.compute(EventPublication.DRAFT, List.of(), vn("2026-12-05T10:00"));

        assertThat(result).isEqualTo(new CurrentStatus(EventStatus.DRAFT, null));
    }

    @Test
    void cancelledIgnoresSchedule() {
        CurrentStatus result = EventStatusCalculator.compute(EventPublication.CANCELLED, SCHEDULE, vn("2026-12-05T10:00"));

        assertThat(result).isEqualTo(new CurrentStatus(EventStatus.CANCELLED, null));
    }

    @Test
    void publishedWithoutScheduleIsProgrammingError() {
        assertThatThrownBy(() -> EventStatusCalculator.compute(EventPublication.PUBLISHED, List.of(), vn("2026-12-05T10:00")))
                .isInstanceOf(IllegalStateException.class);
    }

    // Mỗi dòng là một ca: now (giờ VN), trạng thái mong đợi, tên giai đoạn mong đợi
    @ParameterizedTest(name = "lúc {0} -> {1} ({2})")
    @CsvSource({
            "2026-11-20T10:00,    UPCOMING,  Coming soon",  // trước mốc đầu tiên -> coi như giai đoạn đầu
            "2026-12-01T00:00,    UPCOMING,  Coming soon",  // đúng mốc đầu
            "2026-12-03T12:00,    UPCOMING,  Coming soon",
            "2026-12-05T07:59:59, UPCOMING,  Coming soon",  // 1 giây trước giờ check-in
            "2026-12-05T08:00,    ONGOING,   Check-in",     // đúng mốc: starts_at <= now tính là đã sang
            "2026-12-05T08:59:59, ONGOING,   Check-in",
            "2026-12-05T09:00,    ONGOING,   Biểu diễn",    // 2 giai đoạn liền nhau cùng kind
            "2026-12-05T16:59:59, ONGOING,   Biểu diễn",
            "2026-12-05T17:00,    COMPLETED, Done",
            "2027-06-01T00:00,    COMPLETED, Done",         // rất lâu sau vẫn là giai đoạn cuối
    })
    void publishedFollowsSchedule(String now, EventStatus expectedStatus, String expectedPhaseName) {
        CurrentStatus result = EventStatusCalculator.compute(EventPublication.PUBLISHED, SCHEDULE, vn(now));

        assertThat(result).isEqualTo(new CurrentStatus(expectedStatus, expectedPhaseName));
    }
}
