# Stage 1: Build the application using Gradle
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app

# Copy Gradle build files
COPY build.gradle settings.gradle ./

# Copy source code
COPY src ./src

# Build the application boot jar (excluding tests)
RUN gradle bootJar --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
LABEL service="notification-service"
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy the compiled jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", \
            "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
