---
name: testcontainers
description: Integration-test against real backing services with Testcontainers and Dev Services. Use instead of in-memory fakes.
---
# Testcontainers

## When to use
Integration tests that must exercise a real service instead of a substitute: repository/persistence
tests that need true SQL behavior (vendor types, DDL, constraints, migrations, dialects) where an
in-memory database would lie — or adapters that talk to a real backing service over the network (a
search/index engine, an SMTP server, a cache, a message broker) where a mock would not exercise the
real protocol, serialization, or readiness behavior.

## How — the shared mechanism
- Start the service in a container from a **pinned image** (never a floating tag); reuse **one
  container per JVM** so it starts once, not per test.
- For the database, let **Quarkus Dev Services** start it: Dev Services runs on Testcontainers, so
  pin the image in test configuration and do not hand-write a container lifecycle that duplicates it.
  Dev Services publishes the coordinates as configuration properties itself.
- For other services, a `QuarkusTestResourceLifecycleManager` (or a Dev Services extension) starts the
  container and returns its coordinates as configuration; register it on the shared base test class.
- Keep tests independent: each test sets up and asserts its own data; never depend on order.

## Database flavor (Postgres and friends)
- Let the real **migration tool** build the schema ([[flyway-migrations]] — enable it for the test
  profile) so tests run the same DDL as production — never hand-build the schema in test code.
- Seed only what every test needs via a classpath init script mounted into the container; keep
  per-test data in builders/mothers ([[object-mother-builders]]), not global fixtures.

## Service flavor (search, mail, cache, queue, …)
- Model each service as a `GenericContainer` with declared exposed ports and env vars.
- Use a **wait strategy** so the container is only "started" once actually ready — an HTTP health
  endpoint or log/port readiness probe, never a blind sleep.
- After start, read the dynamically mapped host+port (`getHost()`, `getMappedPort(...)`) and return
  them as configuration; never hardcode the published port.

## Pattern signals (discovery cues — how the scanner recognizes this in any codebase)
- `org.testcontainers.*`: `PostgreSQLContainer`, `GenericContainer<>(DockerImageName.parse(...))`
  with `.withExposedPorts(...)` / `.withEnv(...)` in `src/test/java/**`.
- `QuarkusTestResourceLifecycleManager` implementations returning service properties; Dev Services
  image settings in the test profile.
- `@QuarkusTest` with a migration tool enabled for tests; `.waitingFor(Wait.forHttp(...))` readiness probes.

## Project specifics → see docs
- Code map (which services, images, base classes, migration location, init scripts, exemplars) → `docs/code-maps/testcontainers.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't test repositories against an in-memory DB when production is Postgres — behavior diverges.
- Don't start a fresh container per test method; share one instance per JVM.
- Don't hand-build the schema in test code when a migration tool already owns it.
- Don't hardcode published ports or rely on `Thread.sleep` — mapped ports + a real wait strategy.
- Don't pin to a floating tag (`latest`) — fix the image version for reproducibility.

## Definition of done
- [ ] Each service runs as a pinned container, one instance per JVM, wired through Dev Services or a
      lifecycle manager on a shared base; no hardcoded ports; real readiness waits.
- [ ] Database tests run schema built by the migration tool.
- [ ] Tests are independent and deterministic.
