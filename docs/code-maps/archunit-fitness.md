---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `archunit-fitness` (project: `shop-backend`)

> Thin by design: below the code-map frequency threshold on purpose — a single
> repo-wide suite, not one per module.

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`TransactionalConventionTest.java`](exemplars/transactional-convention-test.md#archunit-fitness) | One centralized `@AnalyzeClasses` suite scanning the whole `com.acme.shop` tree; `classes()`/`noClasses()` + `.because(...)` per rule | test (`application`) |

### Excerpts
Whole-repo scan from one test class (full source in the [exemplar leaf](exemplars/transactional-convention-test.md#archunit-fitness)):
```java
@AnalyzeClasses(packages = "com.acme.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class TransactionalConventionTest {
```

Rule style: one assertion plus a reason:
```java
classes()
        .that().haveSimpleNameEndingWith("UseCase")
        .and().resideInAPackage("..usecase..")
        .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
        .because("All use cases must be annotated with @jakarta.transaction.Transactional for transaction demarcation")
```

### Edge cases
Use-case class annotated with Spring's transaction annotation:
```java
@org.springframework.transaction.annotation.Transactional
class SubmitOrderUseCase { }
```
Expected: the suite fails with the rule that bans Spring's `@Transactional` on use-cases.

Domain type annotated with Jakarta transactions:
```java
@jakarta.transaction.Transactional
record OrderCode(String value) { }
```
Expected: the suite fails because transaction boundaries belong outside the domain layer.

Controller annotated with any transaction marker:
```java
@jakarta.transaction.Transactional
class CustomerController { }
```
Expected: the suite fails because controllers are not the transaction boundary in this repo.

## Local conventions (the project facts the skill omits)
- Package root: `application/src/test/java/com/acme/shop/architecture/` — the one place fitness
  rules live; `application` is chosen because it already depends on every module, so
  `@AnalyzeClasses(packages = "com.acme.shop")` sees the whole codebase from one test source set.
- Naming shape: `*ConventionTest`, one `@ArchTest` method per rule, always with `.because("...")`.
- Rules currently enforced: `TransactionalConventionTest` (use-case classes carry
  `jakarta.transaction.Transactional`, never Spring's; domain/controller classes carry neither) and
  `BatchJobLoggingConventionTest` (every `@Scheduled` method also carries `@BatchJob`, and vice versa).
- Config / wiring: none beyond the ArchUnit JUnit 5 extension already on the test classpath.

## Frequency & coverage (why this stays below the code-map threshold)
- `ArchRuleDefinition` occurs in exactly 2 files / 1 module (as of `abc1234`), matching the first
  harvest's count — deliberate, since the suite is centralized rather than per-module.

## Drift / exceptions
- **Candidate, not a rule yet:** a real module-dependency violation is currently parked —
  `storefront-domain/build.gradle.kts` declares `api(project(":account-domain"))`, i.e. `storefront-domain`
  depends on `account-domain`. The suite has no rule against cross-capability domain-module dependencies
  today, so this is not flagged. The natural home for such a rule is this same suite
  (`application/.../architecture/`) — e.g. a `noClasses().that().resideInAPackage("..storefront.domain..")
  .should().dependOnClassesThat().resideInAPackage("..account.domain..")` rule — but adding it is
  deliberately left for a future ticket, not this scan (this map only records the observation).

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'ArchRuleDefinition'` (2 files / 1 module); manual `grep 'account-domain' storefront-domain/build.gradle.kts` (1 match)
