package feveid.term.dto;

import jakarta.validation.constraints.NotNull;

public record ShopStatusRequest(
        @NotNull
        Boolean open
) {
}
