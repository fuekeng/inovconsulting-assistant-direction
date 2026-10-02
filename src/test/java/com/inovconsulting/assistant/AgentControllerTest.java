package com.inovconsulting.assistant;

import com.inovconsulting.assistant.controller.AgentController;
import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import com.inovconsulting.assistant.service.AgentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AgentController — vérifie la délégation à AgentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentController — Tests unitaires")
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @InjectMocks
    private AgentController agentController;

    @Test
    @DisplayName("chat — délègue au service et retourne 200 avec la réponse")
    void chat_returnsOkWithServiceResponse() {
        ChatRequest request = new ChatRequest(null, "Quels sont mes rendez-vous ?");
        ChatResponse serviceResponse = ChatResponse.builder()
                .sessionId("session-123")
                .response("Vous avez 2 rendez-vous.")
                .turn(1)
                .build();
        when(agentService.chat(request)).thenReturn(serviceResponse);

        ResponseEntity<ChatResponse> response = agentController.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(agentService, times(1)).chat(request);
    }
}
