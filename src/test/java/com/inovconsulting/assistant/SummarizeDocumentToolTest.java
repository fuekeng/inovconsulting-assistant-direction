package com.inovconsulting.assistant;

import com.inovconsulting.assistant.config.ToolContext;
import com.inovconsulting.assistant.tools.SummarizeDocumentTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'outil summarize_document. Le ChatClient est mocké
 * pour vérifier la construction du prompt sans appeler le LLM réel.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummarizeDocumentTool — Tests unitaires")
class SummarizeDocumentToolTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private final SummarizeDocumentTool tool = new SummarizeDocumentTool();

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @AfterEach
    void tearDown() {
        ToolContext.clear();
    }

    private Function<SummarizeDocumentTool.Request, String> function() {
        return tool.summarizeDocument(chatClientBuilder);
    }

    @Test
    @DisplayName("retourne le contenu produit par le LLM")
    void summarizeDocument_returnsLlmContent() {
        String jsonResult = "{\"points_cles\":[],\"decisions\":[],\"actions\":[]}";
        when(callResponseSpec.content()).thenReturn(jsonResult);

        String result = function().apply(new SummarizeDocumentTool.Request("Compte-rendu de réunion..."));

        assertThat(result).isEqualTo(jsonResult);
    }

    @Test
    @DisplayName("inclut le texte du document dans le prompt utilisateur")
    void summarizeDocument_includesDocumentTextInUserPrompt() {
        when(callResponseSpec.content()).thenReturn("{}");

        function().apply(new SummarizeDocumentTool.Request("Texte du document à résumer"));

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("Texte du document à résumer");
    }

    @Test
    @DisplayName("signale l'outil utilisé via ToolContext")
    void summarizeDocument_setsToolContext() {
        when(callResponseSpec.content()).thenReturn("{}");

        function().apply(new SummarizeDocumentTool.Request("texte"));

        assertThat(ToolContext.getToolName()).isEqualTo("summarize_document");
    }
}
