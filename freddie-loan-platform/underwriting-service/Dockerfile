FROM eclipse-temurin:17-jre-alpine
LABEL service="underwriting-service"
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q --spider http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-XX:+UseG1GC", \
            "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

