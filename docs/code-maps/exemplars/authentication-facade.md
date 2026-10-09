---
runtime: lazy
source: authentication-usecase/src/main/java/com/acme/shop/authentication/repository/AuthenticationFacade.java
serves: [auth-context-facade]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `authentication-usecase/.../AuthenticationFacade.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The port every use-case calls to resolve the current principal — see
[authenticated-customer] for the value it returns. Implemented once, in
`authentication-adapter` (`SpringAuthenticationContext`), reading off Spring Security's
`SecurityContextHolder`.

## Auth-context facade port {#auth-context-facade}
**Serves:** [`auth-context-facade`](../auth-context-facade.md)

Two methods: `getAuthentication()` — the common case, use-cases that require an authenticated
caller (throws or is only meaningfully called on a secured route); `getAuthenticatedCustomer()` —
`Optional<AuthenticatedCustomer>` for the rarer path that must also tolerate an anonymous or unset
caller. Dozens of files call this facade rather than touching Spring Security types directly —
resolve once at the boundary, use everywhere else (for example
[get-invoices-usecase]).

### Source (pseudonymized)
```java
package com.acme.shop.authentication.repository;

import com.acme.shop.authentication.domain.AuthenticatedCustomer;

import java.util.Optional;

public interface AuthenticationFacade {
    AuthenticatedCustomer getAuthentication();
    Optional<AuthenticatedCustomer> getAuthenticatedCustomer();
}
```

### Edge cases
Anonymous request on a tolerant path:
```java
assertThat(authenticationFacade.getAuthenticatedCustomer()).isEmpty();
```
Expected: the optional-returning method lets boundary code handle anonymous access without leaking Spring types.

Authenticated request on a required-authentication path:
```java
var customer = authenticationFacade.getAuthentication();
assertThat(customer.getName()).isEqualTo("Jane Doe");
```
Expected: secured flows can depend on the non-optional method and receive a fully populated principal.

Delegated account access:
```java
assertThat(authenticationFacade.getAuthentication().getActiveDelegationNid())
        .contains("NID-4380960");
```
Expected: the same port supports both direct and delegated sessions without changing the downstream API.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'AuthenticationFacade' --glob '*.java'` (75 files / 17 modules)

[authenticated-customer]: authenticated-customer.md
[get-invoices-usecase]: get-invoices-usecase.md
