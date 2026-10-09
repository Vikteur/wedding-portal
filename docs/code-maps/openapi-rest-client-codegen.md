---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `openapi-rest-client-codegen` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`tolgee-gateway/build.gradle.kts`](exemplars/tolgee-gateway-client.md#openapi-rest-client-codegen) | Vendor-spec REST client codegen (`restclient` library), generated sources wired into the normal build | adapter (`tolgee-gateway`) |

### Excerpts
Vendor REST client generation (full source in the [exemplar leaf](exemplars/tolgee-gateway-client.md#openapi-rest-client-codegen)):
```kotlin
openApiGenerate {
    generatorName.set("java")
    library.set("restclient")
    inputSpec.set("${projectDir}/tolgee-openapi-spec.json")
    apiPackage.set("com.acme.shop.tolgee.gateway.client.api")
    invokerPackage.set("com.acme.shop.tolgee.gateway.client")
    globalProperties.set(mapOf(
        "apis" to "Translations,Export,Tags",
        "models" to "",
        "supportingFiles" to ""
    ))
}
```

Generated sources wired into the regular compile path:
```kotlin
sourceSets { main { java { srcDir("${layout.buildDirectory.get()}/generated/src/main/java") } } }
tasks.compileJava { dependsOn(tasks.openApiGenerate) }
```

### Edge cases
Only a subset of APIs is needed:
```kotlin
globalProperties.set(mapOf("apis" to "Translations,Tags"))
```
Expected: the generator emits only the selected API classes, while still generating every required model and support file.

Build runs from a clean checkout:
```kotlin
tasks.compileJava { dependsOn(tasks.openApiGenerate) }
```
Expected: `./gradlew build` regenerates the client automatically; no developer manually runs the generator first.

Generated code must not drift:
```kotlin
outputDir.set("${layout.buildDirectory.get()}/generated")
```
Expected: generated sources stay under `build/`, are never committed, and are replaced on every regeneration.

## Local conventions (the project facts the skill omits)
- **Server vs client, kept separate on purpose:** `shop.openapi-conventions` (applied by ~15
  modules) generates **server** controller stubs from this project's own central spec. This skill
  covers the opposite direction: a **client** generated from a vendor's spec, applied per-gateway,
  not via a shared convention plugin (each vendor spec is different, so there's nothing to centralize).
- Package root: `<gateway-module>/build.gradle.kts` + `<gateway-module>/<spec-file>`; generated code under
  `build/generated/src/main/java`, never committed or hand-edited.
- Naming shape: `apiPackage` / `invokerPackage` under `com.acme.shop.<vendor>.gateway.client(.api)`.
- Required collaborators / base types: `generatorName = "java"`, `library = "restclient"`
  (Spring `RestClient`), `useJakartaEe = true`.
- Config / wiring: the vendor spec is either vendored in-repo (`tolgee-gateway`) or downloaded at build
  time from the vendor's own source (`typesense-gateway`) — both wire generation into
  `tasks.compileJava` via `dependsOn(tasks.openApiGenerate)` so a plain `./gradlew build` regenerates.

## Frequency & coverage (why this earned a skill)
- 3 vendor-facing gateways generate a REST client this way (as of `abc1234`): `tolgee-gateway`,
  `typesense-gateway`; `partner-gateway` follows the same generated-client shape for its SOAP clients
  (see [soap-cxf-gateway] — different generator, same "don't hand-write the client" intent).

## Drift / exceptions
- None — both REST-client gateways use identical `openApiGenerate` config shape (only the spec source and
  selected APIs differ).

## Provenance
- Scanned at: `abc1234` · tool/query: manual read of `tolgee-gateway/build.gradle.kts`, `typesense-gateway/build.gradle.kts`, `partner-gateway/build.gradle.kts`

[soap-cxf-gateway]: soap-cxf-gateway.md
