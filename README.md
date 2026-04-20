# Assistant de Direction — Inov Consulting

Agent IA backend permettant à un directeur de gérer son agenda et de synthétiser
des documents, le tout en langage naturel via une API REST.

---

## Stack technique & justification

| Composant        | Choix retenu         | Justification |
|------------------|----------------------|---------------|
| **Backend**      | Java 17 + Spring Boot 3.3 | Robustesse entreprise, injection de dépendances mature. |
| **IA Framework** | **Spring AI**        | Abstraction de haut niveau, gestion native du Tool Calling et de la mémoire. |
| **LLM**          | Groq API (llama-3.3-70b) | Tier gratuit, compatible OpenAI, latence ultra-faible. |
| **Base de données** | SQLite + JPA/Hibernate | Zéro infrastructure, idéal pour un test technique. |
| **Documentation**| SpringDoc / Swagger UI | Auto-générée depuis les annotations. |
| **Tests**        | JUnit 5 + Mockito    | Standard Java, intégré Spring Boot Test. |

---

## Prérequis

- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`) — ou utiliser le wrapper `./mvnw`
- Un compte Groq gratuit → [console.groq.com](https://console.groq.com) (2 minutes)

---

## 1. Obtenir une clé API Groq (gratuit, sans CB)

1. Rendez-vous sur **https://console.groq.com**
2. Cliquez sur **Sign Up** et créez un compte.
3. Dans le menu gauche → **API Keys** → **Create API Key**
4. Copiez la clé générée (elle commence par `gsk_...`)

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

---

## 3. Lancement (sans Docker)

```bash
# Cloner et entrer dans le projet
git clone <url-du-repo>
cd inovconsulting-assistant-direction

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
# Santé de l'API en local (Vérifie la config Spring AI)
curl http://localhost:8080/health

# Agenda de la semaine (Accès direct REST)
curl "http://localhost:8080/agenda?range=week"
```

---

## 6. Exemples de requêtes curl

### Consulter l'agenda via l'Agent IA

```bash
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Quels sont mes rendez-vous de demain ?"}' \
  | jq .
```

### Planifier un événement (Tool Calling automatique)

```bash
curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Planifie une réunion vendredi à 10h avec l'équipe tech"}' \
  | jq .
```

### Conversation multi-tours (Mémoire Spring AI)

```bash
# Tour 1 — démarrer une session
SESSION=$(curl -s -X POST http://localhost:8080/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"session_id": null, "message": "Résume ma semaine à venir"}' \
  | jq -r '.session_id')

# Tour 2 — continuer la même session (contexte conservé par l'advisor)
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
    "message": "Synthétise ce document : [Texte du compte-rendu...]"
  }' \
  | jq .
```

---

## 7. Endpoints disponibles

| Méthode | Route                   | Description                          |
|---------|-------------------------|--------------------------------------|
| POST    | `/agent/chat`           | Point d'entrée de l'agent (Spring AI)|
| GET     | `/agenda`               | Lister les événements                |
| POST    | `/agenda`               | Créer un événement (REST direct)     |
| GET     | `/session/{id}/history` | Historique persistant                |
| GET     | `/health`               | Statut (DB + Config LLM)             |

---

## 8. Lancer les tests

```bash
./mvnw test
```

---

## 9. Architecture du projet

```
src/main/java/com/inovconsulting/assistant/
├── controller/        → Endpoints HTTP
│   ├── AgentController.java      (Chat via Spring AI)
│   ├── AgendaController.java     (CRUD Agenda)
│   └── HealthController.java     (Santé & Config LLM)
├── service/           → Logique métier
│   ├── AgentService.java         (Orchestration ChatClient + Advisors)
│   ├── AgendaService.java        (Gestion Agenda SQLite)
│   └── SessionService.java       (Gestion des sessions et historique)
├── tools/             → Fonctions Spring AI (Tool Calling)
│   ├── GetAgendaTool.java        (@Bean Function - Consultation)
│   ├── CreateEventTool.java      (@Bean Function - Création)
│   └── SummarizeDocumentTool.java (@Bean Function - Synthèse)
├── model/             → Entités JPA et DTOs
├── repository/        → Interfaces JpaRepository
└── db/                → DataSeeder (Données de test)
```
