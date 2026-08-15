FROM maven:3-eclipse-temurin-26-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S clinic \
    && adduser -S -G clinic -h /app clinic

WORKDIR /app
COPY --from=build --chown=clinic:clinic /workspace/target/dental-clinic-*.jar /app/application.jar

USER clinic:clinic
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/application.jar"]
