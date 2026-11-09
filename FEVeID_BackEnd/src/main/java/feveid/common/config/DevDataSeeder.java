package feveid.common.config;

import feveid.account.entity.Account;
import feveid.account.enums.AccountRole;
import feveid.account.repository.AccountRepository;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev") // chỉ chạy khi bật profile dev, không bao giờ chạy trên production
public class DevDataSeeder {

    @Bean
    CommandLineRunner seedHrAccount(AccountRepository accountRepository,
                                    MemberRepository memberRepository,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${DEV_HR_EMAIL}") String email,
                                    @Value("${DEV_HR_PASSWORD}") String password) {
        return args -> {
            if (accountRepository.existsByEmail(email)) {
                return;
            }
            Account hr = accountRepository.save(Account.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(AccountRole.HR)
                    .build());
            memberRepository.save(Member.builder()
                    .account(hr)
                    .fullName("HR (dev)")
                    .build());
        };
    }
}