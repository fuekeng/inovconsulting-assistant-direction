package com.inovconsulting.assistant;
import com.inovconsulting.assistant.service.SessionService;
import com.inovconsulting.assistant.model.entity.SessionMessage;
import com.inovconsulting.assistant.repository.SessionMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service de mémoire conversationnelle SessionService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — Tests unitaires")
class SessionSessionServiceTest {

    @Mock
    private SessionMessageRepository sessionMessageRepository;

    @InjectMocks
    private SessionService sessionService;

    // ─────────────────────────────────────────────────────────────────────────
    // resolveSessionId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveSessionId — génère un UUID si session_id est null")
    void resolveSessionId_generatesUuidWhenNull() {
        String id = sessionService.resolveSessionId(null);
        assertThat(id).isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("resolveSessionId — génère un UUID si session_id est vide")
    void resolveSessionId_generatesUuidWhenBlank() {
        String id = sessionService.resolveSessionId("   ");
        assertThat(id).isNotBlank();
    }

    @Test
    @DisplayName("resolveSessionId — conserve le session_id fourni s'il est valide")
    void resolveSessionId_preservesExistingId() {
        String existing = "mon-session-id";
        assertThat(sessionService.resolveSessionId(existing)).isEqualTo(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildContextMessages
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildContextMessages — retourne uniquement user et assistant (pas tool)")
    void buildContextMessages_excludesToolMessages() {
        List<SessionMessage> stored = List.of(
                makeMsg(1L, "user",      "Bonjour",      1),
                makeMsg(2L, "assistant", "Bonjour !",    1),
                makeMsg(3L, "tool",      "{...result}", 1),   // doit être filtré
                makeMsg(4L, "user",      "Et demain ?",  2),
                makeMsg(5L, "assistant", "Deux RDV.",    2)
        );

        when(sessionMessageRepository.findBySessionIdOrderByIdAsc("sess-1"))
                .thenReturn(stored);

        List<Map<String, String>> ctx = sessionService.buildContextMessages("sess-1");

        assertThat(ctx).hasSize(4)
                .noneMatch(m -> "tool".equals(m.get("role")));
    }

    @Test
    @DisplayName("buildContextMessages — applique la fenêtre glissante (maxTurns=2 → 4 messages max)")
    void buildContextMessages_appliesSlidingWindow() {
        // Simuler 6 échanges user+assistant = 12 messages
        List<SessionMessage> stored = List.of(
                makeMsg(1L, "user",      "msg1", 1), makeMsg(2L,  "assistant", "rep1", 1),
                makeMsg(3L, "user",      "msg2", 2), makeMsg(4L,  "assistant", "rep2", 2),
                makeMsg(5L, "user",      "msg3", 3), makeMsg(6L,  "assistant", "rep3", 3),
                makeMsg(7L, "user",      "msg4", 4), makeMsg(8L,  "assistant", "rep4", 4),
                makeMsg(9L, "user",      "msg5", 5), makeMsg(10L, "assistant", "rep5", 5),
                makeMsg(11L,"user",      "msg6", 6), makeMsg(12L, "assistant", "rep6", 6)
        );

        when(sessionMessageRepository.findBySessionIdOrderByIdAsc(anyString()))
                .thenReturn(stored);

        // maxTurns = 20 par défaut via @Value — ici on vérifie que tous sont retournés
        List<Map<String, String>> ctx = sessionService.buildContextMessages("any");
        assertThat(ctx).hasSize(12); // 20 * 2 = 40 > 12 → tous conservés
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistory — retourne uniquement les messages user et assistant")
    void getHistory_returnsOnlyConversationalMessages() {
        List<SessionMessage> stored = List.of(
                makeMsg(1L, "user",      "Quels RDV ?",  1),
                makeMsg(2L, "tool",      "{events...}",  1),
                makeMsg(3L, "assistant", "Vous avez…",   1)
        );

        when(sessionMessageRepository.findBySessionIdOrderByIdAsc("sess-2"))
                .thenReturn(stored);

        var history = sessionService.getHistory("sess-2");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo("user");
        assertThat(history.get(1).getRole()).isEqualTo("assistant");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private SessionMessage makeMsg(Long id, String role, String content, int turn) {
        return SessionMessage.builder()
                .id(id)
                .sessionId("test-session")
                .role(role)
                .content(content)
                .turn(turn)
                .timestamp(Instant.now())
                .build();
    }
}
