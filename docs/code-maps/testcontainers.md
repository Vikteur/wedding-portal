---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

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
| PostgreSQL | `postgres:13.18-alpine` | `TestDatabaseInitializer` (two copies, see Drift) | `spring.datasource.url`, `spring.datasource.driver-class-name` |
| Typesense | `typesense/typesense:30.0` | `TypesenseTestContainerInitializer` | `typesense.api.base-path`, `typesense.api.key` |
| smtp4dev | `rnwood/smtp4dev:3.7.1` | `Smtp4devTestContainerInitializer` | `spring.mail.host`, `spring.mail.port` |

Image tags are pinned exactly - never `latest`. A tag bump is a deliberate, reviewable change.

### Excerpts
PostgreSQL is started through an `ApplicationContextInitializer` and wired into Spring properties:
```java
public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:13.18-alpine");

    @Override
    public void initialize(final ConfigurableApplicationContext context) {
        postgreSQLContainer
                .withDatabaseName("shop")
                .withCopyFileToContainer(MountableFile.forClasspathResource("init_postgres.sql"),
                        "/docker-entrypoint-initdb.d/")
                .start();

        TestPropertyValues.of("spring.datasource.url=" + postgreSQLContainer.getJdbcUrl())
                .and("spring.datasource.driver-class-name=" + postgreSQLContainer.getDriverClassName())
                .applyTo(context.getEnvironment());
    }
}
```

Typesense uses a generic container with explicit env and readiness wiring:
```java
private static final GenericContainer<?> typesenseContainer =
        new GenericContainer<>(DockerImageName.parse("typesense/typesense:30.0"))
                .withExposedPorts(8108)
                .withEnv("TYPESENSE_API_KEY", "<test-key>")
                .withEnv("TYPESENSE_DATA_DIR", "/tmp")
                .withEnv("TYPESENSE_ENABLE_SEARCH_ANALYTICS", "TRUE")
                .waitingFor(new HttpWaitStrategy().forPath("/ready"));
```

Repository integration tests inherit a shared base that keeps JPA on the real container database:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
        initializers = {
                TypesenseTestContainerInitializer.class,
                Smtp4devTestContainerInitializer.class
        },
        classes = AbstractRepositoryTest.RepositoryTestConfig.class)
public abstract class AbstractRepositoryTest { }
```

### Edge cases
Omitted `Replace.NONE` on a repository integration test:
```java
@DataJpaTest
class PageJpaRepositoryTest { }
```
Expected: likely falls back to an in-memory database, which defeats the point of the container path.

No cleanup between test classes:
```java
entityManager.createNativeQuery("TRUNCATE TABLE page CASCADE").executeUpdate();
```
Expected: keep something equivalent. Without it, rows leak across classes and failures become order-dependent.

Forgotten dynamic property:
```java
TestPropertyValues.of("spring.mail.host=" + smtp4devContainer.getHost()).applyTo(env);
```
Expected: also wire the matching port (and any API key) or the test still points at nowhere useful.

## Local conventions (the project facts the skill omits)
- **Mechanism: `ApplicationContextInitializer`, not `@Testcontainers` / `@Container`.** Every container
  here is a `private static final` field started inside `initialize(...)`, with the connection
  details pushed into the context via `TestPropertyValues.of(...).applyTo(...)`. A test opts in with
  `@ContextConfiguration(initializers = { ... })`. The consequence is deliberate: the container is
  static, so it starts once per JVM and is shared by every test class in that module rather than
  being recycled per class.
- **Wiring, per module:** `testImplementation("org.testcontainers:testcontainers-postgresql")` (and
  `org.testcontainers:testcontainers` where a `GenericContainer` is used) - declared in 5 modules:
  `application`, `authentication-adapter`, `storefront-adapter`, `account-adapter`,
  `attribute-mapping-adapter`. Versions come from the BOM, never pinned per module.
- **The repository base class** is `storefront-adapter/.../repository/support/AbstractRepositoryTest`:
  `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` (so the container database is used
  rather than an in-memory replacement) + a narrowed `@ComponentScan` that admits only `@Repository`
  beans + the Typesense and smtp4dev initializers. New repository integration tests extend it; they
  do not re-declare containers.
- **Schema comes from the real migrations.** `authentication-adapter`'s initializer sets
  `spring.flyway.enabled=true`, so the container runs the same Flyway DDL as production -
  `application/src/main/resources/db/migration/ddl/V1.{n}__{desc}.sql`. Never hand-create tables in
  a test.
- **Database bootstrap:** the PostgreSQL container mounts the repo-root `init_postgres.sql` at
  `/docker-entrypoint-initdb.d/` via `MountableFile.forClasspathResource`, and uses database name
  `shop`.
- **Wait strategies are explicit for the non-JDBC containers** - Typesense waits on an HTTP readiness
  endpoint, smtp4dev on `HTTP /api/messages` returning 200 on port 80. `PostgreSQLContainer` brings
  its own readiness check, so it declares none.

## Frequency & coverage
- 4 initializer classes across 3 modules; 7 test sources reference an initializer or extend a base
  class that does (as of `abc1234`).
- Everything else in the suite is a plain unit or slice test - the container path is the exception,
  not the default. See [spring-boot-slice-tests] for the non-container slice conventions and
  [wiremock-gateway-stubs] for the outbound-HTTP equivalent.

## Drift / exceptions
- **`TestDatabaseInitializer` exists twice**, in `application/src/test/java/com/acme/shop/` and
  in `authentication-adapter/src/test/java/com/acme/shop/authentication/adapter/repository/`,
  with the same image and near-identical bodies. They are not identical: only the
  `authentication-adapter` copy enables Flyway, and only the `application` copy mounts
  `init_postgres.sql`. Treat the divergence as unintentional - a shared test-jar (the
  `shop.test-jar-producer` convention plugin already supports exactly this) is the
  natural home. Not consolidated here; recorded as an observation.
- Containers are static and never explicitly stopped; Ryuk reaps them at JVM exit. Fine for CI,
  worth knowing when a local run leaves images resident.

## Provenance
- Scanned at: `abc1234` - tool/query: `rg -l 'testcontainers|Testcontainers'` over Java and Gradle
  sources excluding `build/`, plus reads of the four initializers and `AbstractRepositoryTest`.
  Not yet scanner-harvested.

[spring-boot-slice-tests]: spring-boot-slice-tests.md
[wiremock-gateway-stubs]: wiremock-gateway-stubs.md
