# Assistant de Direction — Inov Consulting

Agent IA backend permettant à un directeur de gérer son agenda et de synthétiser
des documents, le tout en langage naturel via une API REST.

---

## Stack technique & justification

| Composant        | Choix retenu         | Justification |
|------------------|----------------------|---------------|
| **Backend**      | Java 17 + Spring Boot 3 | Robustesse entreprise, injection de dépendances mature, auto-configuration JPA |
| **LLM**          | Groq API (llama-3.3-70b) | Tier gratuit, support natif du tool calling OpenAI-compatible, latence < 1s |
| **Base de données** | SQLite + JPA/Hibernate | Zéro infrastructure, seed immédiat, idéal pour un test technique |
| **Documentation**| SpringDoc / Swagger UI | Auto-générée depuis les annotations, toujours synchronisée |
| **Tests**        | JUnit 5 + Mockito    | Standard Java, intégré Spring Boot Test |

---

## Prérequis

- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`) — ou utiliser le wrapper `./mvnw`
- Un compte Groq gratuit → [console.groq.com](https://console.groq.com) (2 minutes)

---

## 1. Obtenir une clé API Groq (gratuit, sans CB)

1. Rendez-vous sur **https://console.groq.com**
2. Cliquez sur **Sign Up** et créez un compte (email + mot de passe)
3. Dans le menu gauche → **API Keys** → **Create API Key**
4. Copiez la clé générée (elle commence par `gsk_...`)

> Le tier gratuit Groq est largement suffisant pour ce projet (~14 400 tokens/min sur llama-3.3-70b).

---

## 2. Configuration des variables d'environnement

```bash
# Copier le fichier exemple
cp .env.example .env

# Éditer .env et renseigner votre clé Groq
nano .env   # ou code .env
```

Variables obligatoires dans `.env` :

```
GROQ_API_KEY=gsk_VOTRE_CLE_ICI
```

Les autres variables ont des valeurs par défaut et sont optionnelles.

---

## 3. Lancement (sans Docker)

```bash
# Cloner et entrer dans le projet
git clone <url-du-repo>
cd inovconsulting-assistant-direction


# Charger les variables d'environnement
export $(grep -v '^#' .env | xargs)

# Lancer l'application (le seed s'exécute automatiquement)
./mvnw spring-boot:run
```

L'application démarre sur **http://localhost:8080**.
Swagger UI disponible sur : **http://localhost:8080/swagger-ui.html**

---

## 4. Lancement avec Docker (bonus)

```bash
# Copier et configurer .env (voir étape 2)
cp .env.example .env && nano .env

# Lancer
docker-compose up --build

# Arrêter
docker-compose down
```

---

## 5. Vérification rapide

```bash
# Santé de l'API en local
curl http://localhost:8080/health

# Agenda de la semaine
curl "http://localhost:8080/agenda?range=week"
```

---

## 6. Exemples de requêtes curl

### Consulter l'agenda

```bash
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Quels sont mes rendez-vous de demain ?"}' \
  | jq .
```

Réponse attendue :
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "response": "Vous avez 2 rendez-vous demain : un Comité de direction à 9h00 avec DG, DAF, DSI (Budget Q2 à valider) et une Réunion équipe Tech à 14h30 avec Lead Dev, DevOps.",
  "tool_used": "get_agenda",
  "turn": 1
}
```

### Planifier un événement

```bash
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Planifie une réunion vendredi à 10h avec léquipe tech pour le sprint review"}' \
  | jq .
```

### Conversation multi-tours (mémoire de session)

```bash
# Tour 1 — démarrer une session
SESSION=$(curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Résume ma semaine à venir"}' \
  | jq -r '.session_id')

# Tour 2 — continuer la même session
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d "{\"session_id\": \"$SESSION\", \"message\": \"Quel est le premier rendez-vous ?\"}" \
  | jq .
```

### Synthèse de document

```bash
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": null,
    "message": "Synthétise ce document : Compte-rendu réunion du 15 avril. Présents : DG, DAF, DSI. Points abordés : 1) Validation budget Q2 approuvée à 85%. 2) Recrutement de 3 développeurs décidé pour juin. 3) Migration cloud à planifier avant Q3. Action : DAF prépare les bons de commande avant le 30 avril. DSI rédige le cahier des charges migration."
  }' \
  | jq .
```

### Historique de session

```bash
curl -s http://localhost:8080/session/$SESSION/history | jq .
```

### Créer un événement directement (REST)

```bash
curl -s -X POST http://localhost:8080/agenda \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Réunion Board",
    "date": "2026-04-25",
    "time": "09:00",
    "participants": "DG, Board membres",
    "notes": "Revue stratégique annuelle"
  }' | jq .
```

---

## 7. Endpoints disponibles

| Méthode | Route                   | Description                          | Priorité |
|---------|-------------------------|--------------------------------------|----------|
| POST    | `/agent/chat`           | Point d'entrée principal de l'agent  | Core     |
| GET     | `/agenda`               | Lister les événements (filtrables)   | Core     |
| POST    | `/agenda`               | Créer un événement                   | Core     |
| GET     | `/session/{id}/history` | Historique d'une session             | Core     |
| DELETE  | `/agenda/{id}`          | Supprimer un événement               | Bonus    |
| PATCH   | `/agenda/{id}`          | Modifier partiellement un événement  | Bonus    |
| GET     | `/health`               | Statut de l'API et de la DB          | Bonus    |

---

## 8. Lancer les tests

```bash
./mvnw test
```

---

## 9. Architecture du projet

```
src/
├── controller/        → Endpoints HTTP (routes uniquement, pas de logique)
│   ├── AgentController.java      POST /agent/chat
│   ├── AgendaController.java     GET|POST|PATCH|DELETE /agenda
│   ├── SessionController.java    GET /session/{id}/history
│   └── HealthController.java     GET /health
├── service/           → Logique métier & orchestration
│   ├── AgentService.java         Orchestrateur principal (tool calling loop)
│   ├── AgendaService.java        CRUD agenda + validation
│   ├── SessionService.java       Mémoire conversationnelle
│   └── GroqClient.java           Client HTTP bas niveau vers Groq
├── tools/             → Outils appelés par l'agent (tool calling)
│   ├── AgentTool.java            Interface contrat
│   ├── GetAgendaTool.java        Outil 01a — consultation agenda
│   ├── CreateEventTool.java      Outil 01b — création événement
│   ├── SummarizeDocumentTool.java Outil 02  — synthèse document
│   └── ToolRegistry.java         Registre & dispatcher des outils
├── model/
│   ├── entity/        → Entités JPA (Event, SessionMessage)
│   └── dto/           → DTOs requêtes/réponses
├── repository/        → Interfaces JpaRepository
├── db/                → DataSeeder (données de départ)
└── config/            → AppConfig, GlobalExceptionHandler
```

---
