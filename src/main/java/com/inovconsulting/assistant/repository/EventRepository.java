package com.inovconsulting.assistant.repository;

import com.inovconsulting.assistant.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Accès aux données des événements agenda.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /** Récupère tous les événements d'une journée donnée, triés par heure. */
    List<Event> findByDateOrderByTimeAsc(LocalDate date);

    /** Récupère tous les événements entre deux dates (incluses), triés par date puis heure. */
    List<Event> findByDateBetweenOrderByDateAscTimeAsc(LocalDate start, LocalDate end);
}