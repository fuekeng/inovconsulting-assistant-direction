package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.dto.EventRequest;
import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest eventRequest);

    EventResponse getEventById(Long eventId);

    List<EventResponse> getAllEvent();

    EventResponse updateEvent(Long eventId, EventRequest eventRequest);

    void deleteEvent(Long eventId);

}
