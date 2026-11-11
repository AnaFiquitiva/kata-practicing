# ---------- Etapa 1: compilación con Maven + JDK 21 ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Primero solo el pom: las dependencias quedan en caché mientras no cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: ejecución con JRE 21 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: si alguien explotara la aplicación, no tendría root dentro del contenedor.
# UID/GID 1000 coincide con el primer usuario de la mayoría de equipos Linux, así puede escribir en
# los bind mounts de backend_data/ (en Docker Desktop para Windows/Mac los permisos se mapean solos).
RUN addgroup -S -g 1000 app && adduser -S -u 1000 -G app app \
    && mkdir -p /app/data /app/uploads \
    && chown -R app:app /app

COPY --from=builder --chown=app:app /build/target/*.jar app.jar
USER app

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/practicantes.db \
    APP_UPLOADS_DIR=/app/uploads

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
