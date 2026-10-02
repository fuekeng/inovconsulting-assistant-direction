package com.inovconsulting.assistant;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import com.inovconsulting.assistant.service.AgentService;
import com.inovconsulting.assistant.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AgentService.
 * La chaîne fluide de ChatClient (Builder / RequestSpec / CallResponseSpec) est mockée
 * pour isoler AgentService du LLM réel.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentService — Tests unitaires")
class AgentServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatMemory chatMemory;
    @Mock
    private SessionService sessionService;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultToolNames(any(String[].class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        agentService = new AgentService(chatClientBuilder, chatMemory, sessionService);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @AfterEach
    void tearDown() {
        ToolContext.clear();
    }

    @Test
    @DisplayName("chat — résout la session, appelle le LLM et persiste les messages")
    void chat_resolvesSessionAndPersistsMessages() {
        ChatRequest request = new ChatRequest(null, "Quels sont mes rendez-vous ?");

        when(sessionService.resolveSessionId(null)).thenReturn("session-123");
        when(sessionService.getTurnCount("session-123")).thenReturn(0);
        when(requestSpec.user("Quels sont mes rendez-vous ?")).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("Vous avez 2 rendez-vous.");

        ChatResponse response = agentService.chat(request);

        assertThat(response.getSessionId()).isEqualTo("session-123");
        assertThat(response.getResponse()).isEqualTo("Vous avez 2 rendez-vous.");
        assertThat(response.getTurn()).isEqualTo(1);
        assertThat(response.getToolUsed()).isNull();

        verify(sessionService, times(1)).saveUserMessage("session-123", "Quels sont mes rendez-vous ?", 1);
        verify(sessionService, times(1)).saveAssistantMessage("session-123", "Vous avez 2 rendez-vous.", 1);
    }

    @Test
    @DisplayName("chat — incrémente le tour à partir du nombre existant")
    void chat_incrementsTurnFromExistingCount() {
        ChatRequest request = new ChatRequest("session-existante", "Et demain ?");

        when(sessionService.resolveSessionId("session-existante")).thenReturn("session-existante");
        when(sessionService.getTurnCount("session-existante")).thenReturn(2);
        when(requestSpec.user("Et demain ?")).thenReturn(requestSpec);
        when(callResponseSpec.content()).thenReturn("Réponse");

        ChatResponse response = agentService.chat(request);

        assertThat(response.getTurn()).isEqualTo(3);
        verify(sessionService, times(1)).saveUserMessage("session-existante", "Et demain ?", 3);
        verify(sessionService, times(1)).saveAssistantMessage("session-existante", "Réponse", 3);
    }

    @Test
    @DisplayName("chat — restitue le nom de l'outil capturé via ToolContext pendant l'appel")
    void chat_capturesToolUsedFromToolContext() {
        ChatRequest request = new ChatRequest(null, "Planifie une réunion demain à 10h");

        when(sessionService.resolveSessionId(null)).thenReturn("session-456");
        when(sessionService.getTurnCount("session-456")).thenReturn(0);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenAnswer(invocation -> {
            ToolContext.setToolName("create_event");
            return callResponseSpec;
        });
        when(callResponseSpec.content()).thenReturn("Réunion planifiée.");

        ChatResponse response = agentService.chat(request);

        assertThat(response.getToolUsed()).isEqualTo("create_event");
        // Le ThreadLocal doit être nettoyé après l'appel (bloc finally)
        assertThat(ToolContext.getToolName()).isNull();
    }

    @Test
    @DisplayName("chat — nettoie le ToolContext même si le LLM lève une exception")
    void chat_clearsToolContextOnException() {
        ChatRequest request = new ChatRequest(null, "Message qui échoue");

        when(sessionService.resolveSessionId(null)).thenReturn("session-789");
        when(sessionService.getTurnCount("session-789")).thenReturn(0);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("LLM indisponible"));

        assertThatThrownBy(() -> agentService.chat(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM indisponible");

        assertThat(ToolContext.getToolName()).isNull();
        verify(sessionService, never()).saveUserMessage(any(), any(), anyInt());
        verify(sessionService, never()).saveAssistantMessage(any(), any(), anyInt());
    }
}
