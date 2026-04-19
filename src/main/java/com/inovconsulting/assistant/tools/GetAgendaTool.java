package com.inovconsulting.assistant.tools;

import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Outil de consultation de l'agenda refactorisé pour Spring AI.
 */
@Configuration
@Slf4j
public class GetAgendaTool {

    public record Request(
            @Description("Date précise au format ISO YYYY-MM-DD. Ex: 2024-12-25")
            LocalDate date,
            @Description("Plage temporelle. Valeur acceptée : 'week' pour les 7 prochains jours.")
            String range
    ) {}

    public record Response(String today, List<EventResponse> events) {}

    @Bean
    @Description("Interroge l'agenda du directeur pour consulter ses rendez-vous et réunions.")
    public Function<Request, Response> getAgenda(AgendaService agendaService) {
        return request -> {
            log.info("GetAgendaTool — appel avec date={} et range={}", request.date(), request.range());

            List<EventResponse> events;
            if (request.date() != null) {
                events = agendaService.getEventsByDate(request.date());
            } else if ("week".equalsIgnoreCase(request.range())) {
                events = agendaService.getEventsByRange(LocalDate.now(), LocalDate.now().plusDays(6));
            } else {
                events = agendaService.getEvents(null, null);
            }

            return new Response(LocalDate.now().toString(), events);
        };
    }
}
