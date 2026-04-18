package com.inovconsulting.assistant.controller;

import com.inovconsulting.assistant.repository.EventRepository;
import com.inovconsulting.assistant.tools.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de santé — vérifie l'état de l'API et de la base de données.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health", description = "Statut de l'application")
public class HealthController {

    private final EventRepository eventRepository;
    private final ToolRegistry    toolRegistry;

    private static final String STATUS_KEY ="status";

    @Value("${groq.model}")
    private String groqModel;

    @GetMapping
    @Operation(
            summary     = "Statut de l'API",
            description = "Vérifie la connectivité à la base de données et retourne les informations de configuration."
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put(STATUS_KEY,    "UP");
        status.put("timestamp", Instant.now().toString());

        // Vérification base de données
        try {
            long count = eventRepository.count();
            status.put("database", Map.of(STATUS_KEY, "UP", "events_count", count));
        } catch (Exception e) {
            log.error("HealthController — DB inaccessible : {}", e.getMessage());
            status.put("database", Map.of(STATUS_KEY, "DOWN", "error", e.getMessage()));
            status.put(STATUS_KEY, "DEGRADED");
        }

        // Configuration LLM (sans exposer la clé)
        status.put("llm", Map.of("provider", "Groq", "model", groqModel));

        // Outils enregistrés
        status.put("tools", toolRegistry.getToolNames());

        return ResponseEntity.ok(status);
    }
}