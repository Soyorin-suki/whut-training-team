FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/training.db

RUN apk add --no-cache wget \
  && mkdir -p /app/data /app/logs

COPY --from=build /workspace/target/*.jar /app/app.jar

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health > /dev/null || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
