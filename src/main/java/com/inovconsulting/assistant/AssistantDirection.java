package com.inovconsulting.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class AssistantDirection {

    private static final Logger logger = Logger.getLogger(AssistantDirection.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(AssistantDirection.class, args);
        logger.info("Assistant Direction a démarré avec succès !!");
    }
}