# Build the worker and its shared module from the repository root context.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY coordinator/pom.xml coordinator/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY common/src common/src
COPY worker/src worker/src
RUN mvn -pl worker -am package -DskipTests

# Run the packaged Spring Boot worker.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/worker/target/worker-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
