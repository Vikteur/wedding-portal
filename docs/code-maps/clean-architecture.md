---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map - `clean-architecture` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

> The skill says *how* Clean Architecture works. This is *where* the layers are in this repo, which
> is not obvious: layering is expressed by **Gradle module**, and only partly mirrored in packages.

## The layer -> module -> package mapping

| Layer | Module shape | Package shape | Count |
|---|---|---|---|
| Domain | `<cap>-domain` | `com.acme.shop.<cap>.domain` | 15 |
| Usecase | `<cap>-usecase` | `com.acme.shop.<cap>.usecase` **and** `com.acme.shop.<cap>.repository` (ports) | 15 |
| Adapter | `<cap>-adapter` | `com.acme.shop.<cap>.adapter.{controller,repository,gateway,config}` | 15 |
| Gateway | `<name>-gateway`, `webservice-config`, `*-hubservices`, `xml-bindings` | `com.acme.shop.<name>.{gateway,config,controller,...}` - **no layer segment** | 7 + 4 |
| Platform | `application`, `logging`, `encryption`, `time` | `com.acme.shop.{configuration,audit,batch,oauth,exceptionhandling,...}` | 4 |

**The trap this map exists to name:** the package segment is not a reliable layer signal.
`consent-usecase/src/main/java/com/acme/shop/consent/repository/` holds usecase-owned **ports**
with no `usecase` in the path, and no gateway module has an `adapter` segment anywhere. Judge the
layer from the **module suffix**, not the package - which is exactly why the instruction globs are
`**/*-usecase/**` and not only `**/usecase/**`.

## The 15 capability triads
`access-matrix` - `product-restriction` - `authentication` - `common` - `consent` - `promotion` -
`storefront` - `merchant` - `campaign` - `account` - `attribute-mapping` - `invoice` - `catalog` -
`vendor-relation` - `order`

Every one is a complete `-domain` / `-usecase` / `-adapter` set: 15/15, no partial triads. A new
capability adds three modules, not one.

## The 7 named gateways
`captcha` - `payments` - `partner` - `tolgee` - `typesense` - `fulfilment` - `loyalty` - plus
`webservice-config` (CXF/WS-Security wiring, 25 files), `partner-hubservices`,
`hubservice-gateway-api` and `xml-bindings` (generated JAXB types). `partner-gateway` alone is 99
files and is the highest-consequence code in the repo.

### Excerpts
The architecture tests pin transaction demarcation to the usecase layer:
```java
classes()
        .that().haveSimpleNameEndingWith("UseCase")
        .and().resideInAPackage("..usecase..")
        .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
        .because("All use cases must be annotated with @jakarta.transaction.Transactional for transaction demarcation")
        .check(classes);

noClasses()
        .that().resideInAPackage("..domain..")
        .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
        .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
        .check(classes);
```

One deliberate cross-capability dependency is visible in the module graph:
```kotlin
plugins {
    id("shop.bom-conventions")
    id("shop.test-jar-producer")
}
dependencies {
    api(project(":common-domain"))
    api(project(":account-domain"))
}
```

### Edge cases
Usecase-owned ports live in a `repository` package but are still usecase code:
```java
package com.acme.shop.consent.repository;

public interface ConsentRepository { }
```
Expected: treat this as an inward-facing port because the module is `consent-usecase`.

Spring transaction annotation on a controller:
```java
@RestController
@org.springframework.transaction.annotation.Transactional
class AccountController { }
```
Expected: architecture violation. Transaction boundaries belong in the usecase layer.

Framework type leaking into the domain:
```java
package com.acme.shop.order.domain;

import jakarta.persistence.Entity;
```
Expected: fail the purity rule or the compile-time module boundary check; domain stays framework-free.

## Local conventions (the project facts the skill omits)
- **Dependency direction is enforced by the module graph**, not by convention: a `-domain` module's
  `build.gradle.kts` declares no Spring or JPA dependency at all, so a framework import in the domain
  fails to compile rather than failing review. Verified: **0** files under `*-domain/src/main` import
  `org.springframework` or `jakarta.persistence` (as of `abc1234`).
- **Runtime fitness rules** live in one place, `application/src/test/java/com/acme/shop/architecture/`,
  run by `:application:archTest` - see [archunit-fitness]. `application` is the module that depends on
  everything, so one `@AnalyzeClasses` suite sees the whole tree.
- **Ports are named `*Repository` and live in the usecase module**; the implementation of the same
  name lives in the adapter module. Both packages are called `repository`, in different modules -
  read the module, not the package.
- **Every module applies `shop.java-conventions`** from `build-logic` (Java 21 toolchain,
  Lombok, JUnit 5, JaCoCo) rather than repeating build config - see [code-generation].

## Drift / exceptions
- `storefront-domain/build.gradle.kts` declares `api(project(":account-domain"))` - a cross-capability
  domain-to-domain dependency. No fitness rule forbids it today; recorded in [archunit-fitness] as a
  parked candidate.
- `common-domain` / `common-usecase` / `common-adapter` is a triad by shape but a shared-kernel by
  role. Depending on it is expected; treat additions to it as widening a shared contract.

## Provenance
- Scanned at: `abc1234` - tool/query: module listing from `settings.gradle.kts` + package-shape reads
  per capability; layer-purity check via `git ls-files -- '*-domain/src/main/**/*.java'` filtered for
  Spring and JPA imports (0 hits). Not yet scanner-harvested.

[archunit-fitness]: archunit-fitness.md
[code-generation]: code-generation.md
