package feveid.auth.service;

import feveid.auth.dto.LoginRequest;

public interface AuthService    {
    // Trả về JWT; controller quyết định gửi nó cho client bằng cách nào (hiện tại: cookie)
    String login(LoginRequest loginRequest);
}
