package feveid.registration.service;

import feveid.registration.dto.CheckInCodeResponse;
import feveid.registration.dto.CheckInResponse;
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

    // ---------- Check-in (requirements 12.3, Bài 12) ----------

    // Màn hình chiếu QR của HR/BCN: mã hiện tại + số người đã check-in. Chỉ khi sự kiện ONGOING
    CheckInCodeResponse getCheckInCode(Long eventId);

    // Member tự quét QR. Chưa đăng ký mà còn chỗ thì tự tạo đăng ký CHECKED_IN (chốt 25/11/2026)
    CheckInResponse checkIn(String email, Long eventId, String code);

    // HR/BCN check-in tay một đăng ký (member hoặc khách) của sự kiện
    EventRegistrationResponse checkInManually(String staffEmail, Long eventId, Long registrationId);
}
