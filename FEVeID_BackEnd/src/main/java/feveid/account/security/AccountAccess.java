package feveid.account.security;

import feveid.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//Dùng trong @PreAuthorize: kiểm tra người đang đăng nhập có phải chủ tài khoản không
@Component("accountAccess")
@RequiredArgsConstructor
public class AccountAccess {
    private final AccountRepository accountRepository;
    public boolean isSelf(Long accountId, String email) {
        return accountRepository.findById(accountId)
                .map(account -> account.getEmail().equals(email))
                .orElse(false);
    }
}
