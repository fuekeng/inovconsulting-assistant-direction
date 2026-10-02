package com.inovconsulting.assistant;

import com.inovconsulting.assistant.controller.AgendaController;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AgendaController — vérifie le mapping HTTP (statuts, délégation au service).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgendaController — Tests unitaires")
class AgendaControllerTest {

    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private AgendaController agendaController;

    @Test
    @DisplayName("getEvents — délègue au service et retourne 200")
    void getEvents_returnsOkWithServiceResult() {
        LocalDate date = LocalDate.of(2026, 4, 18);
        List<EventResponse> events = List.of(EventResponse.builder().id(1L).build());
        when(agendaService.getEvents(date, null)).thenReturn(events);

        ResponseEntity<List<EventResponse>> response = agendaController.getEvents(date, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(events);
    }

    @Test
    @DisplayName("createEvent — retourne 201 avec l'événement créé")
    void createEvent_returnsCreatedWithBody() {
        EventRequest request = new EventRequest("Réunion", LocalDate.of(2026, 4, 18), LocalTime.of(10, 0), null, null);
        EventResponse created = EventResponse.builder().id(1L).title("Réunion").build();
        when(agendaService.createEvent(request)).thenReturn(created);

        ResponseEntity<EventResponse> response = agendaController.createEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    @DisplayName("updateEvent — retourne 200 avec l'événement mis à jour")
    void updateEvent_returnsOkWithUpdatedBody() {
        EventRequest patch = new EventRequest();
        patch.setTitle("Nouveau titre");
        EventResponse updated = EventResponse.builder().id(1L).title("Nouveau titre").build();
        when(agendaService.updateEvent(1L, patch)).thenReturn(updated);

        ResponseEntity<EventResponse> response = agendaController.updateEvent(1L, patch);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    @DisplayName("deleteEvent — délègue au service et retourne 204 sans contenu")
    void deleteEvent_returnsNoContent() {
        ResponseEntity<Void> response = agendaController.deleteEvent(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(agendaService, times(1)).deleteEvent(1L);
    }
}
