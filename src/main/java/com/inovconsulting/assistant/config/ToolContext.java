package com.inovconsulting.assistant.config;

/**
 * Permet de capturer le nom de l'outil utilisé pendant l'exécution d'une requête ChatClient.
 */
public class ToolContext {
    private static final ThreadLocal<String> lastToolUsed = new ThreadLocal<>();

    public static void setToolName(String name) {
        lastToolUsed.set(name);
    }

    public static String getToolName() {
        return lastToolUsed.get();
    }

    public static void clear() {
        lastToolUsed.remove();
    }
}
