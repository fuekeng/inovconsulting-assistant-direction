# ─────────────────────────────────────────────────
#  Stage 1 — Build (Maven + JDK 17)
# ─────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copier uniquement le pom.xml d'abord pour profiter du cache des dépendances Maven
# (Maven est déjà présent dans cette image, contrairement à un JDK nu + apk add maven,
# qui forçait un re-téléchargement du paquet à chaque changement de pom.xml)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et construire le jar
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────────
#  Stage 2 — Runtime (JRE 17 allégé)
# ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Utilisateur non-root dédié à l'exécution du conteneur
RUN addgroup -S spring && adduser -S spring -G spring

# Copier le jar depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Variables d'environnement par défaut (surchargées par docker-compose ou .env)
ENV SERVER_PORT=8080
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=assistant

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]