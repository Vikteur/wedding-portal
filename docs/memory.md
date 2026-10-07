# Memory — decision record

## 2026-10-07 — TASK-1.1 build skeleton

Quarkus platform: 3.39.1
- PIN-AC-1255 / slice 18 P-1. Settled for Gradle only by the CI run of the TASK-1.1 PR, read by commit SHA.
- No fallback to a newer Quarkus line was needed: the build was green on 3.39.1 with Gradle and Java 25.

Gradle: 9.8.0
- Wrapper with the distribution checksum pinned (`distributionSha256Sum`). Java toolchain 25 and `options.release = 25` in every module.

Executor (UD-17): the Archon build-feature workflow (.archon/workflows/build-feature.yaml) running Claude Code; code and tests by Claude Sonnet 5.5 (@build), planning and review by Claude Opus 5.5 (@analyse), per UD-21.a.
- The user chose this executor by starting TASK-1.1 through the Archon build-feature workflow (Archon run 743a7815-4cff-4f5b-bca9-55841a44d63b); `.archon/config.yaml` and UD-21.a record the model split.

Module layout: follows architecture-conventions §2.2 and §14.1. Library beans are discovered through each module's own Jandex index (PIN-AC-0153).
