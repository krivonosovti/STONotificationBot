# syntax=docker/dockerfile:1
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew
COPY src src
RUN --mount=type=cache,target=/home/gradle/.gradle ./gradlew --no-daemon test bootJar

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/andryukha.jar app.jar
USER app
EXPOSE 8080 9090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
