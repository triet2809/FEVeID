package feveid.event.service;

import feveid.event.dto.EventStaffRequest;
import feveid.event.dto.EventStaffResponse;

import java.util.List;

public interface EventStaffService {
    List<EventStaffResponse> getStaff(Long eventId);

    // Thay cả team. Bị khóa khi sự kiện đã hủy hoặc đã chốt
    List<EventStaffResponse> replaceStaff(Long eventId, EventStaffRequest request);
}
