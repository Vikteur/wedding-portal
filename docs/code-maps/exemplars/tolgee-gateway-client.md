---
runtime: lazy
source: tolgee-gateway/build.gradle.kts
serves: [openapi-rest-client-codegen]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `tolgee-gateway/build.gradle.kts` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A vendor-facing REST **client** generated from an in-repo copy of the vendor's own OpenAPI spec
(`src/main/openapi/tolgee-openapi-spec.json`) — not the server-side codegen (`shop.openapi-conventions`, which
generates resource stubs from *this project's own* central spec). Siblings using the same shape:
`typesense-gateway/build.gradle.kts` (downloads the vendor spec at build time rather than vendoring
a copy) and `partner-gateway` (which generates SOAP clients through Quarkus CXF).

## `quarkus-openapi-generator`, MicroProfile REST Client, vendor spec {#openapi-rest-client-codegen}
**Serves:** [`openapi-rest-client-codegen`](../openapi-rest-client-codegen.md)

The build applies the `io.quarkus` plugin and the Quarkiverse `quarkus-openapi-generator` extension,
which generates MicroProfile REST Client interfaces (`@RegisterRestClient(configKey = "tolgee")`) during
`quarkusGenerateCode`. Only the APIs present in the vendored spec are generated — the spec is kept
sliced to the Translations, Export and Tags tags — and the generated tree under
`build/classes/java/quarkus-generated-sources/` joins the normal Java compilation automatically.

### Source (pseudonymized)
```kotlin
plugins {
    id("shop.bom-conventions")
    id("io.quarkus")
}

val openApiGeneratorVersion = "2.9.0"

dependencies {
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator:$openApiGeneratorVersion")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.wiremock:wiremock-standalone")
}
```

```properties
# tolgee-gateway/src/main/resources/application.properties
quarkus.openapi-generator.codegen.validateSpec=false
quarkus.openapi-generator.codegen.spec.tolgee_openapi_spec_json.base-package=com.acme.shop.tolgee.gateway.client
quarkus.openapi-generator.codegen.spec.tolgee_openapi_spec_json.config-key=tolgee
quarkus.rest-client.tolgee.url=${properties.endpoint.tolgee}
```

### Edge cases
Restrict the generated API surface:
```properties
# slice src/main/openapi/tolgee-openapi-spec.json to the Translations and Tags paths
quarkus.openapi-generator.codegen.include=tolgee-openapi-spec.json
```
Expected: only those API interfaces are emitted, while the extension still emits every model they reference.

Clean checkout build:
```kotlin
plugins { id("io.quarkus") }
```
Expected: `compileJava` depends on `quarkusGenerateCode`, so the client regenerates automatically; no manual pre-step is required.

Generated tree drift:
```text
build/classes/java/quarkus-generated-sources/open-api-json/
```
Expected: the client stays disposable under `build/` and is never hand-edited or committed.

## Provenance
- Scanned at: `abc1234` · tool/query: manual read of `tolgee-gateway/build.gradle.kts`, `typesense-gateway/build.gradle.kts`, `partner-gateway/build.gradle.kts`
