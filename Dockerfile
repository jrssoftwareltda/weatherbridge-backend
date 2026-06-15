FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /workspace

COPY pom.xml ./

RUN mvn \
    --batch-mode \
    --no-transfer-progress \
    dependency:go-offline

COPY src ./src

RUN mvn \
    --batch-mode \
    --no-transfer-progress \
    clean package \
    -DskipTests

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

COPY --from=builder \
    --chown=1001:1001 \
    /workspace/target/*.jar \
    /app/application.jar

USER 1001:1001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
