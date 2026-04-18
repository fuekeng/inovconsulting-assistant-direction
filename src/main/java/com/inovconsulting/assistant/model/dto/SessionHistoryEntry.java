package com.inovconsulting.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un échange individuel dans l'historique de session
 * retourné par GET /session/{id}/history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Échange dans l'historique de session")
public class SessionHistoryEntry {

    @Schema(description = "Rôle de l'émetteur : user | assistant", example = "user")
    private String role;

    @Schema(description = "Contenu textuel du message")
    private String content;

    @Schema(description = "Horodatage ISO 8601 UTC", example = "2026-04-18T09:00:00Z")
    private String timestamp;

    @Schema(description = "Numéro du tour", example = "2")
    private int turn;
}