# Initial architecture

The root Maven project is an aggregator and parent so all modules share Java 17 and Spring Boot dependency management. `common` is a plain jar to keep contracts reusable and free of application concerns. `coordinator` and `worker` are separate deployable processes, each with its own Spring Boot entry point and port. Docker Compose owns only local PostgreSQL lifecycle and persistence. Distributed coordination, heartbeats, leases, claiming, retries, and failure recovery are intentionally deferred for hands-on implementation.
