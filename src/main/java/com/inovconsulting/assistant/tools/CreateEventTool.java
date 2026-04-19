package com.inovconsulting.assistant.tools;

import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Function;

/**
 * Outil de création d'événement refactorisé pour Spring AI.
 */
@Configuration
@Slf4j
public class CreateEventTool {

    public record Request(
            @Description("Titre ou objet de l'événement") String title,
            @Description("Date de l'événement (YYYY-MM-DD)") LocalDate date,
            @Description("Heure de début (HH:mm)") String time,
            @Description("Participants séparés par des virgules") String participants,
            @Description("Notes complémentaires") String notes
    ) {}

    @Bean
    @Description("Crée un nouvel événement ou rendez-vous dans l'agenda du directeur.")
    public Function<Request, EventResponse> createEvent(AgendaService agendaService) {
        return request -> {
            log.info("CreateEventTool — création : {}", request.title());
            
            // Parsing de l'heure avec gestion du format "10h" -> "10:00"
            LocalTime parsedTime = LocalTime.parse(request.time().replace("h", ":").replaceAll(":$", ":00")
                    .replaceAll("^(\\d):","0$1:"));

            EventRequest req = new EventRequest(
                    request.title(),
                    request.date(),
                    parsedTime,
                    request.participants(),
                    request.notes()
            );

            return agendaService.createEvent(req);
        };
    }
}
