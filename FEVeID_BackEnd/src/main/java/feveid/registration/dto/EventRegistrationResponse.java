package feveid.registration.dto;

import feveid.member.entity.Member;
import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Một dòng trong danh sách đăng ký của sự kiện, cho HR/BCN (requirements.md 6, 12.3: tìm theo tên/MSSV khi check-in tay).
// Member và khách hiện chung một dạng: thông tin member lấy từ hồ sơ, của khách lấy từ lúc đăng ký
public record EventRegistrationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RegistrationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        Instant cancelledAt,
        // true = thành viên CLB, false = khách
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean member,
        // null với khách
        Long memberId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        String phone,
        // Member: MSSV trong hồ sơ; khách: MSSV hoặc tên trường đã nhập
        String studentCodeOrSchool
) {
    public static EventRegistrationResponse from(Registration registration) {
        Member member = registration.getMember();
        if (member != null) {
            return new EventRegistrationResponse(
                    registration.getId(), registration.getStatus(), registration.getCreatedAt(),
                    registration.getCancelledAt(), true, member.getId(),
                    member.getFullName(), member.getAccount().getEmail(), member.getPhone(), member.getStudentCode());
        }
        return new EventRegistrationResponse(
                registration.getId(), registration.getStatus(), registration.getCreatedAt(),
                registration.getCancelledAt(), false, null,
                registration.getGuestName(), registration.getGuestEmail(), registration.getGuestPhone(),
                registration.getGuestSchool());
    }
}
