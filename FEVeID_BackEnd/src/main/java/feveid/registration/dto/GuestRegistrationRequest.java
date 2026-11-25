package feveid.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Khách đăng ký bằng họ tên, email, số điện thoại, MSSV/trường (requirements.md 12.2)
public record GuestRegistrationRequest(
        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        // Số Việt Nam: 0xxxxxxxxx hoặc +84xxxxxxxxx (9–10 chữ số sau đầu số)
        @NotBlank
        @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotBlank
        @Size(max = 200)
        String school
) {
    public GuestRegistrationRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim();
        phone = phone == null ? null : phone.replace(" ", "");  // "0912 345 678" -> "0912345678"
        school = school == null ? null : school.trim();
    }
}
