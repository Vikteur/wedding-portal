---
runtime: lazy
generated-by: manual (audit H4 follow-up, pseudonymized)
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map - `clean-code` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> The skill says *how* to write clean code. This is what this repo actually enforces, what it merely
> conventionally does, and - importantly - what nothing checks.

## What is mechanically enforced

| Concern | Enforced by | Where |
|---|---|---|
| Language level | Java 21 toolchain + `options.release = 21` | `build-logic/src/main/kotlin/shop.java-conventions.gradle.kts` |
| Encoding | UTF-8, compiler and test JVM | same |
| Parameter names retained | `-parameters` compiler arg (Spring MVC needs it for `@PathVariable` / `@RequestParam` without an explicit name) | same |
| Coverage | JaCoCo per module, aggregated by `jacocoRootReport`, uploaded to SonarCloud | root `build.gradle.kts` |
| Static analysis | SonarCloud, project key `shop-backend`, org `acme-platform` | root `build.gradle.kts`; run by `./gradlew clean build sonar` in `backend.yml` |
| Layer purity | ArchUnit suite - see [archunit-fitness] | `application/src/test/java/com/acme/shop/architecture/` |

**There is no formatter and no style linter.** No spotless, checkstyle, ktlint, palantir or PMD is
applied anywhere, and there is no `.editorconfig`. This is why the `lint-format` hook gate was
deliberately unwired rather than left as inert scaffolding. **Do not reformat files you are not
otherwise changing:** a whitespace-only diff has no gate behind it and only costs review attention.

### Excerpts
Compiler, test and coverage defaults are centralized in the convention plugin:
```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "512m"
    jvmArgs("-Dfile.encoding=UTF-8", "-XX:+UseG1GC")
}
```

Root build wiring aggregates JaCoCo and ships the reports to Sonar:
```kotlin
configure<SonarExtension> {
    properties {
        property("sonar.projectKey", "shop-backend")
        property("sonar.projectName", "Shop-backend")
        property("sonar.organization", "acme-platform")
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
}
```

### Edge cases
Mutable Lombok on a domain value object:
```java
@Data
public class OrderCode {
    private String code;
}
```
Expected: challenge it. The local style favors immutable domain types (`@Value` or `record`) unless mutability is unavoidable.

Field injection:
```java
@Autowired
private OrderRepository orderRepository;
```
Expected: reject it. Constructor injection via `@RequiredArgsConstructor` and `final` fields is the house style.

Whitespace-only reformat of an untouched file:
```diff
-    public void run() {
+  public void run() {
```
Expected: avoid it. No formatter makes this deterministic, and no gate rewards the churn.

## Naming shapes actually used (main sources, counts as of `abc1234`)
`*Mapper` 105 - `*UseCase` 91 - `*Repository` 91 - `*Request` 56 - `*Factory` 43 - `*Gateway` 35 -
`*Controller` 35 - `*Config` 35 - `*Exception` 31 - `*Entity` 14 - `*Response` 5

Tests: `*Test` 416 (the convention - **not** `*Tests`; the single `ApplicationTests` is the
exception), `*Mother` 62 (object mothers, see [object-mother-builders]), `*TestConfig` 16.

`*UseCase` is the interactor and `*Repository` is *both* the usecase-owned port and its adapter
implementation - the module tells you which. See [clean-architecture].

## Lombok is the house style
`lombok.config` sets exactly one option, `lombok.addLombokGeneratedAnnotation = true`, so generated
members carry `@lombok.Generated` and JaCoCo/Sonar exclude them from coverage. Usage, by frequency:

`@RequiredArgsConstructor` 226 - `@Getter` 183 - `@Value` 136 - `@Builder` 134 -
`@NoArgsConstructor` 104 - `@Slf4j` 56 - `@Setter` 29 - `@Data` 15 - `@AllArgsConstructor` 13

- **Constructor injection via `@RequiredArgsConstructor` + `final` fields** is the dominant DI shape -
  no field injection. See [bean-config-di].
- **`@Value` (immutable) is preferred over `@Data`** in domain types: 136 to 15. A domain type whose
  invariants are enforced in a factory must not expose setters - `@Setter` in a `-domain` module is a
  smell worth challenging.
- **`@Slf4j` for logging, never a hand-rolled logger field.** What may be logged is a policy question,
  not a style one - see [security-review] before logging anything derived from a customer identifier.

## Anti-patterns this repo has already decided against
- Field injection (`@Autowired` on a field) - constructor injection everywhere instead.
- Reformatting on save across whole files; there is no formatter to make it deterministic.
- Business logic in a `@Configuration` class - wiring only (see [bean-config-di]).
- Hand-editing generated code: OpenAPI stubs under `*/build/generated/*` and JAXB types under
  `*/build/generated-sources/*` are guarded by `.github/hooks/guard-generated.globs`, which denies the
  write outright. Change the contract, not the output.

## Provenance
- Scanned at: `abc1234` - tool/query: `shop.java-conventions.gradle.kts` and root `build.gradle.kts`
  read in full; formatter search for spotless, checkstyle, ktlint, palantir and PMD (0 hits); naming
  and Lombok counts from `git ls-files -- '*.java' | xargs grep -ho`. Not yet scanner-harvested.

[archunit-fitness]: archunit-fitness.md
[bean-config-di]: bean-config-di.md
[clean-architecture]: clean-architecture.md
[object-mother-builders]: object-mother-builders.md
[security-review]: security-review.md
