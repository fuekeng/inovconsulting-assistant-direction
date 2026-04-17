package com.inovconsulting.assistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import com.inovconsulting.assistant.dto.EventDto;
import com.inovconsulting.assistant.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import  java.util.List;
import java.util.logging.Logger;

@Tag(name = "Event Controller", description = "Gestion des événements de l'agenda")
@AllArgsConstructor
@RestController
@RequestMapping("/agenda")
public class EventController {
    private static final Logger logger = Logger.getLogger(EventController.class.getName());
    private EventService eventService;

    @Operation(summary = "Créer un nouvel événement", description = "Build Add Event REST API")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EventDto> createEvent(@RequestBody EventDto eventDto){
        logger.info("Starting saving event controller...");
        EventDto savedEvent = eventService.createEvent(eventDto);
        logger.info("Endind saving event controller...");
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un événement par son ID", description = "Get event by Id")
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable("id") Long eventId){
        EventDto eventDto = eventService.getEventById(eventId);
        return ResponseEntity.ok(eventDto);
    }

    @Operation(summary = "Récupérer la liste de tous les événements", description = "Get a list on events")
    @GetMapping
    public ResponseEntity<List<EventDto>> getAllEvents(){
        List<EventDto> eventDtos = eventService.getAllEvent();
        return ResponseEntity.ok(eventDtos);
    }

    @Operation(summary = "Mettre à jour un événement", description = "Update Event REST API")
    @PatchMapping("{id}")
    public ResponseEntity<EventDto> updateEvent(@PathVariable("id") Long eventId,
                                                @RequestBody EventDto updatedEvent) {
        EventDto eventDto = eventService.updateEvent(eventId, updatedEvent);
        return ResponseEntity.ok(eventDto);
    }

    @Operation(summary = "Supprimer un événement", description = "Delete Mapping")
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable("id") Long eventId){
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok("event deleted succefully");
    }

}
