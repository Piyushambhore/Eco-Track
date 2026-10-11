# ==============================================================================
# EcoTrack Backend - Production Multi-Stage Dockerfile
# Optimized for AWS ECS Fargate & Cloud Native Deployments
# ==============================================================================

# --- Stage 1: Build Environment ---
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy Gradle wrappers and build scripts first for optimal layer caching
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./

# Download dependencies in isolated layer
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and resources
COPY src ./src

# Build production executable shadow JAR / fat JAR
RUN ./gradlew build -x test --no-daemon

# --- Stage 2: Hardened Production Runtime ---
FROM eclipse-temurin:17-jre-jammy AS runtime

# Install curl for healthcheck & dumb-init for proper PID 1 signal forwarding
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Create dedicated non-root application user and group
RUN groupadd -g 10001 ecotrack && \
    useradd -u 10001 -g ecotrack -s /bin/bash -m ecotrack

WORKDIR /app

# Copy application artifact from builder stage
COPY --from=builder --chown=ecotrack:ecotrack /build/build/libs/*-all.jar /app/ecotrack-api.jar

# Switch to non-root user
USER ecotrack:ecotrack

# Expose default HTTP port
EXPOSE 8080

# Environment variables with sensible defaults
ENV PORT=8080 \
    ENVIRONMENT=production \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Container Healthcheck targeting Ktor /health/live endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/health/live || exit 1

# Launch application via dumb-init for graceful termination handling (SIGTERM)
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/ecotrack-api.jar"]
