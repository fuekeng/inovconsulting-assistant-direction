package com.inovconsulting.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Outil 01b — Création d'un événement agenda en langage naturel.
 *
 * Appelé quand l'utilisateur demande : "Planifie une réunion vendredi à 10h avec l'équipe tech".
 * Le LLM extrait les paramètres structurés et cet outil persiste l'événement.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateEventTool implements AgentTool {

    private static final String TYPE_STRING = "string";
    private static final String DESC_KEY = "description";
    private static final String TITLE_KEY = "title";
    private static final String DATE_KEY = "date";
    private static final String TIME_KEY = "time";

    private final AgendaService agendaService;
    private final ObjectMapper  objectMapper;

    @Override
    public String getName() {
        return "create_event";
    }

    @Override
    public ObjectNode getSchema() {
        // Propriétés
        ObjectNode titleProp = objectMapper.createObjectNode();
        titleProp.put("type", TYPE_STRING);
        titleProp.put(DESC_KEY, "Titre ou objet de l'événement");

        ObjectNode dateProp = objectMapper.createObjectNode();
        dateProp.put("type", TYPE_STRING);
        dateProp.put(DESC_KEY,
                "Date de l'événement au format ISO YYYY-MM-DD. "
                        + "Aujourd'hui = " + LocalDate.now()
                        + ", demain = " + LocalDate.now().plusDays(1));

        ObjectNode timeProp = objectMapper.createObjectNode();
        timeProp.put("type", TYPE_STRING);
        timeProp.put(DESC_KEY, "Heure de début au format HH:mm, ex: 10:00, 14:30");

        ObjectNode partProp = objectMapper.createObjectNode();
        partProp.put("type", TYPE_STRING);
        partProp.put(DESC_KEY, "Liste des participants séparés par virgules, ex: Lead Dev, DRH");

        ObjectNode notesProp = objectMapper.createObjectNode();
        notesProp.put("type", TYPE_STRING);
        notesProp.put(DESC_KEY, "Notes ou remarques complémentaires (optionnel)");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set(TITLE_KEY,        titleProp);
        properties.set(DATE_KEY,         dateProp);
        properties.set(TIME_KEY,         timeProp);
        properties.set("participants", partProp);
        properties.set("notes",        notesProp);

        // Champs obligatoires
        ArrayNode required = objectMapper.createArrayNode()
                .add(TITLE_KEY).add(DATE_KEY).add(TIME_KEY);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        parameters.set("required", required);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("name", getName());
        schema.put(DESC_KEY,
                "Crée un nouvel événement dans l'agenda du directeur. "
                        + "À utiliser quand l'utilisateur demande de planifier, ajouter ou créer un rendez-vous.");
        schema.set("parameters", parameters);

        return schema;
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            String title        = arguments.path(TITLE_KEY).asText(null);
            String dateStr      = arguments.path(DATE_KEY).asText(null);
            String timeStr      = arguments.path(TIME_KEY).asText(null);
            String participants = arguments.path("participants").asText(null);
            String notes        = arguments.path("notes").asText(null);

            // Validation minimale
            if (title == null || dateStr == null || timeStr == null) {
                return "{\"error\": \"Paramètres manquants : title, date et time sont obligatoires.\"}";
            }

            EventRequest req = parseEventRequest(title, dateStr, timeStr, participants, notes);
            EventResponse created = agendaService.createEvent(req);

            log.info("CreateEventTool — événement créé : id={}, title={}", created.getId(), created.getTitle());
            return objectMapper.writeValueAsString(created);

        } catch (DateTimeParseException e) {
            return "{\"error\": \"Format date/heure invalide. Date: YYYY-MM-DD, Heure: HH:mm\"}";
        } catch (Exception e) {
            log.error("CreateEventTool — erreur : {}", e.getMessage());
            return "{\"error\": \"Impossible de créer l'événement : " + e.getMessage() + "\"}";
        }
    }

    private EventRequest parseEventRequest(String title, String dateStr, String timeStr, String participants, String notes) {
        LocalDate date = LocalDate.parse(dateStr);
        // Accepter "10:00" ou "10h00" ou "10h"
        LocalTime time = LocalTime.parse(timeStr.replace("h", ":").replaceAll(":$", ":00")
                .replaceAll("^(\\d):","0$1:"));

        return new EventRequest(title, date, time, participants, notes);
    }
}
