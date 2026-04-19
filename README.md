# Assistant de Direction — Inov Consulting

Agent IA backend permettant à un directeur de gérer son agenda et de synthétiser
des documents, le tout en langage naturel via une API REST.

---

## Stack technique & justification

| Composant        | Choix retenu         | Justification |
|------------------|----------------------|---------------|
| **Backend**      | Java 17 + Spring Boot 3.3 | Robustesse entreprise, injection de dépendances mature. |
| **IA Framework** | **Spring AI**        | Abstraction de haut niveau pour les LLM, gestion native du Tool Calling et de la mémoire. |
| **LLM**          | Groq API (llama-3.3-70b) | Tier gratuit, compatible OpenAI, latence ultra-faible. |
| **Base de données** | SQLite + JPA/Hibernate | Zéro infrastructure, idéal pour un test technique. |
| **Documentation**| SpringDoc / Swagger UI | Auto-générée depuis les annotations. |
| **Tests**        | JUnit 5 + Mockito    | Standard Java, intégré Spring Boot Test. |

---

## Prérequis

- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`) — ou utiliser le wrapper `./mvnw`
- Un compte Groq gratuit → [console.groq.com](https://console.groq.com)

---

## 1. Obtenir une clé API Groq (gratuit, sans CB)

1. Rendez-vous sur **https://console.groq.com**
2. Cliquez sur **Sign Up** et créez un compte.
3. Menu gauche → **API Keys** → **Create API Key**.
4. Copiez la clé générée (`gsk_...`).

---

## 2. Configuration des variables d'environnement

```bash
# Copier le fichier exemple
cp .env.example .env

# Éditer .env et renseigner votre clé Groq
nano .env
```

Variable obligatoire dans `.env` :
```
GROQ_API_KEY=gsk_VOTRE_CLE_ICI
```

---

## 3. Lancement

L'application utilise `dotenv-java` pour charger automatiquement le fichier `.env` au démarrage.

```bash
# Lancer l'application
./mvnw spring-boot:run
```

- **Swagger UI** : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Santé de l'API** : [http://localhost:8080/health](http://localhost:8080/health)

---

## 4. Architecture du projet (Refactorisée Spring AI)

Le projet utilise le concept de **Function Calling** de Spring AI. Les outils sont déclarés comme des `@Bean Function` Java simples.

```
src/main/java/com/inovconsulting/assistant/
├── AssistantDirection.java → Point d'entrée & chargement .env
├── controller/        → Endpoints HTTP
│   ├── AgentController.java      (POST /agent/chat)
│   ├── AgendaController.java     (CRUD Agenda)
│   ├── SessionController.java    (Historique session)
│   └── HealthController.java     (Santé & Config)
├── service/           → Logique métier
│   ├── AgentService.java         (Orchestration via ChatClient Spring AI)
│   ├── AgendaService.java        (Gestion de l'agenda SQLite)
│   └── SessionService.java       (Persistance de l'historique en base)
├── tools/             → Fonctions appelées par l'IA (Spring AI Functions)
│   ├── GetAgendaTool.java        (Consultation agenda)
│   ├── CreateEventTool.java      (Création d'événement)
│   └── SummarizeDocumentTool.java (Synthèse via appel LLM secondaire)
├── model/             → Entités JPA et DTOs
├── repository/        → Accès aux données (Spring Data JPA)
├── mapper/            → Conversion Entity <-> DTO
├── exception/         → Gestion des erreurs personnalisées
└── db/                → DataSeeder (données de test automatiques)
```

---

## 5. Exemples d'utilisation

### Consulter l'agenda (via Agent)
```bash
curl -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Quels sont mes rendez-vous de demain ?"}'
```

### Synthèse de document
```bash
curl -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Synthétise ce texte : [votre texte long ici...]"}'
```

---

## 6. Tests

```bash
./mvnw test
```
