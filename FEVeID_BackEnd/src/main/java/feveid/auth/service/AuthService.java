package feveid.auth.service;

import feveid.auth.dto.JwtAuthResponse;
import feveid.auth.dto.LoginRequest;

public interface AuthService    {
    JwtAuthResponse login(LoginRequest loginRequest);
}
