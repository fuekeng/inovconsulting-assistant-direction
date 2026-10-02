package com.inovconsulting.assistant;

import com.inovconsulting.assistant.model.dto.SessionHistoryEntry;
import com.inovconsulting.assistant.model.entity.SessionMessage;
import com.inovconsulting.assistant.repository.SessionMessageRepository;
import com.inovconsulting.assistant.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de SessionService.
 * Utilise Mockito pour isoler le repository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — Tests unitaires")
class SessionServiceTest {

    @Mock
    private SessionMessageRepository sessionMessageRepository;

    @InjectMocks
    private SessionService sessionService;

    // ─────────────────────────────────────────────────────────────────────────
    // resolveSessionId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveSessionId — retourne l'id fourni s'il est valide")
    void resolveSessionId_returnsProvidedId() {
        String result = sessionService.resolveSessionId("session-abc");

        assertThat(result).isEqualTo("session-abc");
    }

    @Test
    @DisplayName("resolveSessionId — génère un nouvel UUID si null")
    void resolveSessionId_generatesUuidWhenNull() {
        String result = sessionService.resolveSessionId(null);

        assertThat(result).isNotBlank();
        assertThatCode(() -> java.util.UUID.fromString(result)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("resolveSessionId — génère un nouvel UUID si vide/blanc")
    void resolveSessionId_generatesUuidWhenBlank() {
        String result = sessionService.resolveSessionId("   ");

        assertThat(result).isNotBlank();
        assertThatCode(() -> java.util.UUID.fromString(result)).doesNotThrowAnyException();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTurnCount
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTurnCount — délègue au repository avec le rôle 'user'")
    void getTurnCount_delegatesToRepository() {
        when(sessionMessageRepository.countBySessionIdAndRole("session-abc", "user")).thenReturn(3L);

        int result = sessionService.getTurnCount("session-abc");

        assertThat(result).isEqualTo(3);
        verify(sessionMessageRepository, times(1)).countBySessionIdAndRole("session-abc", "user");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveUserMessage / saveAssistantMessage
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveUserMessage — persiste un message avec le rôle 'user'")
    void saveUserMessage_persistsWithUserRole() {
        ArgumentCaptor<SessionMessage> captor = ArgumentCaptor.forClass(SessionMessage.class);

        sessionService.saveUserMessage("session-abc", "Bonjour", 1);

        verify(sessionMessageRepository, times(1)).save(captor.capture());
        SessionMessage saved = captor.getValue();
        assertThat(saved.getSessionId()).isEqualTo("session-abc");
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getContent()).isEqualTo("Bonjour");
        assertThat(saved.getTurn()).isEqualTo(1);
    }

    @Test
    @DisplayName("saveAssistantMessage — persiste un message avec le rôle 'assistant'")
    void saveAssistantMessage_persistsWithAssistantRole() {
        ArgumentCaptor<SessionMessage> captor = ArgumentCaptor.forClass(SessionMessage.class);

        sessionService.saveAssistantMessage("session-abc", "Voici votre réponse", 1);

        verify(sessionMessageRepository, times(1)).save(captor.capture());
        SessionMessage saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo("assistant");
        assertThat(saved.getContent()).isEqualTo("Voici votre réponse");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistory — ne retourne que les messages user et assistant, dans l'ordre")
    void getHistory_returnsOnlyUserAndAssistantMessages() {
        Instant now = Instant.parse("2026-04-18T09:00:00Z");
        SessionMessage userMsg = SessionMessage.builder()
                .sessionId("session-abc").role("user").content("Bonjour").timestamp(now).turn(1).build();
        SessionMessage toolMsg = SessionMessage.builder()
                .sessionId("session-abc").role("tool").content("{}").timestamp(now).turn(1).build();
        SessionMessage assistantMsg = SessionMessage.builder()
                .sessionId("session-abc").role("assistant").content("Bonjour à vous").timestamp(now).turn(1).build();

        when(sessionMessageRepository.findBySessionIdOrderByIdAsc("session-abc"))
                .thenReturn(List.of(userMsg, toolMsg, assistantMsg));

        List<SessionHistoryEntry> history = sessionService.getHistory("session-abc");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo("user");
        assertThat(history.get(0).getContent()).isEqualTo("Bonjour");
        assertThat(history.get(0).getTimestamp()).isEqualTo(now.toString());
        assertThat(history.get(1).getRole()).isEqualTo("assistant");
        assertThat(history.get(1).getContent()).isEqualTo("Bonjour à vous");
    }

    @Test
    @DisplayName("getHistory — retourne une liste vide si aucune session trouvée")
    void getHistory_returnsEmptyListWhenNoSession() {
        when(sessionMessageRepository.findBySessionIdOrderByIdAsc("inconnu")).thenReturn(List.of());

        List<SessionHistoryEntry> history = sessionService.getHistory("inconnu");

        assertThat(history).isEmpty();
    }
}
