package feveid.term.dto;

import feveid.term.enums.TermSeason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Tạo kỳ: chỉ chọn mùa + năm, tên và ngày do hệ thống tính theo lịch cố định (TermSeason)
public record TermRequest(
        @NotNull
        TermSeason season,

        // Integer (không phải int) để thiếu field thì @NotNull báo 400, không âm thầm thành 0
        @NotNull
        @Min(2020)
        @Max(2100)
        Integer year
) {
}
