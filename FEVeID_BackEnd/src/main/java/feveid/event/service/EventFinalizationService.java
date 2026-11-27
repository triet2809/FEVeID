package feveid.event.service;

import feveid.event.dto.FinalizationResponse;
import feveid.event.dto.FinalizeEventRequest;

public interface EventFinalizationService {
    // Xem trước màn chốt, hoặc kết quả đã chốt
    FinalizationResponse getFinalization(Long eventId);

    // Chốt sự kiện: cộng điểm hàng loạt, CONFIRMED -> NO_SHOW, khóa sự kiện. Chỉ 1 lần
    FinalizationResponse finalizeEvent(String staffEmail, Long eventId, FinalizeEventRequest request);
}
