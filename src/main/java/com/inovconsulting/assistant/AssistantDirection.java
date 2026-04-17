package com.inovconsulting.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        SpringApplication.run(AssistantDirection.class, args);
        logger.info("Assistant Direction a démarré avec succès !!");
    }
}