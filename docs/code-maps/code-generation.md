---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `code-generation` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`shop.java-conventions.gradle.kts`](exemplars/java-conventions-plugin.md#code-generation) | The convention plugin applied to 60 of 62 modules, replacing per-module build boilerplate | `build-logic` |

### Excerpts
Convention plugin applied repo-wide (full source in the [exemplar leaf](exemplars/java-conventions-plugin.md#code-generation)):
```kotlin
plugins {
    `java-library`
    jacoco
}

group = "com.acme.shop"
version = rootProject.version
```

Centralized compiler and test wiring:
```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}
```

### Edge cases
Module with no convention plugin:
```kotlin
plugins {
    id("java")
}
```
Expected: this is a drift candidate unless the module is one of the known exceptions (root project or another non-standard build-only module).

Module that only declares `junit-jupiter-api`:
```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}
```
Expected: the shared plugin still supplies the runtime launcher and engine so tests run consistently.

Module exporting Mothers for reuse:
```kotlin
plugins {
    id("shop.test-jar-producer")
}
```
Expected: the module publishes its test helpers once, and consumers depend on the produced `testArtifacts` configuration.

## Local conventions (the project facts the skill omits)
- Package root: `build-logic/src/main/kotlin/shop.<name>-conventions.gradle.kts` (4
  convention plugins: `java-conventions`, `bom-conventions`, `openapi-conventions`,
  `test-jar-producer`).
- Naming shape: `shop.<name>-conventions`, applied via
  `plugins { id("shop.<name>-conventions") }` in each module's `build.gradle.kts`.
- Required collaborators / base types: `shop.openapi-conventions` generates the `*Api`
  interfaces controllers implement (see [api-first-controller]);
  `shop.test-jar-producer` is what lets a domain module export an object mother as a
  reusable test-jar (see [object-mother-builders]).
- Config / wiring: applied per-module, not via `allprojects`/`subprojects` in the root
  `build.gradle.kts`.

## Frequency & coverage (why this earned a skill)
- Occurrences: convention-plugin `id(...)` references found in 60 of 62 module `build.gradle.kts`
  files (as of `abc1234`); `openapi-conventions` specifically in 15 modules (the ones with a
  generated REST API — matches the `api-first-controller` module list).

## Drift / exceptions
- 2 of 62 `build.gradle.kts` files (root + one non-standard module) do not apply a convention
  plugin — expected (root project has no source to compile).

## Provenance
- Scanned at: `abc1234` · tool/query: manual `Get-ChildItem build.gradle.kts` + content match on `shop\.(java|openapi|bom|test-jar)`

[api-first-controller]: api-first-controller.md
[object-mother-builders]: object-mother-builders.md
