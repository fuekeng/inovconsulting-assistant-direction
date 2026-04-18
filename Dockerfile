# ─────────────────────────────────────────────────
#  Stage 1 — Build (Maven + JDK 17)
# ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copier uniquement le pom.xml d'abord pour profiter du cache des dépendances Maven
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B

# Copier le code source et construire le jar
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────────
#  Stage 2 — Runtime (JRE 17 allégé)
# ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Répertoire pour la base SQLite (monté en volume)
RUN mkdir -p /app/data

# Copier le jar depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Variables d'environnement par défaut (surchargées par docker-compose ou .env)
ENV SERVER_PORT=8080
ENV DB_PATH=/app/data/assistant.db

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]