package feveid.event.dto;

import feveid.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// Màn "Chốt sự kiện". Chưa chốt: bản xem trước (team mặc định, người đã check-in, điểm dự kiến).
// Đã chốt: điểm thật đã ghi trong sổ cái. GET preview và POST finalize trả cùng dạng này
public record FinalizationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long eventId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean finalized,
        Instant finalizedAt,
        String finalizedByName,
        // Chưa chốt: bấm chốt được ngay không. false thì xem blockedReason
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean finalizable,
        String blockedReason,
        // Kỳ nhận điểm = kỳ chứa ngày kết thúc sự kiện. null khi sự kiện chưa có lịch
        String termName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int organizePoints,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int supportPoints,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int attendPoints,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Person> organizers,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Person> supporters,
        // Member đã check-in, trừ người trong team (mỗi người một vai trò)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Person> attendees,
        // Khách đã check-in: có mặt nhưng không được điểm
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long guestAttendeeCount,
        // Chưa chốt: số đăng ký CONFIRMED sẽ thành NO_SHOW. Đã chốt: số NO_SHOW
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long noShowCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPoints
) {
    public record Person(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long memberId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
            String studentCode
    ) {
        public static Person from(Member member) {
            return new Person(member.getId(), member.getFullName(), member.getStudentCode());
        }
    }
}
