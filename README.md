# Testloom

Turn real HTTP flows into reproducible Spring Boot integration tests.

## Status

Testloom is in early development.

Implemented now:
- Multi-module Gradle monorepo
- Runnable CLI (`testloom`) with `--help` and `init`
- Config template generation (`testloom.yaml`)
- YAML config loader in `testloom-core`
- Redaction rules model + normalization + validation
- DDD-style package organization for config domain

## MVP scope

- Spring Boot support
- REST/HTTP capture at application boundary
- Spring MVC as the first runtime path
- PostgreSQL fixture workflow
- Local/dev/staging capture only
- Template-based integration test generation

Not released yet:
- Published artifacts
- Full runtime HTTP capture in starter
- Test generation pipeline

## Quick start

Build:

```bash
./gradlew test
```

Show CLI help:

```bash
./gradlew :testloom-cli:run --args='--help'
```

Create default config:

```bash
./gradlew :testloom-cli:run --args='init'
```

Or write to a custom location:

```bash
./gradlew :testloom-cli:run --args='init --path ./config/testloom.yaml --force'
```

## CLI command surface

```text
testloom init
```

Planned next commands:

- `testloom inspect --capture <file>`
- `testloom generate --capture <file> --out <dir>`
- `testloom db dump --tables <...> --out <file>`

## testloom.yaml shape

Generated config contains:
- `testloom.recorder` (enabled/mode/output-dir/body and path controls)
- `testloom.redaction` (headers/json-fields/query-params/mask)
- `testloom.redaction.rules[]` (typed rule structure: target type, matcher, action)

## Config package layout (DDD)

`testloom-core` organizes config code by DDD concerns:

```text
dev.testloom.core.config
  application
    port
    service
  domain
    model
    exception
  infrastructure
    yaml
```

## Monorepo layout

```text
testloom/
  testloom-core/
  testloom-spring-boot-starter/
  testloom-cli/
  testloom-examples/
    mvc-postgres-demo/
    grpc-postgres-demo/
```

## Non-goals for current MVP

- Kafka capture
- gRPC capture in MVP runtime
- Distributed trace backend
- SaaS dashboard
- Automatic full fixture minimization

## Contributing

Contributions are welcome. Open an issue before large changes so scope and direction stay aligned.
