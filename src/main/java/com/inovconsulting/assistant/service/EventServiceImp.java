package com.inovconsulting.assistant.service;

import lombok.AllArgsConstructor;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.entity.Event;
import com.inovconsulting.assistant.exception.ResourceNotFoundException;
import com.inovconsulting.assistant.mapper.EventMapper;
import com.inovconsulting.assistant.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EventServiceImp implements EventService {
    private static final String EVENT_NOT_FOUND_MSG = "Event is  not exist with id=";
    private EventRepository eventRepository;

    @Override
    public EventResponse createEvent(EventRequest eventRequest) {
        Event savedEvent = EventMapper.mapToEvent(eventRequest);
        savedEvent = eventRepository.save(savedEvent);
        return EventMapper.mapToEventResponseDto(savedEvent);
    }

    @Override
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        return EventMapper.mapToEventResponseDto(event);

    }

    @Override
    public List<EventResponse> getAllEvent() {
        List<Event> events = eventRepository.findAll();
        return events.stream().map(EventMapper::mapToEventResponseDto).toList();
    }

    @Override
    public EventResponse updateEvent(Long eventId, EventRequest eventRequest) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        event.setTitle(eventRequest.getTitle());
        event.setDate(eventRequest.getDate());
        event.setTime(eventRequest.getTime());
        event.setParticipants(eventRequest.getParticipants());
        event.setNotes(eventRequest.getNotes());


        Event eventUpdated = eventRepository.save(event);

        return EventMapper.mapToEventResponseDto(eventUpdated);
    }

    @Override
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        eventRepository.delete(event);
    }

}