package com.inovconsulting.assistant.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Outil de synthèse documentaire refactorisé pour Spring AI.
 */
@Configuration
@Slf4j
public class SummarizeDocumentTool {

    public record Request(
            @Description("Texte brut du document à synthétiser (rapport, email, compte-rendu)")
            String text
    ) {}

    @Bean
    @Description("Produit une synthèse structurée (points clés, décisions, actions) d'un document texte.")
    public Function<Request, String> summarizeDocument(ChatClient.Builder chatClientBuilder) {
        return request -> {
            log.info("SummarizeDocumentTool — synthèse demandée");

            String systemPrompt = """
                Tu es un assistant de direction expert en synthèse documentaire.
                Analyse le document fourni et réponds en JSON valide avec cette structure :
                {
                  "points_cles": ["point 1", "point 2"],
                  "decisions": ["décision 1"],
                  "actions": [{"action": "desc", "responsable": "nom"}]
                }
                """;

            // On utilise un ChatClient frais pour cet appel "one-shot" sans outils
            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user("Document à synthétiser :\n\n" + request.text())
                    .call()
                    .content();
        };
    }
}
