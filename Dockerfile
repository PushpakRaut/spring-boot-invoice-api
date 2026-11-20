FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
# Run as an unprivileged user, not root
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /app/target/invoice-api-1.0.0.jar app.jar
# Keeps memory under free-tier limits (512 MB)
# prod profile: no in-memory database fallback, DATABASE_URL is required
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
