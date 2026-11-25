package feveid.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

public final class Authentications {

    private Authentications() {
    }

    // Route permitAll: chưa đăng nhập thì Spring Security đặt AnonymousAuthenticationToken (thường không phải null)
    public static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
