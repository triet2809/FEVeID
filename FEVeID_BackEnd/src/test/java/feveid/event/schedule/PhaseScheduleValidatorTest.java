package feveid.event.schedule;

import feveid.common.exception.BadRequestException;
import feveid.event.enums.PhaseKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static feveid.event.enums.PhaseKind.COMPLETED;
import static feveid.event.enums.PhaseKind.ONGOING;
import static feveid.event.enums.PhaseKind.UPCOMING;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhaseScheduleValidatorTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // Giai đoạn ngày 05/12/2026 lúc hh:mm giờ VN; tên lấy luôn theo kind cho gọn
    private static PhaseSlot at(String hhmm, PhaseKind kind) {
        Instant startsAt = LocalDateTime.parse("2026-12-05T" + hhmm).atZone(VN).toInstant();
        return new PhaseSlot(kind.name(), startsAt, kind);
    }

    private static void assertValid(List<PhaseSlot> phases) {
        assertThatCode(() -> PhaseScheduleValidator.validate(phases)).doesNotThrowAnyException();
    }

    // Chỉ kiểm loại exception + có message, không so từng chữ: câu chữ do bạn tự đặt
    private static void assertInvalid(List<PhaseSlot> phases) {
        assertThatThrownBy(() -> PhaseScheduleValidator.validate(phases))
                .isInstanceOf(BadRequestException.class)
                .hasMessageMatching("(?s).*\\S.*");
    }

    // ---- Lịch hợp lệ ----

    @Test
    void minimalSchedule() {
        assertValid(List.of(at("07:00", UPCOMING), at("08:00", ONGOING), at("17:00", COMPLETED)));
    }

    @Test
    void consecutivePhasesMayShareKind() {
        assertValid(List.of(
                at("06:00", UPCOMING), at("07:00", UPCOMING),
                at("08:00", ONGOING), at("09:00", ONGOING),
                at("17:00", COMPLETED), at("18:00", COMPLETED)));
    }

    // ---- Lịch sai ----

    @Test
    void emptySchedule() {
        assertInvalid(List.of());
    }

    @Test
    void firstPhaseMustBeUpcoming() {
        assertInvalid(List.of(at("08:00", ONGOING), at("17:00", COMPLETED)));
    }

    @Test
    void missingOngoing() {
        assertInvalid(List.of(at("07:00", UPCOMING), at("17:00", COMPLETED)));
    }

    @Test
    void missingCompleted() {
        assertInvalid(List.of(at("07:00", UPCOMING), at("08:00", ONGOING)));
    }

    @Test
    void onlyUpcoming() {
        assertInvalid(List.of(at("07:00", UPCOMING)));
    }

    @Test
    void duplicateStartsAt() {
        assertInvalid(List.of(at("07:00", UPCOMING), at("08:00", ONGOING), at("08:00", ONGOING), at("17:00", COMPLETED)));
    }

    @Test
    void startsAtNotIncreasing() {
        // đúng thứ tự kind nhưng giờ đi lùi (ONGOING 09:00 rồi ONGOING 08:00)
        assertInvalid(List.of(at("07:00", UPCOMING), at("09:00", ONGOING), at("08:00", ONGOING), at("17:00", COMPLETED)));
    }

    @Test
    void kindGoesBackAfterCompleted() {
        // kết thúc rồi lại diễn ra tiếp
        assertInvalid(List.of(at("07:00", UPCOMING), at("08:00", ONGOING), at("17:00", COMPLETED), at("18:00", ONGOING)));
    }

    @Test
    void kindGoesBackToUpcoming() {
        assertInvalid(List.of(at("07:00", UPCOMING), at("08:00", ONGOING), at("09:00", UPCOMING), at("17:00", COMPLETED)));
    }

    @Test
    void completedBeforeOngoing() {
        assertInvalid(List.of(at("07:00", UPCOMING), at("08:00", COMPLETED), at("09:00", ONGOING)));
    }
}
