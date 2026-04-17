package com.inovconsulting.assistant.db;

import com.inovconsulting.assistant.model.entity.Event;
import com.inovconsulting.assistant.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Initialise la base de données avec les 5 événements de départ définis dans le cahier des charges.
 * Les dates sont calculées dynamiquement par rapport à la date de lancement (J+1, J+2…).
 * Le seed ne s'exécute que si la table est vide, pour ne pas dupliquer les données.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;

    @Override
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            log.info("DataSeeder — données déjà présentes, seed ignoré.");
            return;
        }

        LocalDate today = LocalDate.now();

        List<Event> seedEvents = List.of(
                Event.builder()
                        .title("Comité de direction")
                        .date(today.plusDays(1))          // J+1
                        .time(LocalTime.of(9, 0))
                        .participants("DG, DAF, DSI")
                        .notes("Budget Q2 à valider")
                        .build(),

                Event.builder()
                        .title("Réunion équipe Tech")
                        .date(today.plusDays(1))          // J+1
                        .time(LocalTime.of(14, 30))
                        .participants("Lead Dev, DevOps")
                        .notes("Point sprint en cours")
                        .build(),

                Event.builder()
                        .title("Call client Ministère")
                        .date(today.plusDays(2))          // J+2
                        .time(LocalTime.of(11, 0))
                        .participants("Client, Chef de projet")
                        .notes("Revue livrables phase 2")
                        .build(),

                Event.builder()
                        .title("Déjeuner partenaire")
                        .date(today.plusDays(3))          // J+3
                        .time(LocalTime.of(12, 30))
                        .participants("Partenaire externe")
                        .notes("Hôtel Hilton Yaoundé")
                        .build(),

                Event.builder()
                        .title("Revue RH mensuelle")
                        .date(today.plusDays(4))          // J+4
                        .time(LocalTime.of(10, 0))
                        .participants("DRH, Managers")
                        .notes("Évaluations semestrielles")
                        .build()
        );

        eventRepository.saveAll(seedEvents);
        log.info("DataSeeder — {} événements insérés avec succès.", seedEvents.size());
    }
}