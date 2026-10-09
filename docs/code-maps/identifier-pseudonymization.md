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
# Code map — `identifier-pseudonymization` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`PseudonymGateway.java`](exemplars/pseudonym-gateway.md#identifier-pseudonymization) | NID→pseudonym conversion — EC-curve blinding, per-scope buffer-size caching, blank-input guards | adapter (`partner-gateway`) |
| [`TokenExchangeGateway.java`](exemplars/token-exchange-gateway.md#identifier-pseudonymization) | OAuth2 token exchange feeding the pseudonymization call with a client-scoped token | adapter (`partner-gateway`) |

### Excerpts
Blank-guard + get-or-fetch cache (full source in the [gateway exemplar](exemplars/pseudonym-gateway.md#identifier-pseudonymization)):
```java
public String convertNidToPseudo(String nid, String pseudoAccessToken, String scopeIdentifier) {
    validateParameters(nid, pseudoAccessToken, scopeIdentifier);
    var bufferSize = getScopeBufferSizeForIdentifier(scopeIdentifier, pseudoAccessToken);
    var pseudonymization = new Pseudonymization(nid, bufferSize);
    var pseudoInfo = pseudonymizeForScope(scopeIdentifier, pseudoAccessToken, pseudonymization.getBlindedCurvePoint());
    return pseudonymization.getPseudoSubject(pseudoInfo);
}
```

Token exchange before pseudonymization:
```java
data.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
data.add("subject_token", token);
data.add("audience", getClientIdForAudience(client));
data.add("client_id", extractClientIdFromToken(token));
```

### Edge cases
Blank customer identifier:
```java
assertThatThrownBy(() -> gateway.convertNidToPseudo("", "pseudo-token", "orders"))
        .isInstanceOf(PartnerException.class)
        .hasMessage("nid is empty");
```
Expected: the gateway rejects the call before any network round-trip.

Token exchange returns no `access_token`:
```java
when(restClient.post().retrieve().body(TOKEN_RESPONSE_TYPE)).thenReturn(Map.of("token_type", "Bearer"));
```
Expected: the token exchange step fails loudly with a validation exception.

Repeated pseudonymization for the same scope:
```java
gateway.convertNidToPseudo("00000000000", "pseudo-token", "orders");
gateway.convertNidToPseudo("00000000000", "pseudo-token", "orders");
```
Expected: the per-scope buffer size is fetched once, then served from cache on later calls.

## Local conventions (the project facts the skill omits)
- Package root: `partner-gateway/src/main/java/com/acme/shop/partner/gateway/pseudonym/` (the
  gateway + EC math + request/response DTOs: `Pseudonymization`, `PseudonymizationScalar`,
  `PseudoInfo`, `PseudoSubject`) and `.../gateway/token/` (the OAuth2 token exchange step).
- Naming shape: `*Gateway` in the adapter, a `*Repository` port in the use-case layer
  (`authentication-usecase/.../repository/PseudonymRepository.java`), implemented by
  `authentication-adapter/.../DefaultPseudonymRepository.java`.
- Required collaborators / base types: `authentication-usecase/.../ExchangeTokenUseCase.java` is the
  orchestration point — exchange the token first, then (only for the `FULFILMENT_FHIR` client) convert the
  NID to a pseudonym using that token.
- Downstream consumers never see the raw NID: `FulfilmentFhirGateway`, `OrderFhirGateway`, and
  `ProductRestrictionFhirGateway` accept only `pseudoNid` / `pseudoAccessToken` parameters.
- Config / wiring: per-scope buffer size cached via a plain `CacheManager` get-or-fetch (not
  `@Cacheable`, because the fetch needs the request-scoped access token too) — see
  [spring-caching].

## Frequency & coverage (why this earned a skill)
- Occurrences: `rg -li 'pseudo' --glob '*.java'` — 161 matches / 29 files (including tests); 16 files /
  8 modules main-only (as of `abc1234`): `product-restriction-adapter`, `application`,
  `authentication-adapter`, `authentication-usecase`, `partner-gateway`, `campaign-adapter`,
  `order-adapter`, `fulfilment-gateway`.
- Hotspot: `partner-gateway/.../pseudonym/` and `.../token/` (7 of the 16 main files).

## Drift / exceptions
- **Bookkeeping correction:** the first harvest pass recorded no row for this pattern at all — neither a
  code map nor a below-threshold candidate — even though it clears the frequency threshold (16 files / 8
  modules main-only). This map is the correction; no drift observed in the pattern itself.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg -li 'pseudo' --glob '*.java'` (161 matches / 29 files incl. tests, 16 files / 8 modules main-only)

[spring-caching]: spring-caching.md
