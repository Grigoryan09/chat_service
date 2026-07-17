FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5434/postgres?currentSchema=chat_service"
ENV DB_USERNAME="postgres"
ENV DB_PASSWORD="postgres"
ENV KAFKA_BOOTSTRAP_SERVERS="host.docker.internal:29092"

EXPOSE 8081 9093

ENTRYPOINT ["java", "-jar", "app.jar"]