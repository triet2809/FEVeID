package feveid.registration.enums;

import java.util.Set;

// requirements.md 12.2: PENDING_PAYMENT (chỉ sự kiện có phí) -> CONFIRMED -> CHECKED_IN,
// hoặc CANCELLED / EXPIRED / NO_SHOW. Bài 11 (sự kiện miễn phí) chỉ dùng CONFIRMED và CANCELLED
public enum RegistrationStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CHECKED_IN,
    CANCELLED,
    EXPIRED,
    NO_SHOW;

    // Đang giữ chỗ: tính vào số chỗ đã dùng và không cho đăng ký trùng.
    // Phải khớp với điều kiện WHERE của 2 unique index trong V10
    public static final Set<RegistrationStatus> ACTIVE = Set.of(PENDING_PAYMENT, CONFIRMED, CHECKED_IN);
}
