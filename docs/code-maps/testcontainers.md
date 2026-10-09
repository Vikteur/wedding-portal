---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map - `testcontainers` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> The skill says *how* to run a test against a real service in a container. This is *which* services,
> *which* images, and *which* base classes this repo actually uses.

## Services and images

| Service | Image | Started by | Properties it overrides |
|---|---|---|---|
| PostgreSQL | `postgres:13.18-alpine` | Quarkus Dev Services, configured per module in `%test.` properties (two copies, see Drift) | `quarkus.datasource.jdbc.url`, `quarkus.datasource.username`, `quarkus.datasource.password` (set by Dev Services) |
| Typesense | `typesense/typesense:30.0` | `TypesenseTestResource` | `typesense.api.base-path`, `typesense.api.key` |
| smtp4dev | `rnwood/smtp4dev:3.7.1` | `Smtp4devTestResource` | `quarkus.mailer.host`, `quarkus.mailer.port` |

Image tags are pinned exactly - never `latest`. A tag bump is a deliberate, reviewable change.

### Excerpts
PostgreSQL is started by Quarkus Dev Services (Testcontainers under the hood), pinned in the test profile:
```properties
quarkus.datasource.db-kind=postgresql
%test.quarkus.datasource.devservices.image-name=postgres:13.18-alpine
%test.quarkus.datasource.devservices.db-name=shop
%test.quarkus.datasource.devservices.init-script-path=init_postgres.sql
```

Typesense uses a generic container in a `QuarkusTestResourceLifecycleManager` with explicit env and readiness wiring:
```java
public class TypesenseTestResource implements QuarkusTestResourceLifecycleManager {
    private static final GenericContainer<?> typesenseContainer =
            new GenericContainer<>(DockerImageName.parse("typesense/typesense:30.0"))
                    .withExposedPorts(8108)
                    .withEnv("TYPESENSE_API_KEY", "<test-key>")
                    .withEnv("TYPESENSE_DATA_DIR", "/tmp")
                    .withEnv("TYPESENSE_ENABLE_SEARCH_ANALYTICS", "TRUE")
                    .waitingFor(new HttpWaitStrategy().forPath("/ready"));

    @Override
    public Map<String, String> start() {
        typesenseContainer.start();
        return Map.of(
                "typesense.api.base-path", "http://" + typesenseContainer.getHost() + ":" + typesenseContainer.getMappedPort(8108),
                "typesense.api.key", "<test-key>");
    }

    @Override
    public void stop() { }
}
```

Repository integration tests inherit a shared base that boots Quarkus against the Dev Services database:
```java
@QuarkusTest
@QuarkusTestResource(TypesenseTestResource.class)
@QuarkusTestResource(Smtp4devTestResource.class)
public abstract class AbstractRepositoryTest { }
```

### Edge cases
A datasource URL set in the test profile:
```properties
%test.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/shop
```
Expected: Dev Services silently stays off when a URL is configured, so the test hits whatever database runs locally, which defeats the point of the container path.

No rollback between tests:
```java
@Test
@TestTransaction
void givenPage_whenPersist_thenFindByPath() { }
```
Expected: keep `@TestTransaction` (or an equivalent `TRUNCATE TABLE page CASCADE`). Without it, rows leak across tests and failures become order-dependent.

Forgotten config entry:
```java
return Map.of("quarkus.mailer.host", smtp4devContainer.getHost());
```
Expected: also return the matching `quarkus.mailer.port` (and any API key) or the test still points at nowhere useful.

## Local conventions (the project facts the skill omits)
- **Mechanism: Dev Services + `QuarkusTestResourceLifecycleManager`, not `@Testcontainers` / `@Container`.**
  PostgreSQL is a Dev Services container driven purely by `%test.quarkus.datasource.devservices.*`
  properties. Every other container is a `private static final` field in a lifecycle manager,
  started in `start()`, whose returned map overrides the application config. The resources are
  registered with `@QuarkusTestResource` (global to the module), so they start once per Quarkus
  test application and are shared by every `@QuarkusTest` class in that module rather than being
  recycled per class.
- **Wiring, per module:** PostgreSQL needs no Testcontainers dependency (Dev Services comes with
  `quarkus-jdbc-postgresql`); `testImplementation("org.testcontainers:testcontainers")` where a
  `GenericContainer` is used - declared in 5 modules: `application`, `authentication-adapter`,
  `storefront-adapter`, `account-adapter`, `attribute-mapping-adapter`. Versions come from the
  Quarkus BOM, never pinned per module.
- **The repository base class** is `storefront-adapter/.../repository/support/AbstractRepositoryTest`:
  `@QuarkusTest` (so the Dev Services database is used) + the Typesense and smtp4dev test
  resources. Subclasses `@Inject` the repository under test and mark each test `@TestTransaction`
  to roll it back. New repository integration tests extend it; they do not re-declare containers.
- **Schema comes from the real migrations.** `authentication-adapter`'s test profile sets
  `%test.quarkus.flyway.migrate-at-start=true`, so the container runs the same Flyway DDL as
  production - `application/src/main/resources/db/migration/ddl/V1.{n}__{desc}.sql`. Never
  hand-create tables in a test.
- **Database bootstrap:** Dev Services runs the repo-root `init_postgres.sql` via
  `quarkus.datasource.devservices.init-script-path`, and uses database name `shop`.
- **Wait strategies are explicit for the non-JDBC containers** - Typesense waits on an HTTP readiness
  endpoint, smtp4dev on `HTTP /api/messages` returning 200 on port 80. Dev Services brings its own
  PostgreSQL readiness check, so it declares none.

## Frequency & coverage
- 4 container setups (Dev Services blocks + test resources) across 3 modules; 7 test sources
  reference a test resource or extend a base class that does (as of `abc1234`).
- Everything else in the suite is a plain unit test - the container path is the exception,
  not the default.

## Drift / exceptions
- **The PostgreSQL Dev Services setup exists twice**, in `application/src/test/resources/` and
  in `authentication-adapter/src/test/resources/` `application.properties`, with the same image and
  near-identical `%test.` blocks. They are not identical: only the `authentication-adapter` copy
  enables Flyway, and only the `application` copy sets `init_postgres.sql`. Treat the divergence as
  unintentional - a shared test-jar (the `shop.test-jar-producer` convention plugin already
  supports exactly this) is the natural home for shared test config. Not consolidated here;
  recorded as an observation.
- Lifecycle managers' `stop()` is a no-op, so their containers are never explicitly stopped; Ryuk
  reaps them at JVM exit. Fine for CI, worth knowing when a local run leaves images resident.

## Provenance
- Scanned at: `abc1234` - tool/query: `rg -l 'testcontainers|Testcontainers|devservices'` over Java,
  properties and Gradle sources excluding `build/`, plus reads of the four container setups and
  `AbstractRepositoryTest`.
  Not yet scanner-harvested.
