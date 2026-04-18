package com.inovconsulting.assistant.controller;

import com.inovconsulting.assistant.model.dto.ChatRequest;
import com.inovconsulting.assistant.model.dto.ChatResponse;
import com.inovconsulting.assistant.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur du point d'entrée principal de l'agent IA.
 * Expose POST /agent/chat.
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Agent IA", description = "Point d'entrée principal de l'assistant de direction")
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    @Operation(
            summary     = "Envoyer un message à l'agent",
            description = "Traite une requête en langage naturel. L'agent active les outils adaptés "
                    + "(get_agenda, create_event, summarize_document) selon le contexte. "
                    + "Le champ session_id est généré automatiquement au premier appel "
                    + "et doit être retransmis pour maintenir le contexte sur plusieurs échanges.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples  = {
                                    @ExampleObject(name  = "Consulter l'agenda",
                                            value = "{\"session_id\": null, \"message\": \"Quels sont mes rendez-vous de demain ?\"}"),
                                    @ExampleObject(name  = "Planifier un événement",
                                            value = "{\"session_id\": null, \"message\": \"Planifie une réunion vendredi à 10h avec l'équipe tech\"}"),
                                    @ExampleObject(name  = "Synthèse de document",
                                            value = "{\"session_id\": null, \"message\": \"synthesize_document Compte-rendu réunion : [votre texte]\"}")
                            }
                    )
            ),
            responses   = {
                    @ApiResponse(responseCode = "200", description = "Réponse de l'agent",
                            content = @Content(schema = @Schema(implementation = ChatResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Requête invalide (message vide)"),
                    @ApiResponse(responseCode = "500", description = "Erreur interne ou LLM indisponible")
            }
    )
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("POST /agent/chat — session={}", request.getSessionId());
        ChatResponse response = agentService.chat(request);
        return ResponseEntity.ok(response);
    }
}