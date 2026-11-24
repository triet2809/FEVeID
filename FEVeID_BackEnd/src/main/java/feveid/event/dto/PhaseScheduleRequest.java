package feveid.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Cả lịch giai đoạn, gửi lên một lần (thay toàn bộ lịch cũ). Bọc trong object để sau này thêm field không phá API
public record PhaseScheduleRequest(
        // @Valid trên kiểu phần tử: kiểm tra @NotBlank/@NotNull bên trong từng PhaseRequest
        @NotNull
        @Size(max = 20, message = "Tối đa 20 giai đoạn")
        List<@Valid @NotNull PhaseRequest> phases
) {
}
