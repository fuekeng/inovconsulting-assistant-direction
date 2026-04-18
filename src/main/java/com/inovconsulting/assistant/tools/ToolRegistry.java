package com.inovconsulting.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inovconsulting.assistant.service.GroqClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre centralisé de tous les outils disponibles pour l'agent.
 *
 * Responsabilités :
 *  - Initialiser et référencer chaque outil par son nom
 *  - Fournir les schémas JSON à envoyer au LLM lors de chaque appel
 *  - Router l'exécution vers le bon outil à partir du nom retourné par le LLM
 *
 * Pour ajouter un outil : implémenter AgentTool, l'injecter ici, et l'enregistrer dans init().
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolRegistry {

    private final GetAgendaTool    getAgendaTool;
    private final CreateEventTool  createEventTool;
    private final GroqClient       groqClient;
    private final ObjectMapper     objectMapper;

    /** Map nom → instance d'outil */
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // Outils agenda
        register(getAgendaTool);
        register(createEventTool);

        // Outil synthèse — instancié manuellement pour éviter la dépendance circulaire
        register(new SummarizeDocumentTool(groqClient, objectMapper));

        log.info("ToolRegistry — {} outils enregistrés : {}", tools.size(), tools.keySet());
    }

    private void register(AgentTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * Retourne les schémas JSON de tous les outils enregistrés.
     * Ce sont ces schémas qui sont envoyés au LLM pour le guider.
     */
    public List<ObjectNode> getAllSchemas() {
        return tools.values().stream()
                .map(AgentTool::getSchema)
                .toList();
    }

    /**
     * Exécute l'outil identifié par son nom avec les arguments JSON fournis.
     *
     * @param toolName  nom de l'outil (retourné par le LLM)
     * @param arguments nœud JSON des arguments extraits de la réponse LLM
     * @return résultat sérialisé en JSON à retransmettre au LLM
     * @throws IllegalArgumentException si l'outil est inconnu
     */
    public String execute(String toolName, com.fasterxml.jackson.databind.JsonNode arguments) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            log.warn("ToolRegistry — outil inconnu demandé par le LLM : '{}'", toolName);
            return "{\"error\": \"Outil inconnu : " + toolName + "\"}";
        }
        log.info("ToolRegistry — exécution de l'outil '{}'", toolName);
        return tool.execute(arguments);
    }

    /** Liste tous les outils enregistrés (pour monitoring / health check). */
    public Collection<String> getToolNames() {
        return tools.keySet();
    }
}