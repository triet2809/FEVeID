package feveid.event.service;

import feveid.event.dto.EventRequest;
import feveid.event.dto.EventResponse;
import feveid.event.dto.PhaseScheduleRequest;

import java.util.List;

public interface EventService {
    List<EventResponse> getEvents();
    EventResponse getEvent(Long id);
    EventResponse createEvent(EventRequest request);
    EventResponse updateEvent(Long id, EventRequest request);
    EventResponse replacePhases(Long id, PhaseScheduleRequest request);
    EventResponse startPhaseNow(Long id, Long phaseId);
    EventResponse publishEvent(Long id);
    EventResponse cancelEvent(Long id);
    void deleteEvent(Long id);
    EventResponse uploadBanner(Long id, byte[] data);
    EventResponse deleteBanner(Long id);
}
