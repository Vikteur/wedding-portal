# jvm-testing — wedding-portal

The project facts the `jvm-testing` skill applies to. No code yet (TASK-47 started over); these are the conventions
the first skeleton ticket sets up.

- **Test HTTP port 0.** Set `%test.quarkus.http.test-port=0` in `application.properties`, so parallel worktrees can
  run their test suites at the same time without a port clash. A fixed test port cost extra Gradle runs before the
  restart (umbrella `docs/archon/retro-synthesis.md`, row 11).
- **Line endings: check the blob, not the work tree.** `.gitattributes` forces LF for scripts, `*.json` and `*.md`.
  A check or a hash over a committed file reads `git show HEAD:<file>`, never the checked-out copy, which
  `core.autocrlf` may have turned to CRLF on Windows (row 12).

## Scaffold
Render the base case with `scripts/scaffold.sh <template> '<json on one line>' <target>` from the repo root. Each
template's header comment lists its exact variables; the script refuses to overwrite an existing file. The base case
is the happy path (plus one missing-field case per field for a factory); every other scenario in the part's
acceptance criteria is yours to add.

| Template | Target | Variables | Left to you |
|---|---|---|---|
| [domain/factory-test](scaffold/domain/factory-test.mustache) | `<capability>-domain/src/test/java/<pkg>/domain/<Name>FactoryTest.java` | `package`, `Name`, `fields[type, name, pascal, example, last]` | cross-field and format rules; remove the missing-field case of an optional field |
| [adapter/controller-test](scaffold/adapter/controller-test.mustache) | `<capability>-adapter/src/test/java/<pkg>/adapter/controller/<Name>ControllerTest.java` | `package`, `Name`, `useCase`, `OperationId`, `Domain`, `fixture`, `path`, `expectedJson` (from the contract's example) | the use-case mock on the module's abstract controller test; error statuses; forbidden and unauthenticated cases; parameters and bodies |
| [adapter/jpa-repository-test](scaffold/adapter/jpa-repository-test.mustache) | `<capability>-adapter/src/test/java/<pkg>/adapter/repository/<Name>JpaRepositoryTest.java` | `package`, `Name`, `Key`, `keyExample`, `fields[name, example]` | other query methods and writes |
| [usecase/usecase-test](scaffold/usecase/usecase-test.mustache) | `<capability>-usecase/src/test/java/<pkg>/usecase/<Name>UseCaseTest.java` | the `usecase` template's variables plus `name`, `fixture` | error paths; empty results; validation failures |

The Mother these tests use comes from `object-mother-builders`; the mapper test from `domain-to-dto-mapper-tests`.
