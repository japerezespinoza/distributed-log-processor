# Distributed Log Processor

Initial Java 17 / Spring Boot multi-module foundation. `common` holds shared, dependency-light domain types; `coordinator` and `worker` are independently runnable Spring Boot applications. Neither application contains distributed coordination behavior yet.

## Build and run

```bash
mvn clean verify
docker compose up -d postgres
mvn -pl coordinator spring-boot:run
mvn -pl worker spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Check `http://localhost:8080/status` and `http://localhost:8081/status`.

PostgreSQL is available at `localhost:5432` with database/user/password `logprocessor`.
