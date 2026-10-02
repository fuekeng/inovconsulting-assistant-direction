package com.inovconsulting.assistant;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import com.inovconsulting.assistant.tools.GetAgendaTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'outil get_agenda et de sa logique de dispatch
 * (date précise / range=week / aucun filtre).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetAgendaTool — Tests unitaires")
class GetAgendaToolTest {

    @Mock
    private AgendaService agendaService;

    private final GetAgendaTool tool = new GetAgendaTool();

    @AfterEach
    void tearDown() {
        ToolContext.clear();
    }

    private Function<GetAgendaTool.Request, GetAgendaTool.Response> function() {
        return tool.getAgenda(agendaService);
    }

    @Test
    @DisplayName("avec une date précise — interroge getEventsByDate")
    void getAgenda_withDate_callsGetEventsByDate() {
        LocalDate date = LocalDate.of(2026, 4, 18);
        List<EventResponse> events = List.of(EventResponse.builder().id(1L).build());
        when(agendaService.getEventsByDate(date)).thenReturn(events);

        GetAgendaTool.Response response = function().apply(new GetAgendaTool.Request(date, null));

        assertThat(response.events()).isEqualTo(events);
        assertThat(response.today()).isEqualTo(LocalDate.now().toString());
        verify(agendaService, times(1)).getEventsByDate(date);
        verify(agendaService, never()).getEventsByRange(any(), any());
        verify(agendaService, never()).getEvents(any(), any());
    }

    @Test
    @DisplayName("avec range=week — interroge getEventsByRange sur 7 jours")
    void getAgenda_withWeekRange_callsGetEventsByRange() {
        List<EventResponse> events = List.of(EventResponse.builder().id(1L).build());
        when(agendaService.getEventsByRange(any(), any())).thenReturn(events);

        GetAgendaTool.Response response = function().apply(new GetAgendaTool.Request(null, "week"));

        assertThat(response.events()).isEqualTo(events);
        verify(agendaService, times(1))
                .getEventsByRange(LocalDate.now(), LocalDate.now().plusDays(6));
        verify(agendaService, never()).getEventsByDate(any());
    }

    @Test
    @DisplayName("sans date ni range — interroge getEvents sans filtre")
    void getAgenda_noFilter_callsGetEvents() {
        List<EventResponse> events = List.of();
        when(agendaService.getEvents(null, null)).thenReturn(events);

        GetAgendaTool.Response response = function().apply(new GetAgendaTool.Request(null, null));

        assertThat(response.events()).isEmpty();
        verify(agendaService, times(1)).getEvents(null, null);
        verify(agendaService, never()).getEventsByDate(any());
        verify(agendaService, never()).getEventsByRange(any(), any());
    }

    @Test
    @DisplayName("signale l'outil utilisé via ToolContext")
    void getAgenda_setsToolContext() {
        when(agendaService.getEvents(null, null)).thenReturn(List.of());

        function().apply(new GetAgendaTool.Request(null, null));

        assertThat(ToolContext.getToolName()).isEqualTo("get_agenda");
    }
}
