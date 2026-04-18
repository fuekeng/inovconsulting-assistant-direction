package com.inovconsulting.assistant;

import com.inovconsulting.assistant.service.AgendaServiceImp;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.model.entity.Event;
import com.inovconsulting.assistant.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de la couche service AgendaService.
 * Utilise Mockito pour isoler le repository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgendaService — Tests unitaires")
class AgendaServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private AgendaServiceImp agendaService;

    private Event sampleEvent;
    private EventRequest validRequest;
    private final LocalDate today = LocalDate.now();
    private final LocalDate tomorrow = today.plusDays(1);

    @BeforeEach
    void setUp() {
        sampleEvent = Event.builder()
                .id(1L)
                .title("Comité de direction")
                .date(tomorrow)
                .time(LocalTime.of(9, 0))
                .participants("DG, DAF, DSI")
                .notes("Budget Q2 à valider")
                .build();

        validRequest = new EventRequest(
                "Réunion Test",
                tomorrow,
                LocalTime.of(10, 0),
                "Équipe A",
                "Ordre du jour"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getEventsByDate
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getEventsByDate — retourne les événements du jour correct")
    void getEventsByDate_returnsEventsForDate() {
        when(eventRepository.findByDateOrderByTimeAsc(tomorrow))
                .thenReturn(List.of(sampleEvent));

        List<EventResponse> result = agendaService.getEventsByDate(tomorrow);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Comité de direction");
        assertThat(result.get(0).getDate()).isEqualTo(tomorrow);
        verify(eventRepository, times(1)).findByDateOrderByTimeAsc(tomorrow);
    }

    @Test
    @DisplayName("getEventsByDate — retourne liste vide si aucun événement")
    void getEventsByDate_returnsEmptyListWhenNoEvents() {
        when(eventRepository.findByDateOrderByTimeAsc(any())).thenReturn(List.of());

        List<EventResponse> result = agendaService.getEventsByDate(today);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createEvent — persiste et retourne l'événement avec id")
    void createEvent_persistsAndReturnsWithId() {
        Event saved = Event.builder()
                .id(99L)
                .title(validRequest.getTitle())
                .date(validRequest.getDate())
                .time(validRequest.getTime())
                .participants(validRequest.getParticipants())
                .notes(validRequest.getNotes())
                .build();

        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponse result = agendaService.createEvent(validRequest);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getTitle()).isEqualTo("Réunion Test");
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("createEvent — lève 400 si titre absent")
    void createEvent_throwsBadRequestWhenTitleMissing() {
        EventRequest noTitle = new EventRequest(null, tomorrow, LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> agendaService.createEvent(noTitle))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("titre");
    }

    @Test
    @DisplayName("createEvent — lève 400 si date absente")
    void createEvent_throwsBadRequestWhenDateMissing() {
        EventRequest noDate = new EventRequest("Test", null, LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> agendaService.createEvent(noDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("date");
    }

    @Test
    @DisplayName("createEvent — lève 400 si heure absente")
    void createEvent_throwsBadRequestWhenTimeMissing() {
        EventRequest noTime = new EventRequest("Test", tomorrow, null, null, null);

        assertThatThrownBy(() -> agendaService.createEvent(noTime))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("heure");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteEvent — supprime l'événement existant")
    void deleteEvent_deletesExistingEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

        assertThatCode(() -> agendaService.deleteEvent(1L)).doesNotThrowAnyException();
        verify(eventRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteEvent — lève 404 si événement inexistant")
    void deleteEvent_throws404WhenNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.deleteEvent(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateEvent (PATCH sémantique)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEvent — modifie uniquement les champs fournis")
    void updateEvent_patchSemantic() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EventRequest patch = new EventRequest();
        patch.setTitle("Nouveau titre");

        EventResponse result = agendaService.updateEvent(1L, patch);

        assertThat(result.getTitle()).isEqualTo("Nouveau titre");
        // Les autres champs ne doivent pas avoir changé
        assertThat(result.getDate()).isEqualTo(tomorrow);
        assertThat(result.getTime()).isEqualTo(LocalTime.of(9, 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getEvents (avec range=week)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getEvents avec range=week — interroge la plage de 7 jours")
    void getEvents_weekRange_callsCorrectRepository() {
        when(eventRepository.findByDateBetweenOrderByDateAscTimeAsc(any(), any()))
                .thenReturn(List.of(sampleEvent));

        List<EventResponse> result = agendaService.getEvents(null, "week");

        assertThat(result).hasSize(1);
        verify(eventRepository, times(1))
                .findByDateBetweenOrderByDateAscTimeAsc(today, today.plusDays(6));
    }
}