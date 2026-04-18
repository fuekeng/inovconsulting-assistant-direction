package com.inovconsulting.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Représentation d'un événement agenda retourné dans les réponses HTTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Événement agenda")
public class EventResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Réunion équipe Tech")
    private String title;

    @Schema(description = "Date ISO", example = "2026-04-18")
    private LocalDate date;

    @Schema(description = "Heure de début", example = "14:30")
    private LocalTime time;

    @Schema(description = "Participants séparés par virgules", example = "Lead Dev, DevOps")
    private String participants;

    @Schema(example = "Point sprint en cours")
    private String notes;
}