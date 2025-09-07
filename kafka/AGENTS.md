# Repository Guidelines

## Project Structure & Module Organization
- `src/main/kotlin/...` — Application code (Kotlin, Spring Boot, Kafka).
- `src/main/resources/` — Configuration (e.g., `application.properties`).
- `src/test/kotlin/...` — Tests (JUnit 5 + Spring Boot Test, spring-kafka-test).
- Build tooling: `build.gradle.kts`, `settings.gradle.kts`, Gradle wrapper `./gradlew`.
- Outputs: `build/` (classes, reports, and `build/libs/*.jar`).

## Build, Test, and Development Commands
- `./gradlew build` — Compile, run tests, and assemble artifacts.
- `./gradlew test` — Run unit/integration tests (JUnit Platform).
- `./gradlew bootRun` — Run the application locally.
- `./gradlew clean` — Remove build outputs.
- Example: run a single test class — `./gradlew test --tests 'com.example.kafka.KafkaApplicationTests'`.

## Coding Style & Naming Conventions
- Language: Kotlin with Spring. Follow idiomatic Kotlin style.
- Indentation: 4 spaces; keep lines reasonably short (~120 chars).
- Packages: lowercase domains, e.g., `com.example.kafka`.
- Types: `PascalCase` for classes/objects; `camelCase` for functions/vars; constants `UPPER_SNAKE_CASE`.
- Files should match the primary type name, e.g., `KafkaProducer.kt`.
- Prefer data classes, null-safety, and constructor injection.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test, spring-kafka-test.
- Location: mirror source packages under `src/test/kotlin`.
- Naming: `*Test.kt` or `*Tests.kt` with clear, behavior-focused test names.
- Use slice or unit tests where possible; use embedded Kafka utilities when needed.
- Run locally: `./gradlew test`. Review reports under `build/reports/tests/tests/index.html`.

## Commit & Pull Request Guidelines
- Commits: imperative mood with concise subject (<72 chars), optional body for context.
  - Example: `Add Kafka consumer configuration and topic beans`.
- PRs: link issues, describe motivation and changes, include testing notes; keep changes focused.
- Require green `./gradlew build` before merge.

## Security & Configuration Tips
- Never commit secrets. Use environment variables or a local `application-*.properties` excluded from VCS.
- Kafka config: set `spring.kafka.bootstrap-servers` (e.g., `localhost:9092`) or env `SPRING_KAFKA_BOOTSTRAP_SERVERS`.
- Use Spring profiles (`spring.profiles.active`) for environment-specific settings.

