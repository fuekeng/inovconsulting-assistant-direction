package com.inovconsulting.assistant.service;

import com.inovconsulting.assistant.model.dto.SessionHistoryEntry;
import com.inovconsulting.assistant.model.entity.SessionMessage;
import com.inovconsulting.assistant.repository.SessionMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de gestion de la mémoire conversationnelle.
 *
 * Responsabilités :
 *  - Générer un nouvel identifiant de session si absent
 *  - Persister chaque échange (user + assistant)
 *  - Reconstruire l'historique pour le contexte LLM (en respectant la fenêtre max)
 *  - Exposer l'historique pour l'endpoint GET /session/{id}/history
 *
 * Note : la mémoire de session est une responsabilité de la couche service,
 * non un outil de tool calling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    private static final String ASSISTANT_KEY = "assistant";
    private final SessionMessageRepository sessionMessageRepository;

    /** Nombre maximum de tours conservés dans la fenêtre de contexte LLM */
    @Value("${session.max.turns:20}")
    private int maxTurns;

    // ─────────────────────────────────────────────────────────
    // Gestion des sessions
    // ─────────────────────────────────────────────────────────

    /**
     * Retourne le sessionId fourni s'il est valide, sinon génère un nouveau UUID.
     */
    public String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            String newId = UUID.randomUUID().toString();
            log.info("SessionService — nouvelle session créée : {}", newId);
            return newId;
        }
        return sessionId;
    }

    /**
     * Compte le nombre de tours utilisateur dans la session.
     * Un "tour" = un message de rôle "user".
     */
    public int getTurnCount(String sessionId) {
        return (int) sessionMessageRepository.countBySessionIdAndRole(sessionId, "user");
    }

    // ─────────────────────────────────────────────────────────
    // Persistance des messages
    // ─────────────────────────────────────────────────────────

    /**
     * Persiste un message utilisateur dans la session.
     */
    public void saveUserMessage(String sessionId, String content, int turn) {
        saveMessage(sessionId, "user", content, null, turn);
    }

    /**
     * Persiste un message assistant dans la session.
     */
    public void saveAssistantMessage(String sessionId, String content, int turn) {
        saveMessage(sessionId, ASSISTANT_KEY, content, null, turn);
    }

    /**
     * Persiste le résultat d'un appel d'outil (pour la trace interne).
     */
    public void saveToolMessage(String sessionId, String toolName, String content, int turn) {
        saveMessage(sessionId, "tool", content, toolName, turn);
    }

    // ─────────────────────────────────────────────────────────
    // Reconstruction du contexte LLM
    // ─────────────────────────────────────────────────────────

    /**
     * Retourne les messages de la session sous forme de Map { role, content }
     * utilisables directement dans la requête Groq.
     * Seuls les messages user et assistant sont inclus (pas les messages tool internes).
     * La fenêtre est limitée à maxTurns * 2 messages (user + assistant par tour).
     */
    public List<Map<String, String>> buildContextMessages(String sessionId) {
        List<SessionMessage> all = sessionMessageRepository
                .findBySessionIdOrderByIdAsc(sessionId);

        // Filtrer uniquement user/assistant, exclure les traces tool internes
        List<SessionMessage> conversational = all.stream()
                .filter(m -> "user".equals(m.getRole()) || ASSISTANT_KEY.equals(m.getRole()))
                .toList();

        // Appliquer la fenêtre glissante
        int maxMessages = maxTurns * 2;
        if (conversational.size() > maxMessages) {
            conversational = conversational.subList(
                    conversational.size() - maxMessages, conversational.size());
        }

        return conversational.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // Historique exposé via API
    // ─────────────────────────────────────────────────────────

    /**
     * Retourne l'historique complet d'une session pour l'endpoint REST.
     * N'inclut que les messages user et assistant (lisibles par l'humain).
     */
    public List<SessionHistoryEntry> getHistory(String sessionId) {
        return sessionMessageRepository
                .findBySessionIdOrderByIdAsc(sessionId)
                .stream()
                .filter(m -> "user".equals(m.getRole()) || ASSISTANT_KEY.equals(m.getRole()))
                .map(m -> SessionHistoryEntry.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp().toString())
                        .turn(m.getTurn())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // Helper privé
    // ─────────────────────────────────────────────────────────

    private void saveMessage(String sessionId, String role, String content, String toolName, int turn) {
        SessionMessage msg = SessionMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .toolName(toolName)
                .turn(turn)
                .build();
        sessionMessageRepository.save(msg);
    }
}
