package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.entity.Event;
import com.inovconsulting.assistant.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de gestion des événements agenda.
 * Couche métier appelée à la fois par les controllers REST et par les outils de l'agent IA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgendaServiceImp implements  AgendaService{

    private final EventRepository eventRepository;

    // ─────────────────────────────────────────────────────────
    // Lecture
    // ─────────────────────────────────────────────────────────

    /**
     * Retourne tous les événements, ou filtrés par date ou par semaine.
     *
     * @param date   filtre exact sur une date (prioritaire sur range)
     * @param range  "week" pour retourner les 7 prochains jours à partir d'aujourd'hui
     */
    public List<EventResponse> getEvents(LocalDate date, String range) {
        List<Event> events;

        if (date != null) {
            log.info("AgendaService — getEvents par date : {}", date);
            events = eventRepository.findByDateOrderByTimeAsc(date);

        } else if ("week".equalsIgnoreCase(range)) {
            LocalDate start = LocalDate.now();
            LocalDate end   = start.plusDays(6);
            log.info("AgendaService — getEvents semaine : {} → {}", start, end);
            events = eventRepository.findByDateBetweenOrderByDateAscTimeAsc(start, end);

        } else {
            log.info("AgendaService — getEvents tous les événements");
            events = eventRepository.findAll();
        }

        return events.stream().map(this::toResponse).toList();
    }

    /**
     * Retourne les événements d'une date précise (utilisé par les outils de l'agent).
     */
    public List<EventResponse> getEventsByDate(LocalDate date) {
        return eventRepository.findByDateOrderByTimeAsc(date)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Retourne les événements entre deux dates (utilisé par les outils de l'agent).
     */
    public List<EventResponse> getEventsByRange(LocalDate start, LocalDate end) {
        return eventRepository.findByDateBetweenOrderByDateAscTimeAsc(start, end)
                .stream().map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // Écriture
    // ─────────────────────────────────────────────────────────

    /**
     * Crée un nouvel événement. Retourne l'événement persisté avec son id généré.
     */
    public EventResponse createEvent(EventRequest request) {
        validateCreateRequest(request);

        Event event = Event.builder()
                .title(request.getTitle())
                .date(request.getDate())
                .time(request.getTime())
                .participants(request.getParticipants())
                .notes(request.getNotes())
                .build();

        Event saved = eventRepository.save(event);
        log.info("AgendaService — événement créé : id={}, title={}", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    /**
     * Modifie partiellement un événement existant (PATCH sémantique).
     * Seuls les champs non-null du request sont mis à jour.
     */
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findByIdOrThrow(id);

        if (request.getTitle()        != null) event.setTitle(request.getTitle());
        if (request.getDate()         != null) event.setDate(request.getDate());
        if (request.getTime()         != null) event.setTime(request.getTime());
        if (request.getParticipants() != null) event.setParticipants(request.getParticipants());
        if (request.getNotes()        != null) event.setNotes(request.getNotes());

        Event saved = eventRepository.save(event);
        log.info("AgendaService — événement mis à jour : id={}", id);
        return toResponse(saved);
    }

    /**
     * Supprime un événement par son identifiant.
     * Lève 404 si l'événement n'existe pas.
     */
    public void deleteEvent(Long id) {
        findByIdOrThrow(id); // vérifie l'existence avant suppression
        eventRepository.deleteById(id);
        log.info("AgendaService — événement supprimé : id={}", id);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────

    private Event findByIdOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement introuvable : id=" + id));
    }

    private void validateCreateRequest(EventRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le titre est obligatoire");
        }
        if (request.getDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date est obligatoire");
        }
        if (request.getTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure est obligatoire");
        }
    }

    /** Mappe une entité Event vers son DTO de réponse. */
    public EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .date(event.getDate())
                .time(event.getTime())
                .participants(event.getParticipants())
                .notes(event.getNotes())
                .build();
    }
}
