package feveid.department;

import feveid.department.enums.DepartmentPosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentPositionTest {
    @ParameterizedTest(name = "{0}.isManager() = {1}")
    @CsvSource({
            "LEADER,      true",
            "VICE_LEADER, true",
            "MEMBER,      false",
    })
    void isManager(DepartmentPosition position, boolean expected) {
        assertThat(position.isManager()).isEqualTo(expected);
    }
}