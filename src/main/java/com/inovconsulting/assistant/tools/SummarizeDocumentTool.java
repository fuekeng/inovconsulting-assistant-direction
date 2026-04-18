package com.inovconsulting.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inovconsulting.assistant.service.GroqClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Outil 02 — Synthèse de document.
 *
 * Reçoit un texte brut (compte-rendu, rapport, email) et produit
 * une synthèse structurée contenant :
 *  - 3 à 5 points clés
 *  - Décisions prises
 *  - Actions à suivre avec responsables
 *
 * NOTE : cet outil fait lui-même un second appel LLM dédié à la synthèse,
 * sans outils, pour obtenir un résultat structuré et concentré.
 */
@RequiredArgsConstructor
@Slf4j
// @Component retiré intentionnellement — l'outil est enregistré manuellement
// dans ToolRegistry pour maîtriser l'ordre d'injection et éviter la dépendance circulaire.
public class SummarizeDocumentTool implements AgentTool {

    private static final String CONTENT_KEY = "content";

    private final GroqClient   groqClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "summarize_document";
    }

    @Override
    public ObjectNode getSchema() {
        ObjectNode textProp = objectMapper.createObjectNode();
        textProp.put("type", "string");
        textProp.put("description",
                "Texte brut du document à synthétiser : compte-rendu, rapport, email, note interne.");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("text", textProp);

        ArrayNode required = objectMapper.createArrayNode().add("text");

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        parameters.set("required", required);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("name", getName());
        schema.put("description",
                "Produit une synthèse structurée d'un document texte. "
                        + "Retourne les points clés, les décisions prises et les actions à suivre.");
        schema.set("parameters", parameters);

        return schema;
    }

    @Override
    public String execute(JsonNode arguments) {
        String text = arguments.path("text").asText(null);

        if (text == null || text.isBlank()) {
            return "{\"error\": \"Le champ 'text' est obligatoire et ne peut pas être vide.\"}";
        }

        log.info("SummarizeDocumentTool — synthèse d'un document ({} caractères)", text.length());

        // Prompt système dédié à la synthèse structurée
        String systemPrompt = """
                Tu es un assistant de direction expert en synthèse documentaire.
                Analyse le document fourni et réponds UNIQUEMENT en JSON valide avec cette structure exacte :
                {
                  "points_cles": ["point 1", "point 2", "point 3"],
                  "decisions": ["décision 1", "décision 2"],
                  "actions": [
                    {"action": "description", "responsable": "nom ou rôle", "echeance": "délai si mentionné ou null"}
                  ]
                }
                - points_cles : 3 à 5 éléments maximum, concis et exploitables
                - decisions : liste des décisions prises (peut être vide [])
                - actions : liste des tâches à réaliser avec responsable identifié
                Réponds uniquement avec le JSON, sans texte avant ni après.
                """;

        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            Map<String, Object> sysMsg = new java.util.HashMap<>();
            sysMsg.put("role",    "system");
            sysMsg.put(CONTENT_KEY, systemPrompt);
            messages.add(sysMsg);

            Map<String, Object> userMsg = new java.util.HashMap<>();
            userMsg.put("role",    "user");
            userMsg.put(CONTENT_KEY, "Document à synthétiser :\n\n" + text);
            messages.add(userMsg);

            // Appel LLM dédié — sans outils, pour obtenir du JSON pur
            JsonNode llmMessage = groqClient.chat(messages, null);
            String rawContent   = llmMessage.path(CONTENT_KEY).asText("");

            // Valider que c'est du JSON valide avant de retourner
            objectMapper.readTree(rawContent); // lève une exception si invalide
            log.info("SummarizeDocumentTool — synthèse générée avec succès");
            return rawContent;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Le LLM n'a pas retourné du JSON pur — on l'encapsule proprement
            log.warn("SummarizeDocumentTool — réponse LLM non-JSON, encapsulation");
            try {
                ObjectNode fallback = objectMapper.createObjectNode();
                fallback.put("synthese", arguments.path(CONTENT_KEY).asText("Synthèse indisponible"));
                return objectMapper.writeValueAsString(fallback);
            } catch (Exception ex) {
                return "{\"error\": \"Impossible de générer la synthèse.\"}";
            }
        } catch (Exception e) {
            log.error("SummarizeDocumentTool — erreur : {}", e.getMessage());
            return "{\"error\": \"Erreur lors de la synthèse : " + e.getMessage() + "\"}";
        }
    }
}
