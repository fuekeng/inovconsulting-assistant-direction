package com.inovconsulting.assistant.mapper;

import com.inovconsulting.assistant.dto.EventDto;
import com.inovconsulting.assistant.models.Event;

public class EventMapper {
    // Empêche l’instanciation
    private EventMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static EventDto mapToEventDto(Event event){
       return  new EventDto(event.getId(), event.getTitle(), event.getDate(), event.getTime(), event.getParticipants(), event.getNotes());

    }

    public static Event mapToEvent(EventDto eventDto){
        return new Event(eventDto.getId(), eventDto.getTitle(), eventDto.getDate(), eventDto.getTime(), eventDto.getParticipants(), eventDto.getNotes());
    }
}
