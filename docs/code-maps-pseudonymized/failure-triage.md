---
runtime: lazy
generated-by: manual (duplication-review follow-up, pseudonymized)
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map - `failure-triage` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where the failing signals live
- **CI run logs**: GitHub Actions - the `backend` job in `pipeline.yml` -> `backend.yml`
  (`./gradlew clean build sonar`); fetch a failing job's log by run or SHA via `gh run view <id> --log-failed`.
- **Test reports**: per-module `build/reports/tests/test/` and `build/test-results/test/*.xml`
  after a local `./gradlew :<module>:test`.
- **Application logs (local)**: `docker-compose.yml` services at repo root; Spring Boot console
  output from the `application` module.

## Repro commands
- One module's tests (fast, targeted): `./gradlew :<module>:test` (for example `:consent-usecase:test`).
- One test class: `./gradlew :<module>:test --tests '<FQCN>'`.
- Full gate exactly as CI runs it: `./gradlew clean build`.
- Contract-drift suspicion: `./gradlew clean build` regenerates from the live spec first - a
  compile failure in an adapter module is the contract-drift signal ([layer-model] `Contract sync`).

### Excerpts
The pipeline keeps the backend gate behind the migration check:
```yaml
jobs:
  flyway-analyzer:
    runs-on: ubuntu-latest
  backend:
    needs: flyway-analyzer
    uses: ./.github/workflows/backend.yml
```

The backend workflow runs the same Gradle gate every time:
```yaml
- name: Gradle build and code quality scan
  run: ./gradlew clean build sonar -Dsonar.projectKey=shop-backend -Dsonar.projectName=Shop-backend
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

### Edge cases
A single failing repository test:
```bash
./gradlew :storefront-adapter:test --tests 'com.acme.shop.storefront.adapter.repository.PageJpaRepositoryTest'
```
Expected: start here instead of rerunning the whole repo; widen only if the targeted repro is green.

Adapter compile break right after contract regeneration:
```text
Task :account-adapter:compileJava FAILED
error: constructor AccountResponse(...) cannot be applied to given types
```
Expected: suspect contract drift or codegen fallout before hunting for unrelated logic bugs.

CI failure with no local repro yet:
```bash
gh run view <run-id> --log-failed
```
Expected: read the failing job first; do not guess from memory or rerun the entire pipeline blindly.

## Debug artifact convention
- A triage writes `docs/<epic>/<ticket>/debug.md` - `{root cause, affected part, suggested fix,
  evidence, named repro test}`; the fix flows through the test-first sub-loop, never the debugger.

## Known problem areas (from past writeups)
- Typesense integration: startup-order race + swallowed init exception + port/profile ambiguity -
  root-caused in the companion troubleshooting note `docs/search-index-init-v30-troubleshooting.md`;
  version migrations follow `docs/search-index-migration-cookbook.md`.
- `TODO(verify)`: no flaky-test retry config or quarantine list found in this repo - flakiness is
  handled per ticket, not by tooling.

## Provenance
- Authored from `.github/workflows/{pipeline,backend}.yml`, `docs/search-index-*.md`, and the ticket
  docs convention (`docs/<epic>/<ticket>/debug.md`) at `abc1234`. Not yet scanner-harvested.

[layer-model]: ../layer-model.md
