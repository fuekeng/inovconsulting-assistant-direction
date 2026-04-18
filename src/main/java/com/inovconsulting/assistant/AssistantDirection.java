package com.inovconsulting.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.logging.Logger;


/**
 * Point d'entrée de l'application Assistant de Direction — Inov Consulting.
 *
 * Lancement : ./mvnw spring-boot:run
 * Swagger UI : http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class AssistantDirection {

    private static final Logger logger = Logger.getLogger(AssistantDirection.class.getName());

    public static void main(String[] args) {

        // Chargement des variables d'environnement depuis le fichier .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(AssistantDirection.class, args);
        logger.info("Assistant Direction a démarré avec succès !!");
    }
}