package com.inovconsulting.assistant.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entité représentant un message dans l'historique d'une session.
 * Chaque session est identifiée par un sessionId UUID.
 * Les rôles possibles sont : "user", "assistant", "tool".
 */
@Entity
@Table(name = "session_messages",
        indexes = @Index(name = "idx_session_id", columnList = "sessionId"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant UUID de la session */
    @Column(nullable = false)
    private String sessionId;

    /** "user" | "assistant" | "tool" */
    @Column(nullable = false)
    private String role;

    /** Contenu textuel du message */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Nom de l'outil appelé (nullable — renseigné uniquement pour role="tool") */
    @Column
    private String toolName;

    /** Horodatage UTC de création */
    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Numéro d'ordre du tour dans la session (commence à 1) */
    @Column(nullable = false)
    private int turn;
}