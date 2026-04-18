package com.inovconsulting.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la requête POST /agent/chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête envoyée à l'agent IA")
public class ChatRequest {

    @Schema(
            description = "ID de session existante. Null ou absent pour démarrer une nouvelle session.",
            example = "null",
            nullable = true
    )
    private String sessionId;

    @NotBlank(message = "Le message ne peut pas être vide")
    @Schema(
            description = "Message en langage naturel adressé à l'agent",
            example = "Quels sont mes rendez-vous de demain ?"
    )
    private String message;
}