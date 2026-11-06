package feveid.account.service.impl;

import feveid.account.dto.AccountResponse;
import feveid.account.dto.CreateAccountRequest;
import feveid.account.entity.Account;
import feveid.account.enums.AccountStatus;
import feveid.account.repository.AccountRepository;
import feveid.account.service.AccountService;
import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email đã được sử dụng");
        }
        if (request.studentCode() != null
                && memberRepository.existsByStudentCode(request.studentCode())) {
            throw new ConflictException("Mã số sinh viên đã được sử dụng");
        }
        Account savedAccount = accountRepository.save(Account.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        memberRepository.save(Member.builder()
                .account(savedAccount)
                .fullName(request.fullName())
                .studentCode(request.studentCode())
                .build());

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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));

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
