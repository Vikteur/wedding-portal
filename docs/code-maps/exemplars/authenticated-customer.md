---
runtime: lazy
generated-by: pattern-scanner
source: authentication-domain/src/main/java/com/acme/shop/authentication/domain/AuthenticatedCustomer.java
serves: [auth-context-facade]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `authentication-domain/.../AuthenticatedCustomer.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The domain-level principal type resolved once per request and threaded through every use-case that
needs identity.

## The resolved-principal value {#auth-context-facade}
**Serves:** [`auth-context-facade`](../auth-context-facade.md)

A plain interface, not a class — `getNid()`, `getActiveNid()`, `getActiveDelegationNid()` (returns
`Optional<String>`, the only optional-typed accessor: delegation is the one genuinely absent case,
everything else on an authenticated request is guaranteed present), `getName()`, `getFirstName()`,
`getAuthenticationToken()`. Use-cases and controllers depend on this interface only, never on the
concrete authenticated-user/JWT type — see [authentication-facade] for how it is resolved at the boundary.

### Source (pseudonymized)
```java
package com.acme.shop.authentication.domain;

import java.util.Optional;

public interface AuthenticatedCustomer {
    String getAuthenticationToken();
    String getActiveNid();
    String getNid();
    String getName();
    String getFirstName();
    Optional<String> getActiveDelegationNid();
    boolean isAuthenticatedThroughShopPortal();
}
```

### Edge cases
Direct customer session with no delegation:
```java
assertThat(authenticatedCustomer.getActiveDelegationNid()).isEmpty();
assertThat(authenticatedCustomer.getActiveNid()).isEqualTo("NID-00000");
```
Expected: the principal still has a complete direct identity even when there is no delegated account in play.

Delegated session:
```java
assertThat(authenticatedCustomer.getActiveNid()).isEqualTo("NID-4380960");
assertThat(authenticatedCustomer.getActiveDelegationNid()).contains("NID-00000");
```
Expected: the interface can represent both the active identity and the delegated identity without exposing framework classes.

Portal-authenticated flag:
```java
assertThat(authenticatedCustomer.isAuthenticatedThroughShopPortal()).isTrue();
```
Expected: downstream logic can branch on the authentication channel without inspecting transport-layer details.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'AuthenticatedCustomer' --glob '*.java'` (103 files / 23 modules)

[authentication-facade]: authentication-facade.md
