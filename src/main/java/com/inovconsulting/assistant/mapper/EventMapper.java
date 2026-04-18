package com.inovconsulting.assistant.mapper;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.entity.Event;

public class EventMapper {
    // Empêche l’instanciation
    private EventMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static EventResponse mapToEventResponseDto(Event event){
       return  new EventResponse(event.getId(), event.getTitle(), event.getDate(), event.getTime(), event.getParticipants(), event.getNotes());

    }

    public static Event mapToEvent(EventRequest eventDto){
        return new Event(null, eventDto.getTitle(), eventDto.getDate(), eventDto.getTime(), eventDto.getParticipants(), eventDto.getNotes());
    }
}
