package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.dto.EventRequest;


import java.time.LocalDate;
import java.util.List;

public interface AgendaService {
    EventResponse createEvent(EventRequest eventRequest);


    List<EventResponse> getEvents(LocalDate date, String range);

    List<EventResponse> getEventsByDate(LocalDate date);
    List<EventResponse> getEventsByRange(LocalDate start, LocalDate end);
    EventResponse updateEvent(Long eventId, EventRequest eventRequest);

    void deleteEvent(Long eventId);

}
