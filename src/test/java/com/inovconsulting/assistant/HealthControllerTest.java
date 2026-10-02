package com.inovconsulting.assistant;

import com.inovconsulting.assistant.controller.HealthController;
import com.inovconsulting.assistant.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de HealthController — vérifie le statut retourné selon
 * l'accessibilité de la base de données.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController — Tests unitaires")
class HealthControllerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(healthController, "groqModel", "llama-3.3-70b-versatile");
    }

    @Test
    @DisplayName("health — retourne UP avec le nombre d'événements quand la base est accessible")
    void health_returnsUpWhenDatabaseAccessible() {
        when(eventRepository.count()).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("UP");
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) body.get("database");
        assertThat(database.get("status")).isEqualTo("UP");
        assertThat(database.get("events_count")).isEqualTo(5L);
        @SuppressWarnings("unchecked")
        Map<String, Object> llm = (Map<String, Object>) body.get("llm");
        assertThat(llm.get("model")).isEqualTo("llama-3.3-70b-versatile");
    }

    @Test
    @DisplayName("health — retourne DEGRADED si la base lève une exception")
    void health_returnsDegradedWhenDatabaseThrows() {
        when(eventRepository.count()).thenThrow(new RuntimeException("connexion refusée"));

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("DEGRADED");
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) body.get("database");
        assertThat(database.get("status")).isEqualTo("DOWN");
        assertThat(database.get("error")).isEqualTo("connexion refusée");
    }
}
