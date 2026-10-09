---
runtime: lazy
generated-by: pattern-scanner
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `auth-context-facade` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`AuthenticatedCustomer.java`](exemplars/authenticated-customer.md#auth-context-facade) | The resolved-principal value type, threaded through use-cases | domain (`authentication-domain`) |
| [`AuthenticationFacade.java`](exemplars/authentication-facade.md#auth-context-facade) | The port that resolves it once at the boundary | usecase (`authentication-usecase`) |

### Excerpts
Resolved-principal contract (full source in the [value-type exemplar](exemplars/authenticated-customer.md#auth-context-facade)):
```java
public interface AuthenticatedCustomer {
    String getAuthenticationToken();
    String getActiveNid();
    String getNid();
    Optional<String> getActiveDelegationNid();
}
```

Boundary port used everywhere else:
```java
public interface AuthenticationFacade {
    AuthenticatedCustomer getAuthentication();
    Optional<AuthenticatedCustomer> getAuthenticatedCustomer();
}
```

### Edge cases
Anonymous request on an optional-authentication path:
```java
assertThat(authenticationFacade.getAuthenticatedCustomer()).isEmpty();
```
Expected: boundary code can detect the absent caller without importing Spring Security types downstream.

Authenticated caller with no active delegation:
```java
assertThat(authenticatedCustomer.getActiveDelegationNid()).isEmpty();
```
Expected: the delegation identifier is the only optional accessor; all other identity fields remain present.

Delegated request using the same domain type as a direct request:
```java
assertThat(authenticatedCustomer.getActiveNid()).isEqualTo("NID-00000");
assertThat(authenticatedCustomer.getActiveDelegationNid()).contains("NID-4380960");
```
Expected: use-cases receive one principal abstraction and decide whether they care about delegation.

## Local conventions (the project facts the skill omits)
- Package root: value type in `authentication-domain/.../domain/`; resolving port in
  `authentication-usecase/.../repository/`; implementation
  (`SpringAuthenticationContext`, reading `SecurityContextHolder`) in `authentication-adapter`.
- Naming shape: `AuthenticatedCustomer` (value), `AuthenticationFacade` (port,
  `getAuthentication()` / `getAuthenticatedCustomer(): Optional<...>`).
- Required collaborators / base types: use-cases and controllers depend only on
  `AuthenticationFacade` + `AuthenticatedCustomer`, never on Spring Security types or a JWT class
  directly.
- Config / wiring: resolved once per request by the adapter implementation; everything downstream
  just calls the facade.

## Frequency & coverage (why this earned a skill)
- Occurrences: `AuthenticatedCustomer` — 103 files / 23 modules; `AuthenticationFacade` — 75 files / 17
  modules (as of `abc1234`) — among the most widely referenced domain types in the repo.

## Drift / exceptions
- `getActiveDelegationNid()` is the one `Optional`-typed accessor on `AuthenticatedCustomer` —
  delegation is the one genuinely absent case on an authenticated request; everything else is
  guaranteed present.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'AuthenticatedCustomer' --glob '*.java'`, `rg 'AuthenticationFacade' --glob '*.java'`
