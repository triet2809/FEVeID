package feveid.account.dto;

import feveid.account.enums.AccountRole;
import feveid.account.enums.AccountStatus;

public record AccountResponse(
        Long id,
        String email,
        AccountStatus accountStatus,
        AccountRole accountRole
) {
}
