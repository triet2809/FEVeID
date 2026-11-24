package feveid.registration.service;

import feveid.registration.dto.RegistrationResponse;

public interface RegistrationService {
    // email = authentication.getName() của member đang đăng nhập
    RegistrationResponse register(String email, Long eventId);

    RegistrationResponse cancel(String email, Long registrationId);
}
