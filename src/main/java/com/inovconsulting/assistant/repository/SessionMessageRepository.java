package com.inovconsulting.assistant.repository;

import com.inovconsulting.assistant.model.entity.SessionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès aux messages de session pour la mémoire conversationnelle.
 */
@Repository
public interface SessionMessageRepository extends JpaRepository<SessionMessage, Long> {

    /** Récupère tous les messages d'une session, dans l'ordre chronologique. */
    List<SessionMessage> findBySessionIdOrderByIdAsc(String sessionId);

    /** Vérifie l'existence d'une session. */
    boolean existsBySessionId(String sessionId);

    /** Compte le nombre de tours (messages user uniquement) dans une session. */
    long countBySessionIdAndRole(String sessionId, String role);
}