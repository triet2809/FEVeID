package feveid.account.service.impl;

import feveid.account.dto.AccountResponse;
import feveid.account.dto.CreateAccountRequest;
import feveid.account.entity.Account;
import feveid.account.enums.AccountStatus;
import feveid.account.repository.AccountRepository;
import feveid.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;




@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {
        if(accountRepository.existsByEmail(request.email())){
            throw new RuntimeException("Email already exists");
        }
        Account account = Account.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getEmail(),
                savedAccount.getAccountStatus(),
                savedAccount.getRole()
        );
    }




    @Override
    public AccountResponse getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getAccountStatus(),
                account.getRole()
        );
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getEmail(),
                        account.getAccountStatus(),
                        account.getRole()
                ))
                .toList();
    }
}
