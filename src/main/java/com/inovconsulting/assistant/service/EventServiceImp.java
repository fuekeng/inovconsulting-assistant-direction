package com.inovconsulting.assistant.service;

import lombok.AllArgsConstructor;
import com.inovconsulting.assistant.dto.EventDto;
import com.inovconsulting.assistant.models.Event;
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
    public EventDto createEvent(EventDto eventDto) {
        Event savedEvent = EventMapper.mapToEvent(eventDto);
        savedEvent = eventRepository.save(savedEvent);
        return EventMapper.mapToEventDto(savedEvent);
    }

    @Override
    public EventDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        return EventMapper.mapToEventDto(event);

    }

    @Override
    public List<EventDto> getAllEvent() {
        List<Event> events = eventRepository.findAll();
        return events.stream().map(EventMapper::mapToEventDto).toList();
    }

    @Override
    public EventDto updateEvent(Long eventId, EventDto eventDto) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        event.setTitle(eventDto.getTitle());
        event.setDate(eventDto.getDate());
        event.setTime(eventDto.getTime());
        event.setParticipants(eventDto.getParticipants());
        event.setNotes(eventDto.getNotes());


        Event eventUpdated = eventRepository.save(event);

        return EventMapper.mapToEventDto(eventUpdated);
    }

    @Override
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(()-> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        eventRepository.delete(event);
    }

}
