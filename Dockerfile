FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
#RUN mvn verify
RUN mvn clean package #incluye los tests

FROM openjdk:21
WORKDIR /app
COPY --from=builder /app/target/api-crowdsensing-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]