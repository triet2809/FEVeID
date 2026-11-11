package feveid.member;

import feveid.member.enums.MemberStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberStatusTest {
    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "CTV,          CTV,          false",
            "CTV,          ACTIVE,       true",
            "CTV,          INACTIVE,     false",
            "CTV,          DISQUALIFIED, true",

            "ACTIVE,       CTV,          false",
            "ACTIVE,       ACTIVE,       false",
            "ACTIVE,       INACTIVE,     true",
            "ACTIVE,       DISQUALIFIED, true",

            "INACTIVE,     CTV,          false",
            "INACTIVE,     ACTIVE,       true",
            "INACTIVE,     INACTIVE,     false",
            "INACTIVE,     DISQUALIFIED, true",

            "DISQUALIFIED, CTV,          false",
            "DISQUALIFIED, ACTIVE,       false",
            "DISQUALIFIED, INACTIVE,     false",
            "DISQUALIFIED, DISQUALIFIED, false",
    })
    void canTransitionTo(MemberStatus from, MemberStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}
