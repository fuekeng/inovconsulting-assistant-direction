package com.inovconsulting.assistant;

import com.inovconsulting.assistant.db.DataSeeder;
import com.inovconsulting.assistant.model.entity.Event;
import com.inovconsulting.assistant.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de DataSeeder : le seed ne doit s'exécuter que si la table est vide,
 * et doit insérer les 5 événements attendus avec des dates relatives à aujourd'hui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder — Tests unitaires")
class DataSeederTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    @DisplayName("run — n'insère rien si des événements existent déjà")
    void run_skipsSeedingWhenEventsAlreadyExist() throws Exception {
        when(eventRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("run — insère 5 événements avec des dates relatives à aujourd'hui quand la table est vide")
    void run_seedsFiveEventsWhenEmpty() throws Exception {
        when(eventRepository.count()).thenReturn(0L);

        dataSeeder.run();

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository, times(1)).saveAll(captor.capture());

        List<Event> seeded = captor.getValue();
        LocalDate today = LocalDate.now();

        assertThat(seeded).hasSize(5);
        assertThat(seeded).allSatisfy(event -> {
            assertThat(event.getTitle()).isNotBlank();
            assertThat(event.getTime()).isNotNull();
        });
        assertThat(seeded.get(0).getDate()).isEqualTo(today.plusDays(1));
        assertThat(seeded.get(2).getDate()).isEqualTo(today.plusDays(2));
        assertThat(seeded.get(3).getDate()).isEqualTo(today.plusDays(3));
        assertThat(seeded.get(4).getDate()).isEqualTo(today.plusDays(4));
    }
}
