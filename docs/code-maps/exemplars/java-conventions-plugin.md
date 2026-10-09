---
runtime: lazy
source: build-logic/src/main/kotlin/shop.java-conventions.gradle.kts
serves: [code-generation, java]
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `build-logic/.../shop.java-conventions.gradle.kts` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The convention plugin replacing a shared parent-POM `<build>` block — applied by most of the repo's
`build.gradle.kts` files (`plugins { id("shop.java-conventions") }`). Its siblings
(`shop.bom-conventions`, `shop.openapi-conventions`, and `shop.test-jar-producer`) live in the same
`build-logic/src/main/kotlin/` directory.

## One convention plugin, applied everywhere {#code-generation}
**Serves:** [`code-generation`](../code-generation.md)

Centralizes: Java toolchain version (21), the Quarkus BOM (`enforcedPlatform("io.quarkus.platform:quarkus-bom:<version>")`),
Lombok annotation-processor wiring, a Jandex index so ArC discovers CDI beans in library modules,
compiler flags (`-parameters`, required for Jackson/records and CDI parameter-name resolution), JUnit
Platform test config plus explicit launcher/engine pinning (comment explains *why*: Gradle 8's bundled
launcher can lag the JUnit 5 version managed by the Quarkus BOM), and JaCoCo reporting — a module's own
`build.gradle.kts` only declares `plugins { id("shop.java-conventions") }` plus its dependencies (for
example `io.quarkus:quarkus-rest-jackson`, `io.quarkus:quarkus-arc`, `io.quarkus:quarkus-junit5`,
`io.quarkus:quarkus-junit5-mockito`, `io.rest-assured:rest-assured`), never repeats this setup. The
`io.quarkus` plugin itself is applied only by the runnable application module and by modules that run
Quarkus codegen.

## Java 21 toolchain, applied repo-wide {#java}
**Serves:** [`java`](../java.md)

`java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }` — set once here, inherited by
every module that applies the plugin; a module never pins its own toolchain version. Paired with
`options.release.set(21)` on `JavaCompile`, so bytecode and API surface are both pinned to 21 in one
place.

### Source (pseudonymized)
```kotlin
/**
 * Convention plugin applied to every subproject.
 * Replaces the root Maven POM <build> / <pluginManagement> block.
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    jacoco
    // library modules need a Jandex index so ArC discovers their CDI beans
    id("org.kordamp.gradle.jandex")
}

group = "com.acme.shop"
version = rootProject.version

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    val quarkusVersion = "3.20.1"
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))

    val lombokVersion = "1.18.36"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    // -parameters lets Jackson (records, creators) and CDI resolve parameter names via reflection
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "512m"
    jvmArgs("-Dfile.encoding=UTF-8", "-XX:+UseG1GC")
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

dependencies {
    // pin launcher + engine to the BOM-managed JUnit 5; Gradle 8's bundled launcher can lag it
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

val testArtifacts: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
```

### Edge cases
Module that omits the convention plugin:
```kotlin
plugins {
    id("java")
}
```
Expected: that module now owns all absent boilerplate itself and is a drift candidate unless it is one of the known exceptions.

Module that only declares the JUnit API:
```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}
```
Expected: the shared plugin still supplies the launcher and engine at runtime, at the versions managed by the Quarkus BOM.

Domain test helpers published for reuse:
```kotlin
plugins {
    id("shop.test-jar-producer")
}
```
Expected: other modules can consume helpers such as [OrderMother.java](order-mother.md#object-mother-builders) through the `testArtifacts` configuration.

## Provenance
- Scanned at: `abc1234` · tool/query: manual `Get-ChildItem build.gradle.kts` + content match on `shop\.(java|openapi|bom|test-jar)` (60 files / 62 total module build files)
