package feveid.registration.dto;

import jakarta.validation.constraints.NotNull;

// Member đăng ký: không cần gửi thông tin cá nhân, backend lấy từ tài khoản đang đăng nhập
public record RegistrationRequest(
        @NotNull
        Long eventId
) {
}
