package com.inovconsulting.assistant;

import com.inovconsulting.assistant.controller.SessionController;
import com.inovconsulting.assistant.model.dto.SessionHistoryEntry;
import com.inovconsulting.assistant.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de SessionController — vérifie la délégation à SessionService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController — Tests unitaires")
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    @Test
    @DisplayName("getHistory — délègue au service et retourne 200 avec l'historique")
    void getHistory_returnsOkWithServiceResult() {
        List<SessionHistoryEntry> history = List.of(
                SessionHistoryEntry.builder().role("user").content("Bonjour").turn(1).build());
        when(sessionService.getHistory("session-123")).thenReturn(history);

        ResponseEntity<List<SessionHistoryEntry>> response = sessionController.getHistory("session-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(history);
    }

    @Test
    @DisplayName("getHistory — retourne 200 avec une liste vide si session inconnue")
    void getHistory_returnsOkWithEmptyListWhenUnknownSession() {
        when(sessionService.getHistory("inconnu")).thenReturn(List.of());

        ResponseEntity<List<SessionHistoryEntry>> response = sessionController.getHistory("inconnu");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
