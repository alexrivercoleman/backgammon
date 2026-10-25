# syntax=docker/dockerfile:1

# Build: resolve dependencies first (cached in a BuildKit mount) so code changes don't re-download them, then
# package without tests (they need Docker for Testcontainers; run them with `make verify`).
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/tavli-*.jar extract --layers --launcher --destination /extracted

# Run: the extracted layers, least to most frequently changing, so rebuilds reuse the dependency layers.
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S tavli && adduser -S -G tavli tavli
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER tavli
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
