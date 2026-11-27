package feveid.point.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// HR/BCN cộng/trừ điểm tay (requirements 11.4). Sửa sai bằng giao dịch mới, không sửa giao dịch cũ (11.5)
public record PointAdjustmentRequest(
        // Bỏ trống = kỳ hiện tại. Gửi id để sửa điểm của kỳ đã qua (khiếu nại trước buổi tổng kết)
        Long termId,

        @NotNull(message = "Nhập số điểm")
        @Min(value = -1000, message = "Mỗi lần điều chỉnh tối đa 1000 điểm")
        @Max(value = 1000, message = "Mỗi lần điều chỉnh tối đa 1000 điểm")
        Integer amount,

        @NotBlank(message = "Bắt buộc ghi lý do điều chỉnh")
        @Size(max = 500)
        String reason
) {
    public PointAdjustmentRequest {
        reason = reason == null ? null : reason.trim();
    }

    // Validation chéo: trả true khi amount null (để @NotNull báo lỗi riêng)
    @AssertTrue(message = "Số điểm phải khác 0")
    public boolean isAmountNonZero() {
        return amount == null || amount != 0;
    }
}
