# Memory — decision record

## 2026-10-07 — TASK-1.1 build skeleton

Quarkus platform: 3.39.1
- PIN-AC-1255 / slice 18 P-1. Settled for Gradle only by the CI run of the TASK-1.1 PR, read by commit SHA. That run is still pending.
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
