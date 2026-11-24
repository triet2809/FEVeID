package feveid.event.schedule;

import feveid.event.enums.EventPublication;
import feveid.event.enums.EventStatus;

import java.time.Instant;
import java.util.List;

// Tính trạng thái hiển thị của sự kiện (requirements.md mục 6.1). Hàm thuần: cùng input -> cùng output,
// không đọc DB, không gọi Instant.now() (now do service truyền vào từ Clock để test cố định được giờ)
public final class EventStatusCalculator {

    private EventStatusCalculator() {
    }

    /**
     * @param publication trạng thái HR/BCN bấm (DRAFT / PUBLISHED / CANCELLED)
     * @param phases      lịch đã qua PhaseScheduleValidator: xếp starts_at tăng dần, giai đoạn đầu là UPCOMING.
     *                    Có thể rỗng khi DRAFT (chưa lập lịch)
     * @param now         thời điểm cần tính
     */
    public static CurrentStatus compute(EventPublication publication, List<PhaseSlot> phases, Instant now) {
        // 1. DRAFT/CANCELLED do người bấm -> trả luôn, không cần nhìn lịch
        if(publication == EventPublication.DRAFT){
            return new CurrentStatus(EventStatus.DRAFT, null);
        }
        if (publication == EventPublication.CANCELLED){
            return new CurrentStatus(EventStatus.CANCELLED, null);
        }
        // 2. Tới đây chắc chắn PUBLISHED. Công bố bắt buộc có lịch, nên rỗng = bug của code, không phải lỗi người dùng
        if ((phases.isEmpty())) {
            throw new IllegalStateException("Sự kiện đã công bố nhưng chưa có lịch giai đoạn");
        }
        // 3. Mặc định là giai đoạn đầu (ca "chưa tới mốc nào"), rồi đi dần về sau
        PhaseSlot current = phases.get(0);
        for (PhaseSlot phase : phases) {
            if (phase.startsAt().isAfter(now)){
                break; // lịch tăng dần, gặp mốc ở tương lai thì mọi mốc sau cũng ở tương lai
            }
            current = phase;
        }
        // 4. PhaseKind và EventStatus trùng tên UPCOMING/ONGOING/COMPLETED
        return new CurrentStatus(EventStatus.valueOf(current.kind().name()), current.name());
    }
}
