package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * Orchestrateur de l'agent IA utilisant Spring AI.
 */
@Service
@Slf4j
public class AgentService {

    private final ChatClient chatClient;
    private final SessionService sessionService;

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant de direction intelligent d'Inov Consulting.
            Tu aides les directeurs et managers à gérer leur agenda et à synthétiser des documents.
            
            Date d'aujourd'hui : {current_date}
            
            Règles :
            - Réponds en français, de manière professionnelle et concise.
            - Utilise les outils fournis pour l'agenda (get_agenda, create_event) et la synthèse (summarize_document).
            - Ne réponds jamais de mémoire sur l'agenda, consulte toujours l'outil.
            """;

    public AgentService(ChatClient.Builder chatClientBuilder, SessionService sessionService) {
        this.sessionService = sessionService;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultFunctions("get_agenda", "create_event", "summarize_document")
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        String sessionId = sessionService.resolveSessionId(request.getSessionId());
        int turn = sessionService.getTurnCount(sessionId) + 1;

        log.info("AgentService (Spring AI) — session={}, turn={}", sessionId, turn);

        // Nettoyage du contexte de l'outil avant l'appel
        ToolContext.clear();

        try {
            // Appel du LLM (Spring AI gère les appels d'outils de manière synchrone ici)
            String responseContent = chatClient.prompt()
                    .system(sp -> sp.param("current_date", LocalDate.now().toString()))
                    .user(request.getMessage())
                    .advisors(a -> a
                            .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                    .call()
                    .content();

            // Récupération du nom de l'outil capturé pendant l'exécution des fonctions
            String toolUsed = ToolContext.getToolName();

            // Persistance base de données
            sessionService.saveUserMessage(sessionId, request.getMessage(), turn);
            sessionService.saveAssistantMessage(sessionId, responseContent, turn);

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(responseContent)
                    .toolUsed(toolUsed)
                    .turn(turn)
                    .build();
        } finally {
            // Nettoyage final pour éviter les fuites de mémoire ThreadLocal
            ToolContext.clear();
        }
    }
}
