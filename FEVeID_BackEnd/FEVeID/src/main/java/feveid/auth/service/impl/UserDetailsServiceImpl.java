package feveid.auth.service.impl;

import feveid.account.entity.Account;
import feveid.account.repository.AccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private AccountRepository accountRepository;

    public UserDetailsServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account =
                accountRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException(
                                        "Không tìm thấy tài khoản với email: " + email
                                )
                        );


        Set<GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().name())
        );

        return new User(
                account.getEmail(),
                account.getPasswordHash(),
                authorities
        );
    }
}
