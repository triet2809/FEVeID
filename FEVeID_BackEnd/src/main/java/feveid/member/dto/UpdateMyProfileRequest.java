package feveid.member.dto;

import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 20) // Check số VN không dùng size của số được mà dùng @Patern
        String phone,

        @Size(max = 500)
        String avatarUrl
        ) {
}
