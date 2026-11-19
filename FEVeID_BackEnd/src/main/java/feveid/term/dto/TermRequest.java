package feveid.term.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TermRequest(
        @Size(max = 100)
        @NotBlank
        String name,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {
    public TermRequest {
        if (name != null) {
            name = name.trim();
        }
    }

    // Kiểm tra chéo 2 field: @NotNull chỉ nhìn từng field riêng lẻ
    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
    public boolean isDateRangeValid() {
        //Thiếu ngày thì để @NotNull báo lỗi, ở đây bỏ qua (tránh NullPointerException)
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
