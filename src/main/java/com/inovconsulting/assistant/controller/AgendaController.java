package com.inovconsulting.assistant.controller;

import com.inovconsulting.assistant.model.dto.EventRequest;
import com.inovconsulting.assistant.model.dto.EventResponse;
import com.inovconsulting.assistant.service.AgendaServiceImp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur REST pour la gestion directe de l'agenda (sans passer par l'agent).
 * Expose GET, POST, PATCH, DELETE sur /agenda.
 */
@RestController
@RequestMapping("/agenda")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Agenda", description = "Gestion directe des événements calendrier")
public class AgendaController {

    private final AgendaServiceImp agendaService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /agenda
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary     = "Lister les événements",
            description = "Retourne tous les événements. Filtrable par ?date=YYYY-MM-DD ou ?range=week.",
            responses   = @ApiResponse(responseCode = "200", description = "Liste des événements")
    )
    public ResponseEntity<List<EventResponse>> getEvents(
            @Parameter(description = "Filtrer par date exacte, format YYYY-MM-DD", example = "2026-04-18")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Plage temporelle : 'week' pour les 7 prochains jours", example = "week")
            @RequestParam(required = false) String range
    ) {
        log.info("GET /agenda — date={}, range={}", date, range);
        return ResponseEntity.ok(agendaService.getEvents(date, range));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /agenda
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary   = "Créer un événement",
            description = "Crée un nouvel événement et retourne l'objet créé avec son id.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Événement créé"),
                    @ApiResponse(responseCode = "400", description = "Données invalides")
            }
    )
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest request) {
        log.info("POST /agenda — title={}", request.getTitle());
        EventResponse created = agendaService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /agenda/{id}  (BONUS)
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}")
    @Operation(
            summary   = "Modifier un événement (partiel)",
            description = "Met à jour un ou plusieurs champs d'un événement existant. "
                    + "Seuls les champs fournis dans le corps sont modifiés.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Événement mis à jour"),
                    @ApiResponse(responseCode = "404", description = "Événement introuvable")
            }
    )
    public ResponseEntity<EventResponse> updateEvent(
            @Parameter(description = "Identifiant de l'événement", example = "1")
            @PathVariable Long id,
            @RequestBody EventRequest request
    ) {
        log.info("PATCH /agenda/{} — champs modifiés", id);
        return ResponseEntity.ok(agendaService.updateEvent(id, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /agenda/{id}  (BONUS)
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(
            summary   = "Supprimer un événement",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Événement supprimé"),
                    @ApiResponse(responseCode = "404", description = "Événement introuvable")
            }
    )
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "Identifiant de l'événement", example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /agenda/{}", id);
        agendaService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}