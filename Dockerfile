FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Production JVM settings
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

COPY target/*.jar app.jar
EXPOSE 8085

# Run as non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
