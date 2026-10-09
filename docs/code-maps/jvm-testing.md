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
| [domain/factory-test](../Moustache%20scripts/domain/factory-test.mustache) | `<capability>-domain/src/test/java/<pkg>/domain/<Name>FactoryTest.java` | `package`, `Name`, `fields[type, name, pascal, example, last]` | cross-field and format rules; remove the missing-field case of an optional field |
| [adapter/controller-test](../Moustache%20scripts/adapter/controller-test.mustache) | `<capability>-adapter/src/test/java/<pkg>/adapter/controller/<Name>ControllerTest.java` | `package`, `Name`, `UseCase`, `useCase`, `OperationId`, `Domain`, `fixture`, `path`, `expectedJson` (from the contract's example); needs `quarkus-junit5-mockito` | `@TestSecurity` for secured endpoints; error statuses; forbidden and unauthenticated cases; parameters and bodies |
| [adapter/default-repository-test](../Moustache%20scripts/adapter/default-repository-test.mustache) | `<capability>-adapter/src/test/java/<pkg>/adapter/repository/Default<Name>RepositoryTest.java` | `package`, `Name`, `Key`, `keyExample`, `fields[name, example]` | other query methods and writes; runs against Dev Services, so it belongs in the integration set |
| [usecase/usecase-test](../Moustache%20scripts/usecase/usecase-test.mustache) | `<capability>-usecase/src/test/java/<pkg>/usecase/<Name>UseCaseTest.java` | the `usecase` template's variables plus `name`, `fixture` | error paths; empty results; validation failures |

The Mother these tests use comes from `object-mother-builders`; the mapper test from `domain-to-dto-mapper-tests`.
