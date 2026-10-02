package com.inovconsulting.assistant;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import com.inovconsulting.assistant.tools.CreateEventTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'outil create_event, notamment le parsing tolérant de l'heure
 * ("9h", "14h30", "09:00"...) vers un LocalTime.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateEventTool — Tests unitaires")
class CreateEventToolTest {

    @Mock
    private AgendaService agendaService;

    private final CreateEventTool tool = new CreateEventTool();

    @AfterEach
    void tearDown() {
        ToolContext.clear();
    }

    private Function<CreateEventTool.Request, EventResponse> function() {
        return tool.createEvent(agendaService);
    }

    @Test
    @DisplayName("parse l'heure au format 'Hh' (une chiffre) en HH:mm")
    void createEvent_parsesShortHourFormat() {
        when(agendaService.createEvent(any(EventRequest.class)))
                .thenAnswer(inv -> EventResponse.builder().id(1L).build());

        CreateEventTool.Request request = new CreateEventTool.Request(
                "Réunion", LocalDate.of(2026, 4, 18), "9h", "Équipe A", "Notes");

        function().apply(request);

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(agendaService).createEvent(captor.capture());
        assertThat(captor.getValue().getTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("parse l'heure au format 'HHhmm' en HH:mm")
    void createEvent_parsesHourWithMinutesFormat() {
        when(agendaService.createEvent(any(EventRequest.class)))
                .thenAnswer(inv -> EventResponse.builder().id(1L).build());

        CreateEventTool.Request request = new CreateEventTool.Request(
                "Réunion", LocalDate.of(2026, 4, 18), "14h30", null, null);

        function().apply(request);

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(agendaService).createEvent(captor.capture());
        assertThat(captor.getValue().getTime()).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    @DisplayName("parse l'heure déjà au format HH:mm sans modification")
    void createEvent_parsesAlreadyFormattedTime() {
        when(agendaService.createEvent(any(EventRequest.class)))
                .thenAnswer(inv -> EventResponse.builder().id(1L).build());

        CreateEventTool.Request request = new CreateEventTool.Request(
                "Réunion", LocalDate.of(2026, 4, 18), "09:00", null, null);

        function().apply(request);

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(agendaService).createEvent(captor.capture());
        assertThat(captor.getValue().getTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("mappe correctement tous les champs de la requête et retourne la réponse du service")
    void createEvent_mapsAllFieldsAndReturnsServiceResponse() {
        EventResponse saved = EventResponse.builder().id(42L).title("Réunion").build();
        when(agendaService.createEvent(any(EventRequest.class))).thenReturn(saved);

        CreateEventTool.Request request = new CreateEventTool.Request(
                "Réunion", LocalDate.of(2026, 4, 18), "10h", "DG, DAF", "Ordre du jour");

        EventResponse result = function().apply(request);

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(agendaService).createEvent(captor.capture());
        EventRequest mapped = captor.getValue();
        assertThat(mapped.getTitle()).isEqualTo("Réunion");
        assertThat(mapped.getDate()).isEqualTo(LocalDate.of(2026, 4, 18));
        assertThat(mapped.getParticipants()).isEqualTo("DG, DAF");
        assertThat(mapped.getNotes()).isEqualTo("Ordre du jour");
    }

    @Test
    @DisplayName("signale l'outil utilisé via ToolContext")
    void createEvent_setsToolContext() {
        when(agendaService.createEvent(any(EventRequest.class)))
                .thenAnswer(inv -> EventResponse.builder().id(1L).build());

        CreateEventTool.Request request = new CreateEventTool.Request(
                "Réunion", LocalDate.of(2026, 4, 18), "9h", null, null);

        function().apply(request);

        assertThat(ToolContext.getToolName()).isEqualTo("create_event");
    }
}
