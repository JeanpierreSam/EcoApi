# Etapa 1: compila el jar con Maven (no hace falta tener Java instalado)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: solo el JRE y el jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ecoapi-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
