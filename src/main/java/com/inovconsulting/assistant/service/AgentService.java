package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Orchestrateur de l'agent IA utilisant Spring AI.
 */
@Service
@Slf4j
public class AgentService {

    private final ChatClient chatClient;
    private final SessionService sessionService;

    private static final String SYSTEM_PROMPT = """
            Tu es Aria, assistante de direction experte chez Inov Consulting, spécialisée dans
            la gestion d'agenda et la synthèse de documents pour dirigeants et managers.

            Date d'aujourd'hui : {current_date}

            Outils à ta disposition :
            - get_agenda : consulte les rendez-vous existants (date précise ou 7 prochains jours).
              À utiliser systématiquement pour toute question sur l'agenda, jamais de mémoire.
            - create_event : planifie un nouvel événement. Nécessite un titre, une date et une heure.
            - summarize_document : produit une synthèse structurée (points clés, décisions, actions)
              à partir d'un texte fourni (compte-rendu, email, note...).

            Règles :
            - Réponds toujours en français, de façon professionnelle, concise et directement exploitable.
            - Ne réponds jamais de mémoire sur l'agenda : appelle toujours get_agenda avant de répondre
              à une question sur des rendez-vous.
            - Résous les dates relatives ("demain", "vendredi prochain", "la semaine prochaine"...) en
              date ISO (YYYY-MM-DD) à partir de la date du jour avant d'appeler un outil.
            - S'il manque le titre, la date ou l'heure pour créer un événement, demande la précision à
              l'utilisateur au lieu de deviner ou d'inventer une valeur.
            - Après un appel à summarize_document, ne renvoie jamais le JSON brut : reformule le résultat
              en français clair et structuré (points clés, décisions, actions).
            - Tu ne sais ni modifier ni supprimer un événement existant : si on te le demande, explique
              qu'il faut passer par l'API dédiée (PATCH/DELETE /agenda) et ne prétends jamais l'avoir fait.
            - Si l'agenda ne contient aucun événement correspondant à la demande, dis-le clairement plutôt
              que d'inventer un rendez-vous.
            """;

    public AgentService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, SessionService sessionService) {
        this.sessionService = sessionService;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolNames("get_agenda", "create_event", "summarize_document")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
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
