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
- The hub is checked out into `contract/` at the ref named by `rekordContractTag` in `gradle.properties` (TASK-2.2): a tag (`refs/tags/<tag>`, vMAJOR.MINOR.PATCH) or, on a feature branch only, the rekord-contract branch of the SAME name as the wedding-portal branch (`refs/heads/<branch>`). `main` always pins a tag. `.github/scripts/contract-pin.sh` reads and validates the pin (exactly one line; outputs kind, ref, pin); `.github/scripts/contract-ref-check.sh` proves the checkout HEAD is exactly that tag or branch head. A branch pin builds and tests against its contract branch, but the last CI step (`.github/scripts/contract-pin-merge-check.sh`) fails until the pin names a tag, so a branch pin can never be merged green; raise the pin to the new tag before merging.
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
- Every `rekord-gateway` test is an IT, so that module alone sets `failOnNoDiscoveredTests = false` on its `test` task. The root keeps Gradle's guard on for every other module.
- `ProbeStatusGateway` is test-only and may go once the first real gateway has its own WireMock test.
- P-3 / PIN-AC-0655 is settled by the CI run of this PR (run id and SHA are filled in after CI).

## 2026-10-07 — TASK-2.3 contract codegen

- `rekord-adapter` runs `openApiGenerate` (OpenAPI Generator 7.25.0, plugin `org.openapi.generator` from the version catalog, declared `apply false` at the root so the Jandex plugin and this one share a classloader) with `jaxrs-spec` and the options `interfaceOnly`, `useJakartaEe`, `useTags` set, and `returnResponse`, `useSwaggerAnnotations`, `openApiNullable` off, `dateLibrary=java8`. API and model tests are off. Packages are `app.rekord.api` and `app.rekord.api.model`.
- Output goes to `rekord-adapter/build/generated/openapi/src/gen/java`, a source root of `main` wired through the task provider. `cleanupOutput` empties it before each run, so an interface or model the spec no longer has cannot linger and keep a drifted resource compiling. It sits under the `build/generated` edit-guard globs and is ignored by git; nothing generated is ever committed or edited.
- `-Pcontract.spec=<rekord-contract checkout>/dist/openapi.yaml` is required on every compiling Gradle run (`help` works without it). Locally: `git clone --branch v0.1.0 C:/Users/Pascal/Documents/weddingapp/rekord-contract contract` (the dir is gitignored), then pass `-Pcontract.spec=contract/dist/openapi.yaml`. The test JVM receives it as the system property `contract.spec`.
- Supersedes the TASK-1.2 bullet "The hand-written HealthResource and Health record are temporary": health now lives in `app.rekord.adapter.web.health.HealthResource`, which implements the generated `HealthApi` and returns the generated `Health`. Paths come from the interface, so the resource carries no JAX-RS annotation. `HealthResourceIT` is unchanged and still passes byte for byte against the rekord-api fixture. `ContractDriftBreaksCompileTest` shows a renamed operation or field stops it compiling.
- A resource that implements a generated interface cannot set a success status by annotation (Quarkus reads the JAX-RS metadata from the interface). It injects `app.rekord.adapter.web.shared.SuccessStatus` (request scoped) and calls `answer(201)` (or 200, 202, 204; any other code throws). `SuccessStatusFilter`, a `@Provider` response filter, applies the recorded code and drops the entity for 204, but only to a successful answer: a refusal after `answer` keeps its error status. A method that never calls `answer` keeps the default. `SuccessStatusIT` proves it against a test-only probe resource in `application/src/test`.
- The two `QuarkusUnitTest` start-up refusal tests add `addPackages(true, "app.rekord.adapter")` to their application root. Their root is a bare jar, so the adapter's classes (indexed by Jandex) were loaded by the parent class loader, and the first `@Provider` (`SuccessStatusFilter`) failed with a cross-loader cast. Any new `QuarkusUnitTest` needs the same line. `SuccessStatusFilter` takes its bean through a public constructor for the same reason.
- Both CI jobs (`build` and `image`) check out the pinned `rekord-contract` into `contract/` (pin, read-only token check, ref check, bundle present) and pass `-Pcontract.spec=contract/dist/openapi.yaml`. The image build gets only `contract/dist/openapi.yaml` through `.dockerignore`, and the token stays on the runner: it is never an `ARG`, `ENV` or secret of `docker build`. `ContractSpecWiringTest` guards all of it.
