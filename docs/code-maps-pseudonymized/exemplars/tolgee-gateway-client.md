---
runtime: lazy
generated-by: pattern-scanner
source: tolgee-gateway/build.gradle.kts
serves: [openapi-rest-client-codegen]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `tolgee-gateway/build.gradle.kts` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
A vendor-facing REST **client** generated from an in-repo copy of the vendor's own OpenAPI spec
(`tolgee-openapi-spec.json`) — not the server-side codegen (`shop.openapi-conventions`, which
generates controller stubs from *this project's own* central spec). Siblings using the same shape:
`typesense-gateway/build.gradle.kts` (downloads the vendor spec at build time rather than vendoring
a copy) and `partner-gateway` (generates SOAP clients instead, see [soap-cxf-gateway]).

## `org.openapi.generator`, `library = "restclient"`, vendor spec {#openapi-rest-client-codegen}
**Serves:** [`openapi-rest-client-codegen`](../openapi-rest-client-codegen.md)

The build uses `generatorName = "java"` and `library = "restclient"` (Spring's `RestClient`, not
`RestTemplate`/WebClient), generates only the APIs named in `globalProperties["apis"]`, and wires
the generated tree under `${layout.buildDirectory}/generated` into the normal Java source set.

### Source (pseudonymized)
```kotlin
plugins {
    id("shop.bom-conventions")
    id("org.openapi.generator") version "7.18.0"
}

openApiGenerate {
    generatorName.set("java")
    library.set("restclient")
    inputSpec.set("${projectDir}/tolgee-openapi-spec.json")
    apiPackage.set("com.acme.shop.tolgee.gateway.client.api")
    invokerPackage.set("com.acme.shop.tolgee.gateway.client")
    skipValidateSpec.set(true)
    globalProperties.set(mapOf(
        "apis" to "Translations,Export,Tags",
        "models" to "",
        "supportingFiles" to "",
        "apiTests" to "false",
        "modelTests" to "false"
    ))
    configOptions.set(mapOf(
        "useJakartaEe" to "true",
        "containerDefaultToNull" to "true"
    ))
    outputDir.set("${layout.buildDirectory.get()}/generated")
}

sourceSets { main { java { srcDir("${layout.buildDirectory.get()}/generated/src/main/java") } } }
tasks.compileJava { dependsOn(tasks.openApiGenerate) }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.swagger.parser.v3:swagger-parser")
    implementation("org.openapitools:jackson-databind-nullable")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("jakarta.annotation:jakarta.annotation-api")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.wiremock:wiremock-standalone")
}
```

### Edge cases
Restrict the generated API surface:
```kotlin
globalProperties.set(mapOf("apis" to "Translations,Tags"))
```
Expected: only those API classes are emitted, while the generator still emits all required models/support files.

Clean checkout build:
```kotlin
tasks.compileJava { dependsOn(tasks.openApiGenerate) }
```
Expected: `compileJava` regenerates the client automatically; no manual pre-step is required.

Generated tree drift:
```kotlin
outputDir.set("${layout.buildDirectory.get()}/generated")
```
Expected: the client stays disposable under `build/` and is never hand-edited or committed.

## Provenance
- Scanned at: `abc1234` · tool/query: manual read of `tolgee-gateway/build.gradle.kts`, `typesense-gateway/build.gradle.kts`, `partner-gateway/build.gradle.kts`

[soap-cxf-gateway]: ../soap-cxf-gateway.md
