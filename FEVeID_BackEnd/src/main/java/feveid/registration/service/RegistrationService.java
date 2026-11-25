package feveid.registration.service;

import feveid.registration.dto.EventRegistrationResponse;
import feveid.registration.dto.GuestRegistrationRequest;
import feveid.registration.dto.MyRegistrationResponse;
import feveid.registration.dto.RegistrationResponse;

import java.util.List;

public interface RegistrationService {
    // email = authentication.getName() của member đang đăng nhập
    RegistrationResponse register(String email, Long eventId);

    // Khách không có tài khoản, chỉ đăng ký được sự kiện PUBLIC
    RegistrationResponse registerGuest(Long eventId, GuestRegistrationRequest request);

    RegistrationResponse cancel(String email, Long registrationId);

    // "Sự kiện của tôi", đăng ký mới nhất trước, gồm cả đăng ký đã hủy
    List<MyRegistrationResponse> getMyRegistrations(String email);

    // Cho HR/BCN: mọi đăng ký của một sự kiện (member + khách)
    List<EventRegistrationResponse> getEventRegistrations(Long eventId);
}
