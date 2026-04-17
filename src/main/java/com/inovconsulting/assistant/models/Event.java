package com.inovconsulting.assistant.models;

import jakarta.persistence.*;
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
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titre / objet de l'événement */
    @Column(nullable = false)
    private String title;

    /** Date au format ISO (YYYY-MM-DD) */
    @Column(nullable = false)
    private LocalDate date;

    /** Heure de début (HH:mm) */
    @Column(nullable = false)
    private LocalTime time;

    /** Liste des participants — stockée en texte libre séparé par virgules */
    @Column
    private String participants;

    /** Notes ou remarques complémentaires */
    @Column(columnDefinition = "TEXT")
    private String notes;

}
