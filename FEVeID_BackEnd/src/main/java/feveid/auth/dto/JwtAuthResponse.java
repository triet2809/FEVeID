package feveid.auth.dto;

public record JwtAuthResponse(
        String accessToken,
        String tokenType //luôn là "Bearer
) {
}
