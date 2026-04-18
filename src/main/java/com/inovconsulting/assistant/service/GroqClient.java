package com.inovconsulting.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inovconsulting.assistant.exception.GroqApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP bas niveau vers l'API Groq (compatible OpenAI).
 *
 * Responsabilité unique : envoyer une requête /v1/chat/completions et retourner
 * le nœud JSON brut de la réponse. Le parsing métier (tool call vs texte) est
 * délégué à AgentService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    @Value("${groq.max.tokens:1024}")
    private int maxTokens;

    /**
     * Envoie une conversation au LLM avec un catalogue d'outils disponibles.
     *
     * @param messages    historique de conversation [{role, content}, ...]
     * @param toolSchemas définitions JSON des outils (format OpenAI function calling)
     * @return le nœud JSON "choices[0].message" brut de la réponse Groq
     */
    public JsonNode chat(List<Map<String, Object>> messages, List<ObjectNode> toolSchemas) {
        try {
            // ── Construction du corps de la requête ──────────────────────
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);

            // Messages
            ArrayNode msgsNode = objectMapper.valueToTree(messages);
            body.set("messages", msgsNode);

            // Outils (tool calling)
            if (toolSchemas != null && !toolSchemas.isEmpty()) {
                ArrayNode toolsNode = objectMapper.createArrayNode();
                for (ObjectNode schema : toolSchemas) {
                    ObjectNode wrapper = objectMapper.createObjectNode();
                    wrapper.put("type", "function");
                    wrapper.set("function", schema);
                    toolsNode.add(wrapper);
                }
                body.set("tools", toolsNode);
                body.put("tool_choice", "auto");
            }

            // ── En-têtes HTTP ─────────────────────────────────────────────
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(body), headers);

            log.debug("GroqClient — requête envoyée au modèle {}", model);

            // ── Appel HTTP ────────────────────────────────────────────────
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode message = root.path("choices").path(0).path("message");

            log.debug("GroqClient — réponse reçue : finish_reason={}",
                    root.path("choices").path(0).path("finish_reason").asText());

            return message;

        } catch (HttpClientErrorException e) {
            log.error("GroqClient — erreur HTTP {} : {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GroqApiException("Erreur Groq API : " + e.getStatusCode() + " — " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("GroqClient — erreur inattendue : {}", e.getMessage());
            throw new GroqApiException("Erreur lors de l'appel au LLM : " + e.getMessage(), e);
        }
    }
}
