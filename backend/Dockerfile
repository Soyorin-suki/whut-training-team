FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

ARG MAVEN_CLI_OPTS="-B -q -DskipTests -Dmaven.wagon.http.retryHandler.count=5"

COPY docker-settings.xml /root/.m2/settings.xml
COPY pom.xml ./
RUN mvn ${MAVEN_CLI_OPTS} dependency:go-offline

COPY src ./src

RUN mvn ${MAVEN_CLI_OPTS} package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/training.db \
    SPRING_DATA_REDIS_HOST=redis \
    SPRING_DATA_REDIS_PORT=6379 \
    APP_DAILY_PROBLEM_CACHE_TTL=1d \
    JAVA_OPTS=""

RUN apk add --no-cache wget \
  && mkdir -p /app/data /app/logs

COPY --from=build /workspace/target/*.jar /app/app.jar

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health > /dev/null || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
