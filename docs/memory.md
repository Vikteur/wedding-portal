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

## 2026-10-07 — TASK-3.3 agent framework

- Domain events are raised in the domain (the aggregate records past-tense event records) and published by the use case through the `DomainEventPublisher` port after the change is valid. Publishing from the domain would put a bus or a port call inside the aggregate, which FW-C-01 forbids. Sources: 18 C-01, architecture-conventions §5.4, CT-22.
- The per-repo code-map leaves live in `docs/code-maps` of this repository; the generic skills and rules live in the umbrella `.claude`. A skill says how, a leaf says what.

## 2026-10-07 — TASK-24.1 matcher text core

- The matcher text core lives in `app.rekord.domain.matching` (module `rekord-domain`), not `domain.shared`: the shared kernel is limited to what 41 D-10 lists, and the song identity belongs to the Matching and export boundary.
- `rekord-domain` has test-only dependencies (JUnit 5, AssertJ, launcher); the main classpath stays `java..` only (ArchUnit rule A1).
- UD-19.m6: `Normalize` keeps Unicode letters and decimal digits (`[^\p{L}\p{Nd}]+`), where rekord-api uses `[^a-z0-9]+`. For "Кино", "Мумий Тролль", "Άλφα Βήτα", "東京事変", "ABBA Ёлка", "ＡＢＢＡ", "Кино+Ария" rekord-api answers "", "", "", "", "abba", "abba", "". The `+` rule still uses ASCII `\w`, as rekord-api does.
- Signature id: `norm(artist)|norm(core)|descriptors joined "+"|norm(remixer)`, featured left out, id = first 16 hex of SHA-1 over the UTF-8 bytes. P3-E05's remembered choices are keyed by it, so any change to `Normalize`, `Versions` or `Signature` output orphans them silently.

## 2026-10-07 — TASK-2.3 contract codegen

- `rekord-adapter` runs `openApiGenerate` (OpenAPI Generator 7.25.0, plugin `org.openapi.generator` from the version catalog, declared `apply false` at the root so the Jandex plugin and this one share a classloader) with `jaxrs-spec` and the options `interfaceOnly`, `useJakartaEe`, `useTags` set, and `returnResponse`, `useSwaggerAnnotations`, `openApiNullable` off, `dateLibrary=java8`. API and model tests are off. Packages are `app.rekord.api` and `app.rekord.api.model`.
- Output goes to `rekord-adapter/build/generated/openapi/src/gen/java`, a source root of `main` wired through the task provider. `cleanupOutput` empties it before each run, so an interface or model the spec no longer has cannot linger and keep a drifted resource compiling. It sits under the `build/generated` edit-guard globs and is ignored by git; nothing generated is ever committed or edited.
- `-Pcontract.spec=<rekord-contract checkout>/dist/openapi.yaml` is required on every compiling Gradle run (`help` works without it). Locally: `git clone --branch v0.1.0 C:/Users/Pascal/Documents/weddingapp/rekord-contract contract` (the dir is gitignored), then pass `-Pcontract.spec=contract/dist/openapi.yaml`. The test JVM receives it as the system property `contract.spec`.
- Supersedes the TASK-1.2 bullet "The hand-written HealthResource and Health record are temporary": health now lives in `app.rekord.adapter.web.health.HealthResource`, which implements the generated `HealthApi` and returns the generated `Health`. Paths come from the interface, so the resource carries no JAX-RS annotation. `HealthResourceIT` is unchanged and still passes byte for byte against the rekord-api fixture. `ContractDriftBreaksCompileTest` shows a renamed operation or field stops it compiling.
- A resource that implements a generated interface cannot set a success status by annotation (Quarkus reads the JAX-RS metadata from the interface). It injects `app.rekord.adapter.web.shared.SuccessStatus` (request scoped) and calls `answer(201)` (or 200, 202, 204; any other code throws). `SuccessStatusFilter`, a `@Provider` response filter, applies the recorded code and drops the entity for 204, but only to a successful answer: a refusal after `answer` keeps its error status. A method that never calls `answer` keeps the default. `SuccessStatusIT` proves it against a test-only probe resource in `application/src/test`.
- The two `QuarkusUnitTest` start-up refusal tests add `addPackages(true, "app.rekord.adapter")` to their application root. Their root is a bare jar, so the adapter's classes (indexed by Jandex) were loaded by the parent class loader, and the first `@Provider` (`SuccessStatusFilter`) failed with a cross-loader cast. Any new `QuarkusUnitTest` needs the same line. `SuccessStatusFilter` takes its bean through a public constructor for the same reason.
- Both CI jobs (`build` and `image`) check out the pinned `rekord-contract` into `contract/` (pin, read-only token check, ref check, bundle present) and pass `-Pcontract.spec=contract/dist/openapi.yaml`. The image build gets only `contract/dist/openapi.yaml` through `.dockerignore`, and the token stays on the runner: it is never an `ARG`, `ENV` or secret of `docker build`. `ContractSpecWiringTest` guards all of it.

