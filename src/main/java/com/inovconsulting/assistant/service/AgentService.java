package com.inovconsulting.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import com.inovconsulting.assistant.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrateur principal de l'agent IA.
 *
 * Flux d'un appel POST /agent/chat :
 *  1. Résoudre ou créer la session
 *  2. Persister le message utilisateur
 *  3. Reconstruire le contexte conversationnel
 *  4. Appeler le LLM avec les schémas d'outils (tool calling)
 *  5a. Si le LLM demande un outil → exécuter l'outil, renvoyer le résultat au LLM
 *  5b. Si réponse directe → utiliser le texte retourné
 *  6. Persister la réponse assistant
 *  7. Retourner le ChatResponse structuré
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private static final String CONTENT_KEY = "content";
    private final GroqClient     groqClient;
    private final ToolRegistry   toolRegistry;
    private final SessionService sessionService;

    /** Prompt système de l'agent — définit son comportement global */
    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant de direction intelligent d'Inov Consulting.
            Tu aides les directeurs et managers à gérer leur agenda et à synthétiser des documents.
            
            Date d'aujourd'hui : %s
            
            Règles de comportement :
            - Réponds TOUJOURS en français, de manière professionnelle et concise.
            - Pour toute question sur l'agenda (rendez-vous, planning, réunions), utilise OBLIGATOIREMENT l'outil get_agenda.
            - Pour planifier ou créer un événement, utilise OBLIGATOIREMENT l'outil create_event.
            - Pour synthétiser un document, utilise OBLIGATOIREMENT l'outil summarize_document.
            - Ne réponds JAMAIS de mémoire sur le contenu de l'agenda — consulte toujours l'outil.
            - Si une demande ne concerne ni l'agenda ni un document, réponds directement sans outil.
            - Sois naturel et conversationnel dans tes formulations finales.
            """;

    /**
     * Point d'entrée principal : traite un message utilisateur et retourne la réponse de l'agent.
     */
    public ChatResponse chat(ChatRequest request) {

        // ── 1. Résolution de session ──────────────────────────────────
        String sessionId = sessionService.resolveSessionId(request.getSessionId());
        int    turn      = sessionService.getTurnCount(sessionId) + 1;

        log.info("AgentService — session={}, turn={}", sessionId, turn);

        // ── 2. Persistance du message utilisateur ──────────────────────
        sessionService.saveUserMessage(sessionId, request.getMessage(), turn);

        // ── 3. Reconstruction du contexte conversationnel ──────────────
        //      On insère le system prompt en tête à chaque appel (stateless LLM)
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role",    "system",
                CONTENT_KEY, String.format(SYSTEM_PROMPT, LocalDate.now())
        ));

        /*
        On récupère tous les enciens messages avec le nouveau qui vient d'être
        ajouté
        */
        messages.addAll(sessionService.buildContextMessages(sessionId));

        // ── 4. Premier appel LLM (avec les outils disponibles) ─────────
        JsonNode llmMessage = groqClient.chat(messages, toolRegistry.getAllSchemas());

        String toolUsed      = null;
        String finalResponse;

        // ── 5. Gestion du tool calling ─────────────────────────────────
        JsonNode toolCalls = llmMessage.path("tool_calls");

        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            // Le LLM a demandé un ou plusieurs outils
            JsonNode firstCall  = toolCalls.get(0);
            String   toolName   = firstCall.path("function").path("name").asText();
            String   toolCallId = firstCall.path("id").asText();

            JsonNode argsNode;
            try {
                // Les arguments arrivent sous forme de string JSON — on les parse
                String argsStr = firstCall.path("function").path("arguments").asText("{}");
                argsNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(argsStr);
            } catch (Exception e) {
                log.error("AgentService — impossible de parser les arguments du tool call : {}", e.getMessage());
                argsNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            }

            log.info("AgentService — tool call demandé : '{}' avec args : {}", toolName, argsNode);

            // Exécution de l'outil
            String toolResult = toolRegistry.execute(toolName, argsNode);
            toolUsed = toolName;

            // Persister la trace de l'appel outil (interne, non visible dans l'historique public)
            sessionService.saveToolMessage(sessionId, toolName, toolResult, turn);

            // ── 5a. Second appel LLM avec le résultat de l'outil ──────────
            // On ajoute le message assistant (avec tool_calls) et le résultat outil au contexte
            messages.add(Map.of(
                    "role",    "assistant",
                    CONTENT_KEY, llmMessage.path(CONTENT_KEY).asText(""),
                    "tool_calls", toolCalls.toString()  // transmis en raw pour compatibilité
            ));

            // Construire le message tool_result au format OpenAI
            messages.add(Map.of(
                    "role",         "tool",
                    "tool_call_id", toolCallId,
                    CONTENT_KEY,      toolResult
            ));

            // Appel LLM final pour formuler la réponse en langage naturel
            JsonNode finalMessage = groqClient.chat(messages, null);
            finalResponse = finalMessage.path(CONTENT_KEY).asText(
                    "Je n'ai pas pu formuler une réponse. Merci de réessayer.");

        } else {
            // ── 5b. Réponse directe sans outil ────────────────────────────
            finalResponse = llmMessage.path(CONTENT_KEY).asText(
                    "Désolé, je n'ai pas pu traiter votre demande.");
        }

        // ── 6. Persistance de la réponse assistant ─────────────────────
        sessionService.saveAssistantMessage(sessionId, finalResponse, turn);

        log.info("AgentService — réponse générée, toolUsed={}", toolUsed);

        // ── 7. Construction de la réponse structurée ───────────────────
        return ChatResponse.builder()
                .sessionId(sessionId)
                .response(finalResponse)
                .toolUsed(toolUsed)
                .turn(turn)
                .build();
    }
}
