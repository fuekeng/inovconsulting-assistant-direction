package com.inovconsulting.assistant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la réponse POST /agent/chat.
 * Le champ tool_used est null si l'agent n'a pas activé d'outil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse de l'agent IA")
public class ChatResponse {

    @JsonProperty("session_id")
    @Schema(description = "Identifiant UUID de la session", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sessionId;

    @Schema(description = "Réponse en langage naturel produite par l'agent",
            example = "Vous avez 2 rendez-vous demain : un comité de direction à 9h et une réunion tech à 14h30.")
    private String response;

    @JsonProperty("tool_used")
    @Schema(description = "Nom de l'outil activé. Null si aucun outil n'a été nécessaire.", example = "get_agenda", nullable = true)
    private String toolUsed;

    @Schema(description = "Numéro du tour dans la session (commence à 1)", example = "3")
    private int turn;
}