## 2026-10-07 — TASK-3.4 governance hooks

- `.claude/hooks/hooks.env` (in the umbrella repo weddingapp, where the agent hooks live) ships with `ARCH_FITNESS_CMD`, `LINT_FIX_CMD` and `LINT_CHECK_CMD` unset.
- `ARCH_FITNESS_CMD` stays unset because CI runs ArchUnit as part of the build (architecture-conventions §14.3).
- `LINT_FIX_CMD` and `LINT_CHECK_CMD` wait for the formatter decision: set them only once a formatter plugin is applied to the build, and re-wire `lint-format.sh` in `.claude/settings.json` in the same change. A gate registered with no command exits 0 on every run and buys false confidence.
- Both PreToolUse guards (`guard-generated.sh`, `scope-guard.sh`) fail closed when `jq` is missing (PIN-17-0755): without jq, scope-guard cannot tell which agent is calling and denies every Write, Edit and Bash call, the main session's included, until jq is installed. Every machine that runs the hooks needs `jq` on the PATH.

## 2026-10-07 — TASK-24.2 candidate retrieval

- `Fuzz`, `LibraryIndex`, `QueryText` and `TrackMatcher` live in `app.rekord.domain.matching`. `TrackMatcher` is not named `Matcher`, because `Versions` imports `java.util.regex.Matcher`. The fuzzy fallback breaks ties by ordinal ascending (UD-8); Python's order is not followed.
- The library order is the order the database returns for `order by t.path` with no collation. The tracks repository built with the library schema (TASK-22.1, read by TASK-24.5) must keep that query. Until the real tables exist, `LibraryPathOrderIT` pins it on a test-owned schema in a throwaway container.
- This ticket ported part of `Score` and `matchOne`: the facets, the weighted mean, the 0.45 floor, the cap of 8 and rekord-api's three-guard bucket. The playlist nudge, `duration_delta_sec` and the UD-19.c auto rule are left to TASK-24.3, and remembered choices to P3-E05-T02.
- `rekord-domain` gains test-only `jackson-databind` (from the Quarkus BOM) for the fuzz fixture. Its main classpath stays `java..` only.

## 2026-10-07 — TASK-3.1 ArchUnit suite

