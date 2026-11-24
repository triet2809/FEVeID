package feveid.event.service;

import feveid.event.dto.PublicEventResponse;
import feveid.event.enums.EventStatus;

import java.util.List;

public interface PublicEventService {
    /**
     * @param signedIn true = thành viên đã đăng nhập (thấy cả INTERNAL), false = khách
     * @param status   lọc theo trạng thái, null = tất cả
     * @param keyword  tìm trong tên sự kiện (không phân biệt hoa thường), null/rỗng = không lọc
     */
    List<PublicEventResponse> getEvents(boolean signedIn, EventStatus status, String keyword);

    PublicEventResponse getEvent(Long id, boolean signedIn);
}
