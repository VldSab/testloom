# Testloom

Turn real HTTP flows into reproducible Spring Boot integration tests.

## Status

Testloom is in early development.

What exists today:
- Gradle multi-module monorepo skeleton
- Base modules: `testloom-core`, `testloom-spring-boot-starter`, `testloom-cli`, `testloom-examples`

What is not released yet:
- Published artifacts
- Working recorder/generator commands
- Production-ready starter behavior

## MVP scope

- Spring Boot support
- REST/HTTP capture at application boundary
- Spring MVC as the first runtime path
- PostgreSQL fixture workflow
- Local/dev/staging capture only
- Template-based integration test generation

Transport strategy:
- MVP: HTTP only
- Next protocol track: gRPC (instead of prioritizing WebFlux)

## Repository layout

```text
testloom/
  testloom-core/
  testloom-spring-boot-starter/
  testloom-cli/
  testloom-examples/
    mvc-postgres-demo/
    grpc-postgres-demo/
```

Module responsibilities:
- `testloom-core`: capture model, redaction, fixture model, generation model, use cases
- `testloom-spring-boot-starter`: Spring Boot integration and safe capture hooks
- `testloom-cli`: user-facing commands that orchestrate core use cases
- `testloom-examples`: demo apps for validation and documentation

## Planned workflow

1. Run a Spring app locally with Testloom starter enabled.
2. Perform a real API call.
3. Save a sanitized capture envelope JSON.
4. Generate an integration test and resources from the capture.
5. Run generated tests in CI.

## MVP command surface (planned)

```text
testloom init
testloom inspect --capture <file>
testloom generate --capture <file> --out <dir>
testloom db dump --tables <...> --out <file>
```

## Build and inspect locally

```bash
./gradlew projects
./gradlew test
```

## Non-goals for MVP

- Kafka capture
- gRPC capture in MVP runtime
- Distributed trace backend
- SaaS dashboard
- Automatic full fixture minimization

## Contributing

Contributions are welcome. Open an issue before large changes so scope and direction stay aligned.
