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
            @Description("Texte brut du document à synthétiser (compte-rendu, rapport, email, note interne)")
            String text
    ) {}

    @Bean
    @Description("Produit une synthèse structurée (points clés, décisions, actions) d'un document texte.")
    public Function<Request, String> summarize_document(ChatClient.Builder chatClientBuilder) {
        return request -> {
            ToolContext.setToolName("summarize_document");
            log.info("SummarizeDocumentTool — synthèse d'un document ({} caractères)", request.text().length());

            String systemPrompt = """
                Tu es un assistant de direction expert en synthèse documentaire.
                Analyse le document fourni et réponds UNIQUEMENT en JSON valide avec cette structure exacte :
                {
                  "points_cles": ["point 1", "point 2", "point 3"],
                  "decisions": ["décision 1", "décision 2"],
                  "actions": [
                    {"action": "description", "responsable": "nom ou rôle", "echeance": "délai si mentionné ou null"}
                  ]
                }
                Règles :
                - points_cles : 3 à 5 éléments maximum, concis et exploitables.
                - decisions : liste des décisions prises (peut être vide []).
                - actions : liste des tâches à réaliser avec responsable identifié.
                - Réponds uniquement avec le JSON, sans texte avant ni après.
                """;

            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user("Document à synthétiser :\n\n" + request.text())
                    .call()
                    .content();
        };
    }
}
