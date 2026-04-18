package com.inovconsulting.assistant.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration des beans transversaux de l'application.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate utilisé par GroqClient pour les appels HTTP sortants.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ObjectMapper partagé, configuré pour :
     *  - Sérialiser/désérialiser les types Java 8 Date/Time (LocalDate, LocalTime…)
     *  - Ignorer les propriétés inconnues à la désérialisation
     *  - Ne pas écrire les dates comme timestamps numériques
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Configuration de l'interface Swagger / OpenAPI 3.0.
     * Accessible sur http://localhost:8080/swagger-ui.html
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Assistant de Direction — Inov Consulting")
                        .description("""
                                API backend de l'agent IA assistant de direction.
                                
                                L'agent traite des requêtes en langage naturel et active les outils :
                                - **get_agenda** : consultation du calendrier du directeur
                                - **create_event** : planification d'un rendez-vous
                                - **summarize_document** : synthèse structurée d'un document
                                
                                La mémoire conversationnelle est maintenue par session (session_id).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Inov Consulting")
                                .email("saurel.lepene@gmail.com"))
                        .license(new License().name("Usage interne — confidentiel")));
    }
}