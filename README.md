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
- MVC HTTP capture filter + JSON file writer in starter
- Unified logging through SLF4J (`@Slf4j` in runtime services)
- Domain exceptions for redaction policy compilation/runtime failures
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
- `testloom.redaction` (`mask` + per-target defaults: `header-default-action`, `query-param-default-action`, `json-field-default-action`)
- `testloom.redaction.rules[]` (typed rule structure: target type, matcher, action, optional replacement)

## Redaction behavior

- Rules are evaluated in order; first matching rule wins.
- If no rule matches, per-target default action is applied.
- Invalid/unsupported runtime decision (`null`) is treated as an error (`RedactionException`), not silently ignored.
- Unknown enum values in YAML (for example invalid redaction target type) fail config parsing before lint.
- JSON body fallback is fail-safe:
  - if body cannot be treated as JSON (not JSON-like, parse failure, write failure), `json-field-default-action` is applied to the whole body.
  - `KEEP` keeps body as-is, `MASK` replaces whole body with configured mask string, `REMOVE` sets body to `null`.

## Quality gate

- `jacocoTestCoverageVerification` is set to `LINE = 0.80` for `testloom-core`, `testloom-spring-boot-starter`, and `testloom-cli`.

## Capture format

Capture JSON schema v0.1 is documented in [docs/capture-format.md](docs/capture-format.md).

## Release process

- Release checklist: [docs/release-checklist.md](docs/release-checklist.md)
- Change history and release notes source: [CHANGELOG.md](CHANGELOG.md)

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

## Module boundaries

- `testloom-core`: capture contracts, domain model, and infrastructure that does not depend on Spring.
- `testloom-spring-boot-starter`: Spring Boot auto-configuration and MVC adapter (`OncePerRequestFilter`).
- `testloom-cli`: command-line entrypoints and orchestration.
- `testloom-examples`: runnable demo applications.

## Non-goals for current MVP

- Kafka capture
- gRPC capture in MVP runtime
- Distributed trace backend
- SaaS dashboard
- Automatic full fixture minimization

## Contributing

Contributions are welcome. Open an issue before large changes so scope and direction stay aligned.
