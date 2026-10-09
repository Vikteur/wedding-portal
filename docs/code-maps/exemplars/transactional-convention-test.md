---
runtime: lazy
source: application/src/test/java/com/acme/shop/architecture/TransactionalConventionTest.java
serves: [archunit-fitness]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `application/.../architecture/TransactionalConventionTest.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
One of the repo-wide ArchUnit fitness suites in `application/src/test/java/com/acme/shop/architecture/`
— this one enforces transaction-annotation conventions; its sibling,
`BatchJobLoggingConventionTest.java` in the same package, enforces that every `@Scheduled` method also
carries the project's own `@BatchJob` logging annotation (and vice versa).

## One centralized suite, not per-module {#archunit-fitness}
**Serves:** [`archunit-fitness`](../archunit-fitness.md)

`@AnalyzeClasses(packages = "com.acme.shop", importOptions = ImportOption.DoNotIncludeTests.class)`
scans the *entire* production codebase from one test class in `application` (the module that already
depends on everything) rather than one ArchUnit suite per module. Four rules here, each a
`classes()` or `noClasses()` one-liner with a `.because(...)` explaining the rule in the failure message:
`*UseCase` classes in `..usecase..` must carry `jakarta.transaction.Transactional` and must **not** carry
Spring's `@Transactional`; domain and controller classes must carry neither. Local convention: keep each
rule to one assertion with a `.because()`, and put new architecture rules in this package rather than
starting a second suite.

### Source (pseudonymized)
```java
package com.acme.shop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.acme.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class TransactionalConventionTest {

    @ArchTest
    void givenUseCaseClasses_whenCheckingAnnotations_thenHaveJakartaTransactional(JavaClasses classes) {
        classes()
                .that().haveSimpleNameEndingWith("UseCase")
                .and().resideInAPackage("..usecase..")
                .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
                .because("All use cases must be annotated with @jakarta.transaction.Transactional for transaction demarcation")
                .check(classes);
    }

    @ArchTest
    void givenUseCaseClasses_whenCheckingAnnotations_thenNotUseSpringTransactional(JavaClasses classes) {
        noClasses()
                .that().haveSimpleNameEndingWith("UseCase")
                .and().resideInAPackage("..usecase..")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("Use Jakarta @Transactional (jakarta.transaction.Transactional), not Spring")
                .check(classes);
    }

    @ArchTest
    void givenDomainClasses_whenCheckingAnnotations_thenNoTransactional(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
                .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("@Transactional belongs on the use case layer, not in domain")
                .check(classes);
    }

    @ArchTest
    void givenControllerClasses_whenCheckingAnnotations_thenNoTransactional(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..adapter..controller..")
                .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
                .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("@Transactional belongs on the use case layer, not on controllers")
                .check(classes);
    }
}
```

### Edge cases
Use-case marked with the wrong transaction annotation:
```java
@org.springframework.transaction.annotation.Transactional
class SubmitOrderUseCase { }
```
Expected: the ArchUnit suite fails with the Spring-annotation prohibition.

Domain record marked transactional:
```java
@jakarta.transaction.Transactional
record CustomerAddress(String cityName, String postalCode) { }
```
Expected: the suite fails because transaction demarcation belongs outside the domain layer.

Controller marked transactional:
```java
@jakarta.transaction.Transactional
class CustomerController { }
```
Expected: the suite fails because controllers are not the transaction boundary in this codebase.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'ArchRuleDefinition'` — 2 files / 1 module (`application`)
