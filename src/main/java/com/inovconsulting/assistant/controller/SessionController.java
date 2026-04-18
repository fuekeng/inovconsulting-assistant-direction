package com.inovconsulting.assistant.controller;

import com.inovconsulting.assistant.model.dto.SessionHistoryEntry;
import com.inovconsulting.assistant.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour la consultation de l'historique de session.
 */
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session", description = "Consultation de la mémoire conversationnelle")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/{id}/history")
    @Operation(
            summary     = "Historique d'une session",
            description = "Retourne la liste complète des échanges (user + assistant) d'une session, "
                    + "dans l'ordre chronologique.",
            responses   = {
                    @ApiResponse(responseCode = "200", description = "Historique retourné (peut être vide si session inconnue)")
            }
    )
    public ResponseEntity<List<SessionHistoryEntry>> getHistory(
            @Parameter(description = "UUID de la session", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id
    ) {
        log.info("GET /session/{}/history", id);
        List<SessionHistoryEntry> history = sessionService.getHistory(id);
        return ResponseEntity.ok(history);
    }
}