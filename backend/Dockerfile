# ═══════════════════════════════════════════════════════════
#  Dockerfile — Full-stack: React + Spring Boot
#
#  Stage 1: Build del frontend React
#  Stage 2: Build del backend Spring Boot (incluye frontend)
#  Stage 3: Runtime ligero con JRE 21
#
#  IMPORTANTE: El build context debe ser la raíz del repo (.)
#              no ./backend. Ver CD_*.yml.
#
#  El frontend se sirve como archivos estáticos desde
#  Spring Boot (resources/static/).
#  Una sola URL para todo → sin CORS, sin Static Web Apps.
# ═══════════════════════════════════════════════════════════

# ── Stage 1: Build Frontend ────────────────────────────────
FROM node:24-slim AS frontend-build

WORKDIR /frontend

# 1. Copiar package.json y lock primero (cachear dependencias)
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# 2. Copiar código y compilar
#    REACT_APP_API_URL vacío = usa la misma URL base (mismo origen)
COPY frontend/ ./
ENV REACT_APP_API_URL=""
RUN npm run build

# ── Stage 2: Build Backend ─────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS backend-build

WORKDIR /app

# 1. Copiar Maven Wrapper y pom.xml primero (cachear dependencias)
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw

# 2. Descargar dependencias (capa cacheada si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# 3. Copiar código fuente del backend
COPY backend/src/ src/

# 4. Copiar build del frontend a resources/static
COPY --from=frontend-build /frontend/build/ src/main/resources/static/

# 5. Compilar el JAR (incluye frontend como recursos estáticos)
RUN ./mvnw package -DskipTests -B

# ── Stage 3: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Instalar curl para health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copiar solo el JAR generado
COPY --from=backend-build /app/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Health check para Azure App Service
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
