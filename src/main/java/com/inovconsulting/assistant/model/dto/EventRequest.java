package com.inovconsulting.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Corps de la requête POST /agenda (création) et PATCH /agenda/{id} (modification partielle).
 * Tous les champs sont optionnels pour le PATCH — seuls les champs non-null sont modifiés.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données d'un événement agenda")
public class EventRequest {

    @Schema(example = "Réunion équipe Tech")
    private String title;

    @Schema(description = "Date ISO YYYY-MM-DD", example = "2026-04-18")
    private LocalDate date;

    @Schema(description = "Heure de début HH:mm", example = "14:30")
    private LocalTime time;

    @Schema(description = "Participants séparés par virgules", example = "Lead Dev, DevOps")
    private String participants;

    @Schema(example = "Point sprint en cours")
    private String notes;
}