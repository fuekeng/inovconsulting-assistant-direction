package com.inovconsulting.assistant.tools;

import com.inovconsulting.assistant.config.ToolContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@Slf4j
public class SummarizeDocumentTool {

    public record Request(
            @Description("Texte brut du document à synthétiser") String text
    ) {}

    @Bean
    @Description("Produit une synthèse structurée d'un document texte.")
    public Function<Request, String> summarize_document(ChatClient.Builder chatClientBuilder) {
        return request -> {
            ToolContext.setToolName("summarize_document"); // Signalement de l'outil
            log.info("SummarizeDocumentTool — synthèse demandée");
            String systemPrompt = "Tu es un assistant expert en synthèse documentaire. Réponds en JSON valide.";
            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user("Document :\n\n" + request.text())
                    .call()
                    .content();
        };
    }
}
