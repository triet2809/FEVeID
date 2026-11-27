package feveid.term.enums;

import java.time.LocalDate;
import java.time.YearMonth;

// Lịch kỳ cố định (chốt 27/11/2026, requirements.md 11.2): 3 kỳ nối liền nhau, không có khoảng trống
public enum TermSeason {
    SPRING("Spring", 1, 4),  // 01/01 - 30/04
    SUMMER("Summer", 5, 8),  // 01/05 - 31/08
    FALL("Fall", 9, 12);     // 01/09 - 31/12

    private final String label;
    private final int firstMonth;
    private final int lastMonth;

    TermSeason(String label, int firstMonth, int lastMonth) {
        this.label = label;
        this.firstMonth = firstMonth;
        this.lastMonth = lastMonth;
    }

    public LocalDate startOf(int year) {
        return LocalDate.of(year, firstMonth, 1);
    }

    // atEndOfMonth: ngày cuối tháng (30/04, 31/08, 31/12), không phải tự nhớ tháng nào 30 hay 31 ngày
    public LocalDate endOf(int year) {
        return YearMonth.of(year, lastMonth).atEndOfMonth();
    }

    // "Spring 2027"
    public String nameOf(int year) {
        return label + " " + year;
    }

    // Ngày này thuộc mùa nào. Kỳ nằm trọn trong một năm nên năm của kỳ = date.getYear()
    public static TermSeason of(LocalDate date) {
        int month = date.getMonthValue();
        for (TermSeason season : values()) {
            if (month >= season.firstMonth && month <= season.lastMonth) {
                return season;
            }
        }
        throw new IllegalStateException("Tháng không thuộc kỳ nào: " + month); // không xảy ra: 3 kỳ phủ đủ 12 tháng
    }
}
