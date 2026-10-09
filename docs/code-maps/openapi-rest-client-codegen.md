---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `openapi-rest-client-codegen` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`tolgee-gateway/build.gradle.kts`](exemplars/tolgee-gateway-client.md#openapi-rest-client-codegen) | Vendor-spec MicroProfile REST Client codegen (`quarkus-openapi-generator`), generated sources wired into the normal Quarkus build | adapter (`tolgee-gateway`) |

### Excerpts
Vendor REST client generation (full source in the [exemplar leaf](exemplars/tolgee-gateway-client.md#openapi-rest-client-codegen)):
```kotlin
plugins { id("io.quarkus") }
dependencies { implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator:$openApiGeneratorVersion") }
```
```properties
# spec at src/main/openapi/tolgee-openapi-spec.json, sliced to the Translations, Export and Tags tags
quarkus.openapi-generator.codegen.spec.tolgee_openapi_spec_json.base-package=com.acme.shop.tolgee.gateway.client
quarkus.openapi-generator.codegen.spec.tolgee_openapi_spec_json.config-key=tolgee
quarkus.rest-client.tolgee.url=${properties.endpoint.tolgee}
```

Generated `@RegisterRestClient(configKey = "tolgee")` interfaces injected where used:
```java
@RestClient TranslationsApi translationsApi
```

### Edge cases
Only a subset of APIs is needed:
```properties
# keep only the Translations and Tags paths in src/main/openapi/tolgee-openapi-spec.json
quarkus.openapi-generator.codegen.include=tolgee-openapi-spec.json
```
Expected: the extension emits only the API interfaces present in the sliced spec, plus every model they reference.

Build runs from a clean checkout:
```kotlin
plugins { id("io.quarkus") }
```
Expected: `./gradlew build` runs `quarkusGenerateCode` before `compileJava` automatically; no developer manually runs the generator first.

Generated code must not drift:
```text
build/classes/java/quarkus-generated-sources/open-api-json/
```
Expected: generated sources stay under `build/`, are never committed, and are replaced on every regeneration.

## Local conventions (the project facts the skill omits)
- **Server vs client, kept separate on purpose:** `shop.openapi-conventions` (applied by ~15
  modules) generates **server** resource stubs from this project's own central spec. This skill
  covers the opposite direction: a **client** generated from a vendor's spec, applied per-gateway,
  not via a shared convention plugin (each vendor spec is different, so there's nothing to centralize).
- Package root: `<gateway-module>/build.gradle.kts` + `<gateway-module>/src/main/openapi/<spec-file>` +
  `application.properties`; generated code under `build/classes/java/quarkus-generated-sources/`, never
  committed or hand-edited.
- Naming shape: `base-package` `com.acme.shop.<vendor>.gateway.client` (the extension adds `.api` and
  `.model`); `config-key` = `<vendor>`, so the base URL lives at `quarkus.rest-client.<vendor>.url`.
- Required collaborators / base types: `io.quarkiverse.openapi.generator:quarkus-openapi-generator`
  generating MicroProfile REST Client interfaces (`@RegisterRestClient`), backed by
  `io.quarkus:quarkus-rest-client-jackson`; injected with `@RestClient`.
- Config / wiring: the vendor spec is either vendored in-repo under `src/main/openapi/` (`tolgee-gateway`)
  or downloaded at build time from the vendor's own source (`typesense-gateway`, pointed at via
  `quarkus.openapi-generator.codegen.input-base-dir`, download task wired before `quarkusGenerateCode`) —
  both regenerate on a plain `./gradlew build`.

## Frequency & coverage (why this earned a skill)
- 3 vendor-facing gateways generate a client this way (as of `abc1234`): `tolgee-gateway`,
  `typesense-gateway`; `partner-gateway` follows the same generated-client shape for its SOAP clients,
  using a different generator (Quarkus CXF) but following the same "don't hand-write the client" intent.

## Drift / exceptions
- None — both REST-client gateways use identical `quarkus.openapi-generator.codegen.spec.*` config shape
  (only the spec source and selected APIs differ).

## Provenance
- Scanned at: `abc1234` · tool/query: manual read of `tolgee-gateway/build.gradle.kts`, `typesense-gateway/build.gradle.kts`, `partner-gateway/build.gradle.kts`
