package com.inovconsulting.assistant.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private Long id;

    /** Titre / objet de l'événement */
    private String title;

    /** Date au format ISO (YYYY-MM-DD) */
    private LocalDate date;

    /** Heure de début (HH:mm) */
    private LocalTime time;

    /** Liste des participants — stockée en texte libre séparé par virgules */
    @Column
    private String participants;

    /** Notes ou remarques complémentaires */
    @Column(columnDefinition = "TEXT")
    private String notes;

}
