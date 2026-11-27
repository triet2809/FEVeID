package feveid.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Thay cả team (giống PUT lịch giai đoạn): gửi danh sách đầy đủ, ai không có trong list là bị gỡ. List rỗng = xóa hết.
// @Valid: kiểm tra luôn từng phần tử bên trong (memberId, role không được null)
public record EventStaffRequest(
        @NotNull
        @Size(max = 200)
        List<@NotNull @Valid StaffAssignmentRequest> staff
) {
}
