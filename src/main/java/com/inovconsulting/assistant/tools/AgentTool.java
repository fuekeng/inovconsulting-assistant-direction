package com.inovconsulting.assistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contrat que tout outil de l'agent doit implémenter.
 *
 * Chaque outil expose :
 *  - son nom unique (utilisé dans tool_used)
 *  - son schéma JSON au format OpenAI function calling (envoyé au LLM)
 *  - sa logique d'exécution à partir des arguments JSON fournis par le LLM
 */
public interface AgentTool {

    /** Nom unique de l'outil, identique à "name" dans le schéma. */
    String getName();

    /**
     * Retourne le schéma JSON de la fonction au format OpenAI :
     * { "name": "...", "description": "...", "parameters": { ... } }
     */
    ObjectNode getSchema();

    /**
     * Exécute l'outil avec les arguments JSON fournis par le LLM.
     * Retourne une chaîne JSON (résultat sérialisé) que l'agent
     * transmettra au LLM comme réponse d'outil.
     *
     * @param arguments nœud JSON des arguments extraits de la réponse du LLM
     * @return résultat sérialisé en JSON
     */
    String execute(JsonNode arguments);
}