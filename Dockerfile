FROM openjdk:21
EXPOSE 8080
WORKDIR /app
COPY target/api-crowdsensing-0.0.1.jar app.jar
#COPY src/main/resources/response.json /app/resources/response.json
ENTRYPOINT ["java", "-jar", "app.jar"]
