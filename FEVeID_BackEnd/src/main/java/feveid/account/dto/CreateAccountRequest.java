package feveid.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Size(max = 100)
        String fullName,

        @Size(max = 20)
        String studentCode
) {
    public CreateAccountRequest {
        if (studentCode != null) {
            studentCode = studentCode.trim();
            if (studentCode.isEmpty()) {
                studentCode = null;
            }
        }

    }
}
