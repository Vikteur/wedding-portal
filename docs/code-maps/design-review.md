---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map - `design-review` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Architecture constraints to weigh against
- The layer model: [`../layer-model.md`](../layer-model.md) - Clean Architecture triads
  (`<cap>-domain` / `-usecase` / `-adapter`), dependency direction adapter -> usecase -> domain,
  contract-first seam via `openapi-conventions`.
- Executable constraints: the ArchUnit suite in `application/src/test/java/com/acme/shop/architecture/`
  - see [`archunit-fitness.md`](archunit-fitness.md) (includes a parked future-rule candidate:
  the observed `storefront-domain` -> `account-domain` module dependency).

## Where prior decisions live
- Per-ticket: `docs/<epic>/<ticket>/plan.md` (`Notes & Decisions`) and `retro.md`.
- Framework-level: decision tables in the hub repo's `templates/*.template.md` design sections
  (below each `DESIGN BELOW` fence).
- `TODO(verify)`: no dedicated ADR directory found in this repo - decisions ride in ticket docs;
  propose `docs/adr/` only if the team wants standalone records.

### Excerpts
The executable architecture rules are concrete enough to review a proposal against:
```java
noClasses()
        .that().resideInAPackage("..adapter..controller..")
        .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
        .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
        .because("@Transactional belongs on the use case layer, not on controllers")
        .check(classes);
```

One existing exception is visible in the module graph and should stay exceptional:
```kotlin
dependencies {
    api(project(":common-domain"))
    api(project(":account-domain"))
}
```

### Edge cases
Refactor proposal with no characterization test:
```text
"Let's move StorefrontMapper and clean it up first; tests can come later."
```
Expected: push back. The repo rule is tests first, then movement.

New cross-capability domain dependency:
```kotlin
api(project(":invoice-domain"))
```
Expected: treat it as an architecture decision, not a casual convenience dependency.

Proposal to create `docs/adr/` for one-off notes while ticket docs already exist:
```text
"Let's add an ADR folder for this tiny mapper rename."
```
Expected: keep the decision in the ticket docs unless the team explicitly wants repo-wide ADRs.

## Refactor entry rule (project-applied)
- Characterization tests first: the 413-file test corpus (see [`jvm-testing.md`](jvm-testing.md))
  mirrors production packages - a refactor of `<module>` starts from `./gradlew :<module>:test`
  green, and adds coverage before moving anything under-tested.

## Provenance
- Authored from [layer-model], [archunit-fitness], and the ticket-docs
  convention at `abc1234`. Not yet scanner-harvested.

[layer-model]: ../layer-model.md
[archunit-fitness]: archunit-fitness.md
