package feveid.term;

import feveid.term.enums.TermSeason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Lịch kỳ cố định: test thuần Java, không cần Spring/DB
class TermSeasonTest {

    @ParameterizedTest
    @CsvSource({
            "2027-01-01, SPRING",
            "2027-04-30, SPRING",
            "2027-05-01, SUMMER",
            "2027-08-31, SUMMER",
            "2027-09-01, FALL",
            "2027-12-31, FALL",
    })
    void of_boundaryDays(LocalDate date, TermSeason expected) {
        assertThat(TermSeason.of(date)).isEqualTo(expected);
    }

    @Test
    void datesAndName() {
        assertThat(TermSeason.SPRING.startOf(2027)).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(TermSeason.SPRING.endOf(2027)).isEqualTo(LocalDate.of(2027, 4, 30));
        assertThat(TermSeason.SUMMER.endOf(2027)).isEqualTo(LocalDate.of(2027, 8, 31));
        assertThat(TermSeason.FALL.endOf(2027)).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(TermSeason.FALL.nameOf(2026)).isEqualTo("Fall 2026");
    }

    @Test
    void seasonsAreContiguous_noGapNoOverlap() {
        // Ngày cuối kỳ trước + 1 = ngày đầu kỳ sau, kể cả sang năm mới
        assertThat(TermSeason.SPRING.endOf(2027).plusDays(1)).isEqualTo(TermSeason.SUMMER.startOf(2027));
        assertThat(TermSeason.SUMMER.endOf(2027).plusDays(1)).isEqualTo(TermSeason.FALL.startOf(2027));
        assertThat(TermSeason.FALL.endOf(2027).plusDays(1)).isEqualTo(TermSeason.SPRING.startOf(2028));
    }
}
