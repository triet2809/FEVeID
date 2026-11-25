package feveid.registration.service;

import feveid.registration.dto.GuestRegistrationRequest;
import feveid.registration.dto.RegistrationResponse;

public interface RegistrationService {
    // email = authentication.getName() của member đang đăng nhập
    RegistrationResponse register(String email, Long eventId);

    // Khách không có tài khoản, chỉ đăng ký được sự kiện PUBLIC
    RegistrationResponse registerGuest(Long eventId, GuestRegistrationRequest request);

    RegistrationResponse cancel(String email, Long registrationId);
}