- ArchUnit is `archunit-junit5` 1.5.1 (catalog key `archunit`). The suite is in `application/src/test/java/app/rekord/architecture`, rules A1 to A14, and runs in `test`.
- It reads the release-25 class files of all six modules from `build/classes/java/main` through `ProductionClasses` (PIN-AC-0230: ArchUnit 1.5.1 reading Java 25 class files; settled by CI run 37652403500, green on fd40198 (PR #15, merged as 5f7f1df)).
- Every rule is frozen, but the store `application/src/test/archunit_store` holds no violation: all files are empty. `archunit.properties` sets `allowStoreCreation=false` and `allowStoreUpdate=false`, so a new violation fails and nothing rewrites the store. A renamed `because` text or a new rule needs its empty entry added on purpose (both flags on, run once, flags off).
- Rule fixtures are compiled at test time by `FixtureCompiler`, so no fixture lives under a reserved layer package.
- Rule-shape decisions: A8 exempts only the Clock and id-port providers in `application.config`; A10 treats a class implementing `DomainEvent` as shape (a) and a port the origin implements as shape (b); A13's helper exemption is deferred to the §4.1 ticket; A14 limits status setting to `adapter.web.shared` and checks `@ResponseStatus` plus the `Response`/`RestResponse` static factories (not every method, so `getStatusInfo()` stays legal); A3 also allows `Transactional$TxType`.

## 2026-10-07 — TASK-3.2 use-case transaction boundary

- A3 now also refuses an `@Produces` method that returns or constructs a type of `app.rekord.usecase..` and an `@Produces` field of such a type (UD-15.a, PIN-AC-0448). Ports in `app.rekord.usecase..port..` stay producible. A3 is one composite rule, so the suite still has A1 to A14 and the store keeps its 14 entries, all empty (A3's key was re-written on its existing id).
- The boundary is proven at transaction level by `UseCaseTransactionBoundaryIT` with a test-only port and no database: a transaction is active inside the `@Transactional` method, and a failing second port call reaches the caller, ends in `STATUS_ROLLEDBACK` and discards the recorded write. The row-level rollback with real rows is PIN-AC-0452 (P0-E04-T02).
- The same use case built by an `@Produces` method was observed to run without a transaction (`STATUS_NO_TRANSACTION`, no completion). That pins PIN-AC-0448 and is the reason for the A3 producer refusal.
- `application` now declares `io.quarkus:quarkus-narayana-jta` itself instead of getting it only through Hibernate ORM.

## 2026-10-08 — TASK-5.1 error families and code table

- `RekordException` (`app.rekord.domain.shared.error`) is sealed over `NotFoundException`, `RejectedException` (with `Kind` VALIDATION or CONFLICT), `NotPermittedException` and `UpstreamUnavailableException`. It carries an `ErrorCode` and a message, never a status (FW-C-10).
- `ErrorCode` has the 43 codes rekord-api throws through `ApiException`. Left out: the six codes rekord-api never produces (EMPTY_FOLDER, BAD_FORMAT, NO_COUPLE, BAD_TOKEN_KIND, BAD_CODE, NO_BUILD), `UNKNOWN` (only the catch-all mapper of TASK-5.2 emits it), and the seven Python-only codes (UX-10). Adding a code means adding its `ErrorStatusTable` row in the same change.
- `ErrorStatusTable` (`app.rekord.application.error`) is the only place where a code in a family becomes an HTTP status. The family follows rekord-api's status: 404 NotFound; 400, 409, 410, 413, 422, 429 Rejected; 401, 403 NotPermitted; 502, 503 UpstreamUnavailable. It has 45 rows (NO_LIBRARY and SPOTIFY_FETCH_FAILED have two each), is not a CDI bean, and building it throws on a duplicate pair. The envelope mapper (TASK-5.2) and the 409 race answer (TASK-5.5) are not in it.
- `ErrorCodeContractTest` binds the domain `ErrorCode` to the pinned contract enum (`components.schemas.ErrorCode.enum`, read from `contract.spec`), so a code missing from the enum fails the build (PIN-20-0205).

## 2026-10-08 — TASK-35 image job on main only

- The `image` job runs only on a push to main (after a merge), not on pull requests or feature branches; PRs are verified by `build` only. Requested by the user to keep image builds off PR pushes.

## 2026-10-08 — TASK-31.1 release image

- **Supersedes** the TASK-1.3 bullet "never pushed (UD-13.e): no registry login, no secret". On a push to main, after `build` is green and `image-check.sh` passes, the `image` job pushes `ghcr.io/<owner/name lowercased>:<full commit sha>` and `:latest`.
- The job signs in with `github.token` (job permission `packages: write`, no stored secret) and signs out with `if: always()`. Only plain `docker login`/`tag`/`push` are used, no new action.
- The tag logic is `.github/scripts/image-tags.sh` (tested by `ImageTagsTest`); the workflow structure is pinned by `CiWorkflowTest`.
- Package visibility and the repository's Actions access to the package are GitHub settings owned by the user, not changed by this ticket.
- The first real push is proven by the first main run after the merge (run id and SHA to be filled in after it).

## 2026-10-08 — TASK-24.3 scoring and buckets

- Playlist membership enters `TrackMatcher.matchOne` as a track-id → playlist-names map (a third argument; the two-argument form passes `Map.of()`), not as a field of `LibraryIndex.Track`. A missing id or a null map means no playlist.
- The nudge (0.02 per playlist, at most 3) only orders candidates; the bucket reads the raw scores, and its margin guard also passes when the leader is in a playlist and the runner-up is in none.
- UD-19.c: auto also needs the leader to be the requested song, through `Signature.songOf` (normalised artist and core title, no version). A null or empty-normalised artist gives null, so such a query or a filename-only file is never auto.
- Of rekord-api's `Matcher.matchOne` only remembered choices (P3-E05-T02) remain unported.
- UD-19.c compares the whole normalised artist field, so "A, B", "A & B" or "A feat. B" written into the artist field is a different song from "A" and stays ambiguous even at score 1.0 (pinned by `an_extra_artist_on_either_side_is_never_auto_even_at_1`), while a featured artist written into a title is dropped by the core-title split and still allows auto. Widening this (for example a token-set artist identity, as the scored artist facet already uses) is an open owner decision, not part of TASK-24.3.

## 2026-10-08 — TASK-5.2 error envelope mapper

- `ErrorEnvelopeMapper` (package `app.rekord.application.error`) answers every refusal raised inside JAX-RS as `{"detail":{"code","message"}}` with `application/json`, like rekord-api. Fixtures in `application/src/test/resources/fixtures/error-*.json` were recorded from rekord-api at commit `ec65ae35c182e6e25f571c76d45b15a78f183c10` (a scratch copy plus a probe resource; rekord-api itself was not edited). The ITs drive the mapper through the test-only `ErrorEnvelopeProbeResource` under `/api/test-only/error-envelope/`, a prefix that stays clear of the `/api/test/` paths of TASK-5.6 and TASK-6.5. The 413 answer is Vert.x's own: no body and no `Content-Type`. Two more answers are not the envelope: the role-denied `io.quarkus.security.ForbiddenException` (next bullet) and a `WebApplicationException` that carries its own entity, which the framework writes as it is.
- There is no mapper for `io.quarkus.security.ForbiddenException`; the authentication and authorization mappers cover `AuthenticationFailedException`, `UnauthorizedException` and `jakarta.ws.rs.ForbiddenException`.
- The 422 `VALIDATION_FAILED` message joins `<last path segment> <message>` with `"; "`, distinct, in the order the violation set gives. Sorting them is TASK-5.6's deviation, not done here.
- The Throwable catch-all logs once per 500 answer: logger `app.rekord.application.error.ErrorEnvelopeMapper`, level ERROR, message `Unhandled exception`, no parameters, and a `RedactedCause` as the throwable (class names and frames of the cause chain and suppressed exceptions, no messages). Nothing is logged for a 4xx. rekord-api logs the original exception with its messages; this deliberately differs to keep personal data and UD-19.f values out of the log.
- The same ERROR line also comes from two 500 answers that are not a plain `Throwable`: a family error whose (code, family) pair has no `ErrorStatusTable` row (a programming error; rekord-api has no such case), and a `WebApplicationException` of 500 and above (503 and exactly 500 are tested). A `WebApplicationException` below 500 keeps its status, answers `UNKNOWN` "That request could not be handled." and logs nothing.
- New dependencies in `application/build.gradle.kts`: `io.quarkus.security:quarkus-security` and `io.quarkus:quarkus-hibernate-validator`. `application.properties` sets `quarkus.http.limits.max-body-size=10240K` explicitly.
