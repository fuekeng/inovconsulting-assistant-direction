package com.inovconsulting.assistant.tools;

import com.inovconsulting.assistant.config.ToolContext;
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

@Configuration
@Slf4j
public class CreateEventTool {

    public record Request(
            @Description("Titre de l'événement") String title,
            @Description("Date (YYYY-MM-DD)") LocalDate date,
            @Description("Heure (HH:mm)") String time,
            @Description("Participants") String participants,
            @Description("Notes") String notes
    ) {}

    @Bean
    @Description("Crée un nouvel événement dans l'agenda du directeur.")
    public Function<Request, EventResponse> create_event(AgendaService agendaService) {
        return request -> {
            ToolContext.setToolName("create_event"); // Signalement de l'outil
            log.info("CreateEventTool — création : {}", request.title());
            LocalTime parsedTime = LocalTime.parse(request.time().replace("h", ":").replaceAll(":$", ":00")
                    .replaceAll("^(\\d):","0$1:"));
            EventRequest req = new EventRequest(request.title(), request.date(), parsedTime, request.participants(), request.notes());
            return agendaService.createEvent(req);
        };
    }
}
