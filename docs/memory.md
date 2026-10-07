# Memory — decision record

## 2026-10-07 — TASK-1.1 build skeleton

Quarkus platform: 3.39.1
- PIN-AC-1255 / slice 18 P-1. Settled for Gradle by the green CI run 37585096973 of the TASK-1.1 PR, read by commit SHA 44185bf.
- The local build is green on 3.39.1 with Gradle 9.8.0 and Temurin 25. If CI is red for a framework reason, this line and the catalog move together to the first newer Quarkus line that turns it green.

Gradle: 9.8.0
- Wrapper with the distribution checksum pinned (`distributionSha256Sum`). Java toolchain 25 and `options.release = 25` in every module.

Executor (UD-17): the Archon build-feature workflow (.archon/workflows/build-feature.yaml) running Claude Code; code and tests by Claude Sonnet 5.5 (@build), planning and review by Claude Opus 5.5 (@analyse), per UD-21.a.
- Confirmed by the user on 2026-10-07; .archon/config.yaml and UD-21.a record the model split.

Module layout: follows architecture-conventions §2.2 and §14.1. Library beans are discovered through each module's own Jandex index (PIN-AC-0153).

## 2026-10-07 — TASK-1.2 health

- `/api` is rooted by `@ApplicationPath("/api")` on `app.rekord.application.config.ApiApplication`, not by `quarkus.http.root-path`, so `/q/*` stays where the readiness probe looks for it.
- `application/src/test/resources/fixtures/health-200.json` was captured from rekord-api at commit `ec65ae35c182e6e25f571c76d45b15a78f183c10` with `./mvnw quarkus:dev` (`GET /api/health`, no cookie): 200, `application/json;charset=UTF-8`, body `{"ok":true}`.
- The hand-written `HealthResource` and `Health` record are temporary; P0-E02-T03 replaces them with the generated `HealthApi`.

## 2026-10-07 — TASK-1.3 image

- Build stage is `eclipse-temurin:25-jdk` using the checked-in Gradle wrapper: `./gradlew --no-daemon :application:quarkusBuild -x test -x integrationTest` (no test compiled or run). The runtime stage is the `eclipse-temurin:25-jre` fast-jar image running as `USER 10001:10001`.
- The gid is explicit (10001, matching the uid), so `USER 10001:10001` names a real group.
- `QUARKUS_HTTP_HOST=0.0.0.0` is kept in the image environment so the container answers on its published port.
- The image is built and checked as a running container in CI (job `image`, `.github/scripts/image-check.sh`) and never pushed (UD-13.e): no registry login, no secret.
- PIN-AC-1008 is settled by the green CI run 37596108025 of the TASK-1.3 PR (jobs `build` and `image`), read by commit SHA 9a6bcac.
- The frontends' nginx images under BR-OPS-26 are deferred (H2).

## 2026-10-07 — TASK-2.1 contract checkout in CI

- The workflow reads the private hub `Vikteur/rekord-contract` with the repository secret named `CONTRACT_TOKEN`: a fine-grained token, read-only Contents on `Vikteur/rekord-contract` only, created by the user (the STOP approval of this ticket; UD-15.e, RISK-51).
- There is no fallback to the workflow's own token: an empty secret fails the checkout step, and nothing after it runs.
- The hub is checked out at its default branch into `contract/` until TASK-2.2 pins a tag.
- Every run proves the token read-only with a dry-run push (`.github/scripts/contract-read-only-check.sh`); a push that is accepted fails the run.
- Every action is pinned by its 40-character commit SHA, with the version as a trailing comment.
- CI runs on pushes to `main` and `feature/**` and on pull requests into `main`; `pull_request_target` is never used.

## 2026-10-07 — TASK-4.1 datasource and Flyway

- The database is named `wedding_portal` in every profile. `%prod` reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment, with no default URL and no default password.
- `V1__baseline.sql` is a comment-only baseline: wedding-portal's own empty history, no import and no shared database (UD-13.b). This migration was the STOP approval of this ticket; business tables arrive in later migrations, each a STOP item.
- `*.sql` is pinned to LF in `.gitattributes`, because the Flyway checksum is computed over the bytes.
- Every Gradle test JVM runs with `-Duser.timezone=UTC`, as the image does.
- The start-up refusals (Flyway on a database without history, Hibernate validate on an unmigrated entity) are tested with `QuarkusUnitTest`, a test-owned Testcontainers PostgreSQL and a stray entity mapped through `stray-entity-orm.xml`, so no annotated entity reaches the `@QuarkusTest` classes. `QuarkusUnitTest` and `@QuarkusTest` cannot share a JVM, so these classes carry the `quarkus-unit-test` tag and run in their own `startupTest` task, which `check` (and so `build`) depends on; `integrationTest` excludes the tag.
- The image check now starts a throwaway `postgres:17-alpine` on a per-run network with a generated password, and checks that Flyway ran.
- Every `@QuarkusTest` needs Docker (Dev Services PostgreSQL) until TASK-4.3 adds the datasource-less test profile.

## 2026-10-07 — TASK-4.3 resource-test profile

- Supersedes the TASK-4.1 bullet "Every `@QuarkusTest` needs Docker ... until TASK-4.3": `HealthResourceIT` no longer needs Docker. The other `@QuarkusTest` classes (`TimeZoneIT`, `ModuleBeanDiscoveryIT`, persistence ITs) still use Dev Services in `integrationTest`.
- Quarkus 3.39.1 boots with the datasource inactive, so the shared §8.5 container fallback (PIN-AC-0535) was not needed. The datasource, Flyway and Hibernate beans are inactive and no JDBC url is set.
- `ResourceTestProfile` (a `QuarkusTestProfile` in `application/src/test`, so the main `application.properties` stays unchanged) overrides six keys: `quarkus.devservices.enabled=false`, `quarkus.datasource.devservices.enabled=false`, `quarkus.datasource.active=false`, `quarkus.flyway.active=false`, `quarkus.flyway.migrate-at-start=false` and `quarkus.hibernate-orm.active=false`. No key was dropped; Quarkus accepted all of them.
- Resource tests carry `@TestProfile(ResourceTestProfile.class)` and `@Tag("resource-test")`, and run in their own `resourceTest` task, which `check` (and so `build`) depends on. `integrationTest` excludes the tag. `ResourceTestTaggingTest` keeps the tag and the profile together.
- `ContainerTripwire` is a Testcontainers `ImageNameSubstitutor`, set through `TESTCONTAINERS_IMAGE_SUBSTITUTOR` for `resourceTest` only. It throws for any image, so a container start in that JVM fails the boot. Removing the Dev Services keys from the profile made the boot fail in the tripwire, which shows it works.

## 2026-10-07 — TASK-4.4 WireMock harness

- WireMock is `org.wiremock:wiremock-standalone` 3.13.2, pinned in the catalog. It is shaded, so it brings no Jetty or Jackson conflict with the Quarkus BOM. 4.x is still beta.
- The Java packages stay `com.github.tomakehurst.wiremock.*`. The ban is on the artifact group `com.github.tomakehurst`, enforced by the `legacyWireMockCheck` task, which `check` depends on.
- Gateway tests that do not boot Quarkus live in the gateway module, end in `IT` and run in `integrationTest`.
- `ProbeStatusGateway` is test-only and may go once the first real gateway has its own WireMock test.
- P-3 / PIN-AC-0655 is settled by the CI run of this PR (run id and SHA are filled in after CI).
