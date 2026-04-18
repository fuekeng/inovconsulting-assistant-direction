package com.inovconsulting.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Outil 01 — Consultation de l'agenda.
 *
 * Le LLM peut appeler cet outil avec :
 *  - date      : une date ISO précise (YYYY-MM-DD)
 *  - range     : "week" pour la semaine courante
 *  - (aucun)   : retourne tous les événements
 *
 * Le résultat est sérialisé en JSON et retourné au LLM qui formule
 * ensuite la réponse en langage naturel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetAgendaTool implements AgentTool {

    private static final String RANGE_KEY = "range";
    private static final String DESC_KEY = "description";

    private final AgendaService agendaService;
    private final ObjectMapper  objectMapper;

    @Override
    public String getName() {
        return "get_agenda";
    }

    /**
     * Schéma OpenAI function calling exposé au LLM.
     */
    @Override
    public ObjectNode getSchema() {
        // Paramètres
        ObjectNode dateParam = objectMapper.createObjectNode();
        dateParam.put("type", "string");
        dateParam.put(DESC_KEY,
                "Date précise au format ISO YYYY-MM-DD. Exemples : aujourd'hui = "
                        + LocalDate.now() + ", demain = " + LocalDate.now().plusDays(1));

        ObjectNode rangeParam = objectMapper.createObjectNode();
        rangeParam.put("type", "string");
        rangeParam.put(DESC_KEY,
                "Plage temporelle. Valeur acceptée : 'week' pour les 7 prochains jours.");
        rangeParam.set("enum", objectMapper.createArrayNode().add("week"));

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("date", dateParam);
        properties.set(RANGE_KEY, rangeParam);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        // Aucun paramètre obligatoire — le LLM choisit la combinaison appropriée
        parameters.set("required", objectMapper.createArrayNode());

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("name", getName());
        schema.put(DESC_KEY,
                "Interroge l'agenda du directeur. Peut filtrer par date précise ou par semaine. "
                        + "Retourne la liste des événements avec titre, date, heure, participants et notes.");
        schema.set("parameters", parameters);

        return schema;
    }

    /**
     * Exécution : appelle AgendaService et sérialise le résultat.
     */
    @Override
    public String execute(JsonNode arguments) {
        String date  = arguments.has("date")  ? arguments.get("date").asText()  : null;
        String range = arguments.has(RANGE_KEY) ? arguments.get(RANGE_KEY).asText() : null;

        List<EventResponse> events;

        if (date != null && !date.isBlank()) {
            try {
                LocalDate localDate = LocalDate.parse(date);
                log.info("GetAgendaTool — consultation date : {}", localDate);
                events = agendaService.getEventsByDate(localDate);
            } catch (DateTimeParseException e) {
                log.warn("GetAgendaTool — date invalide reçue du LLM : '{}'", date);
                return "{\"error\": \"Format de date invalide : " + date + ". Utilisez YYYY-MM-DD.\"}";
            }
        } else if ("week".equalsIgnoreCase(range)) {
            LocalDate start = LocalDate.now();
            LocalDate end   = start.plusDays(6);
            log.info("GetAgendaTool — consultation semaine : {} → {}", start, end);
            events = agendaService.getEventsByRange(start, end);
        } else {
            log.info("GetAgendaTool — consultation tous les événements");
            events = agendaService.getEvents(null, null);
        }

        try {
            // Ajouter la date d'aujourd'hui pour aider le LLM à contextualiser
            ObjectNode result = objectMapper.createObjectNode();
            result.put("today", LocalDate.now().toString());
            result.set("events", objectMapper.valueToTree(events));
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("GetAgendaTool — erreur sérialisation : {}", e.getMessage());
            return "{\"error\": \"Erreur lors de la récupération des événements.\"}";
        }
    }
}
