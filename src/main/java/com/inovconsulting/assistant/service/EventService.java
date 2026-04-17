package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.dto.EventDto;

import java.util.List;

public interface EventService {
    EventDto createEvent(EventDto eventDto);

    EventDto getEventById(Long eventId);

    List<EventDto> getAllEvent();

    EventDto updateEvent(Long eventId, EventDto eventDto);

    void deleteEvent(Long eventId);

}
