FROM maven:3.10.0-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn --batch-mode clean package -DskipTests

FROM eclipse-temurin:25-jre

RUN groupadd --gid 10001 appgroup \
    && useradd --uid 10001 --gid appgroup \
        --home-dir /app --no-create-home \
        --shell /usr/sbin/nologin appuser \
    && mkdir -p /app \
    && chown appuser:appgroup /app
WORKDIR /app

COPY --from=build --chown=appuser:appgroup \
    /workspace/target/catalog-sync-service-*.jar \
    /app/catalog-sync-service.jar

EXPOSE 8081

USER appuser

ENTRYPOINT ["java", "-jar", "/app/catalog-sync-service.jar"]
