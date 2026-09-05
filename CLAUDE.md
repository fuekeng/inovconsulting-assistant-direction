# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Java/Spring Boot backend AI agent ("Assistant de Direction") that lets a director manage their agenda and summarize
documents via natural language, through a REST API. Built with Spring AI on top of the Groq API (OpenAI-compatible,
`llama-3.3-70b-versatile`). Note: the codebase, commit messages, and API responses are primarily in French — match
that when editing prompts, comments, or user-facing strings in this project.

## Commands

```bash
# Run the app (loads .env automatically via dotenv-java, seeds DB on startup)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AgendaServiceTest

# Run a single test method
./mvnw test -Dtest=AgendaServiceTest#getEventsByDate_returnsEventsForDate

# Build
./mvnw clean package

# Docker
cp .env.example .env   # then fill in GROQ_API_KEY
docker-compose up --build
```

Setup requires a `.env` file (copy from `.env.example`) with `GROQ_API_KEY` from https://console.groq.com. The app
starts on `http://localhost:8080`; Swagger UI is at `/swagger-ui.html`.

## Architecture

**Package root:** `com.inovconsulting.assistant`

- `controller/` — HTTP endpoints (`AgentController` for chat, `AgendaController` for CRUD, `HealthController`,
  `SessionController` for history).
- `service/` — business logic. `AgentService` orchestrates the Spring AI `ChatClient`; `AgendaService`/
  `AgendaServiceImp` handle agenda CRUD against SQLite; `SessionService` manages session IDs and persisted history.
- `tools/` — Spring AI function-calling tools, each a `@Configuration` class exposing a `@Bean Function<Request,
  Response>` (e.g. `get_agenda`, `create_event`, `summarize_document`). Tool names are snake_case and are registered
  explicitly in `AgentService` via `.defaultFunctions(...)`. Adding a new tool means: create the `@Bean Function`
  here, add its name to `defaultFunctions`, and mention it in the system prompt if the LLM needs guidance on when to
  call it.
- `model/entity/` — JPA entities (`Event`, `SessionMessage`). `model/dto/` — request/response DTOs.
- `repository/` — Spring Data `JpaRepository` interfaces.
- `mapper/` — entity↔DTO conversion (`EventMapper`).
- `db/DataSeeder` — seeds sample data into SQLite on startup.
- `config/AppConfig` — shared beans (`RestTemplate`, `ObjectMapper`, OpenAPI/Swagger metadata).
- `config/ToolContext` — a `ThreadLocal<String>` used to capture which tool name was invoked during a single
  `ChatClient` call, since Spring AI's function-calling doesn't otherwise surface this. Each tool bean calls
  `ToolContext.setToolName(...)` at the start of its function body; `AgentService.chat()` reads it after the call
  completes and clears it in a `finally` block. Any new tool must follow this same pattern to have its usage
  reported back in `ChatResponse.toolUsed`.
- `exception/` — custom exceptions (`GroqApiException`, `ResourceNotFoundException`).

**Agent flow:** `AgentController` → `AgentService.chat()` builds a `ChatClient` request with the system prompt
(instructing the agent to always call tools rather than answer from memory on agenda questions), per-session chat
memory (`MessageChatMemoryAdvisor` + `InMemoryChatMemory`, keyed by `session_id`), and the registered tool functions.
Spring AI invokes tool functions synchronously when the LLM requests them. After the call, the user/assistant
messages are persisted via `SessionService`, and the response includes which tool (if any) was used.

**Persistence:** MySQL via JPA/Hibernate (`mysql-connector-j` driver, dialect auto-detected by Hibernate), `ddl-auto=update`. Connection
is configured via `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` env vars (defaults: `localhost`/`3306`/
`assistant`/`root`); the target database (e.g. `assistant`) must already exist locally — Hibernate only manages
tables, not the schema/database itself.

**Config:** environment variables are defined in `.env` (gitignored, loaded manually in `main()` via
`dotenv-java` into system properties before `SpringApplication.run`) and consumed in
`src/main/resources/application.properties`. See `.env.example` for the full list (`SERVER_PORT`, `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `GROQ_API_KEY`, `GROQ_API_URL`, `GROQ_MODEL`, `GROQ_MAX_TOKENS`,
`SESSION_MAX_TURNS`, `LOG_LEVEL`).
