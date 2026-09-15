# Build the coordinator and its shared module from the repository root context.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY coordinator/pom.xml coordinator/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY common/src common/src
COPY coordinator/src coordinator/src
RUN mvn -pl coordinator -am package -DskipTests

# Run the packaged Spring Boot coordinator.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/coordinator/target/coordinator-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
