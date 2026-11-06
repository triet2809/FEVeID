package feveid.member;

import feveid.TestcontainersConfiguration;
import feveid.account.entity.Account;
import feveid.account.repository.AccountRepository;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class MemberRepositoryTest {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    MemberRepository memberRepository;

    private Account account;
    @BeforeEach
    void setUp(){
        memberRepository.deleteAll();
        accountRepository.deleteAll();
        account = accountRepository.save(Account.builder()
                .email("member@feveid.test")
                .passwordHash("not-used-here")
                .build());
    }

    @Test
    void save_onlyRequiredFields_appliesDefault(){
        Member member = new Member();
        member.setAccount(account);
        member.setFullName("Nguyen Van A");

        Member saved = memberRepository.save(member);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMemberStatus()).isEqualTo(MemberStatus.CTV);
        assertThat(saved.getJoinedAt()).isEqualTo(LocalDate.now());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByAccountId_returnsMember(){
        saveMember("Nguyen Van A", null);
        Optional<Member> found = memberRepository.findByAccountId(account.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Nguyen Van A");
        // So sánh qua getId(): proxy LAZY trả id mà không cần query, không bị LazyInitializationException
        assertThat(found.get().getAccount().getId()).isEqualTo(account.getId());
    }
    @Test
    void existsByStudentCode_trueOnlyForExistingCode() {
        saveMember("Nguyễn Văn A", "SE123456");

        assertThat(memberRepository.existsByStudentCode("SE123456")).isTrue();
        assertThat(memberRepository.existsByStudentCode("SE999999")).isFalse();
    }

    private Member saveMember(String fullName, String studentCode) {
        Member member = new Member();
        member.setAccount(account);
        member.setFullName(fullName);
        member.setStudentCode(studentCode);
        return memberRepository.save(member);
    }
}
