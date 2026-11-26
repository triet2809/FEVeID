package feveid.registration.checkin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// Unit test thuần như EventStatusCalculatorTest: không bật Spring, không cần Docker
class CheckInCodesTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // Secret cố định (32 byte giá trị 1, 2, 3...) để mỗi lần chạy test ra cùng mã
    private static final byte[] SECRET = bytes(1);
    private static final byte[] OTHER_SECRET = bytes(101);

    // 08:00:00 tròn phút nên là giây đầu tiên của một ô 30 giây
    private static final Instant T = LocalDateTime.parse("2026-12-05T08:00:00").atZone(VN).toInstant();

    private static byte[] bytes(int start) {
        byte[] result = new byte[32];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (start + i);
        }
        return result;
    }

    @Test
    void sameWindowGivesSameCode() {
        assertThat(CheckInCodes.codeAt(SECRET, T.plusSeconds(29))).isEqualTo(CheckInCodes.codeAt(SECRET, T));
    }

    @Test
    void nextWindowGivesDifferentCode() {
        assertThat(CheckInCodes.codeAt(SECRET, T.plusSeconds(30))).isNotEqualTo(CheckInCodes.codeAt(SECRET, T));
    }

    @Test
    void codeIs16UrlSafeCharacters() {
        // Chỉ A-Z a-z 0-9 - _ : đặt thẳng vào link ?code=... không cần escape
        assertThat(CheckInCodes.codeAt(SECRET, T)).matches("[A-Za-z0-9_-]{16}");
    }

    // Mã chiếu lúc T, member gửi lên lúc T + giây. Còn sống ở ô hiện tại và ô kế tiếp (30–60 giây)
    @ParameterizedTest(name = "gửi sau {0} giây -> {1}")
    @CsvSource({
            "0, true",
            "29, true",
            "30, true",   // đã sang ô mới, mã là của ô trước -> vẫn nhận
            "59, true",
            "60, false",  // cách 2 ô -> hết hạn (ảnh chụp gửi cho bạn ở nhà)
            "300, false",
            "-1, false"   // mã "của tương lai" so với giờ server -> không có thật
    })
    void codeExpiresAfterOneExtraWindow(long secondsLater, boolean accepted) {
        String code = CheckInCodes.codeAt(SECRET, T);

        assertThat(CheckInCodes.verify(SECRET, code, T.plusSeconds(secondsLater))).isEqualTo(accepted);
    }

    @Test
    void codeOfAnotherEventIsRejected() {
        String otherEventCode = CheckInCodes.codeAt(OTHER_SECRET, T);

        assertThat(CheckInCodes.verify(SECRET, otherEventCode, T)).isFalse();
    }

    @Test
    void changingOneCharacterIsRejected() {
        char[] chars = CheckInCodes.codeAt(SECRET, T).toCharArray();
        chars[chars.length - 1] = chars[chars.length - 1] == 'A' ? 'B' : 'A';

        assertThat(CheckInCodes.verify(SECRET, new String(chars), T)).isFalse();
    }

    @Test
    void missingOrEmptyCodeIsRejected() {
        assertThat(CheckInCodes.verify(SECRET, null, T)).isFalse();
        assertThat(CheckInCodes.verify(SECRET, "", T)).isFalse();
    }

    @ParameterizedTest(name = "lúc T + {0} giây -> hết hạn lúc T + 30")
    @CsvSource({"0", "10", "29"})
    void expiresAtStartOfNextWindow(long secondsLater) {
        assertThat(CheckInCodes.expiresAt(T.plusSeconds(secondsLater))).isEqualTo(T.plusSeconds(30));
    }
}
