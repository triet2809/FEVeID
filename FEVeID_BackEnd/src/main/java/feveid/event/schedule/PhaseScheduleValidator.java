package feveid.event.schedule;

import feveid.common.exception.BadRequestException;
import feveid.event.enums.PhaseKind;

import java.util.List;

// Kiểm tra ràng buộc khi lưu lịch giai đoạn (requirements.md mục 6.1, "Ràng buộc khi lưu lịch").
// name/startsAt/kind khác null và name không rỗng đã được @Valid ở DTO kiểm tra trước, ở đây không cần kiểm lại
public final class PhaseScheduleValidator {

    private PhaseScheduleValidator() {
    }

    /**
     * Lịch hợp lệ thì không làm gì; sai thì throw BadRequestException với message tiếng Việt
     * (message trả thẳng cho người dùng nên viết dễ hiểu, vd "Giai đoạn đầu tiên phải là UPCOMING").
     *
     * @param phases lịch theo đúng thứ tự client gửi lên (KHÔNG tự sắp xếp lại)
     */
    public static void validate(List<PhaseSlot> phases) {
        // Luật 1
        if (phases.isEmpty()) {
            throw new BadRequestException("Lịch sự kiện phải có ít nhất một giai đoạn");
        }
        // Luật 2
        if (phases.get(0).kind() != PhaseKind.UPCOMING) {
            throw new BadRequestException("Giai đoạn đầu tiên phải là UPCOMING (sắp diễn ra)");
        }

        boolean hasOnGoing = false;
        // so từng cắp (trước,sau); bắt đầu từ i = 1 vì phase 0 không có "trước"
        for (int i = 1; i < phases.size(); i++) {
            PhaseSlot prev = phases.get(i - 1);
            PhaseSlot cur = phases.get(i);

            // Luật 3: !isAfter = sớm hơn hoặc bằng -> chặn cả giờ đi lùi lẫn giờ trùng
            if (!cur.startsAt().isAfter(prev.startsAt())) {
                throw new BadRequestException("Giai đoạn \"" + cur.name() + "\" phải bắt đầu sau giai đoạn \"" + prev.name() + "\"");
            }
            // Luật 4: ordinal UPCOMING=0, ONGOING=1, COMPLETED=2; bằng nhau thì được
            if (cur.kind().ordinal() < prev.kind().ordinal()) {
                throw new BadRequestException("Thứ tự giai đoạn phải là UPCOMING -> ONGOING -> COMPLETED, \"" + cur.name() + "\" bị đi lùi");
            }
            if (cur.kind() == PhaseKind.ONGOING) {
                hasOnGoing = true;
            }
        }
        // Luật 5
        if(!hasOnGoing) {
            throw new BadRequestException("Lịch phải có ít nhất một giai đoạn ONGOING (đang diễn ra)");
        }
        if(phases.getLast().kind() != PhaseKind.COMPLETED){
            throw new BadRequestException("Giai đoạn cuối cùng phải là COMPLETED (đã kết thúc)");
        }
    }
}
