# ETAPA 1: BUILD
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Las pruebas se ejecutan en el pipeline de GitHub Actions (job "test").
RUN mvn -B clean package -DskipTests

# ETAPA 2: RUNTIME
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Se ejecuta con un usuario sin privilegios (no root).
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
