---
runtime: lazy
generated-by: pattern-scanner
source: partner-gateway/src/main/java/com/acme/shop/partner/gateway/pseudonym/PseudonymGateway.java
serves: [identifier-pseudonymization]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `partner-gateway/.../pseudonym/PseudonymGateway.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The partner NID→pseudonym conversion gateway: `convertNidToPseudo(nid, pseudoAccessToken, scopeIdentifier)`
does EC-curve blinding (`Pseudonymization`, `bouncycastle.math.ec.ECPoint`), posts the blinded point to
the pseudonymization REST endpoint, and unblinds the response (`PseudoInfo` → `PseudoSubject`) to get
the scope-scoped pseudonym. Sibling classes in the same package: `Pseudonymization` (the EC math +
`CURVE_SPEC`), `PseudonymizationScalar`, `PseudoInfo`, and `PseudoSubject`.

## Partner pseudonymization gateway {#identifier-pseudonymization}
**Serves:** [`identifier-pseudonymization`](../identifier-pseudonymization.md)

Local convention: the per-scope `bufferSize` (buffer size of the scope's pseudonymization keyset) is
looked up once and cached via `CacheManager.getCache("pseudoScope")` — a plain get-or-fetch-then-put, no
`@Cacheable` annotation, because the fetch also needs the `pseudoAccessToken`. All three inputs
(`nid`, `pseudoAccessToken`, `scopeIdentifier`) are blank-checked up front (`PartnerException` on
failure) before any network call — validate before you spend a round-trip.

### Source (pseudonymized)
```java
package com.acme.shop.partner.gateway.pseudonym;

import com.acme.shop.partner.exception.PartnerException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.util.UUID;

import static com.acme.shop.partner.gateway.pseudonym.Pseudonymization.CURVE_SPEC;
import static java.util.Base64.getEncoder;
import static java.util.Objects.isNull;


@Repository
public class PseudonymGateway {

    private final RestClient restClient;
    private final String pseudoEndpointUrl;
    private final CacheManager cacheManager;


    public PseudonymGateway(
            @Value("${properties.endpoint.partner.pseudo}") final String pseudoEndpointUrl,
            CacheManager cacheManager,
            @Qualifier("proxyAwareRestClient") final RestClient restClient) {
        this.pseudoEndpointUrl = pseudoEndpointUrl;
        this.cacheManager = cacheManager;
        this.restClient = restClient;
    }

    public String convertNidToPseudo(String nid, String pseudoAccessToken, String scopeIdentifier) {
        validateParameters(nid, pseudoAccessToken, scopeIdentifier);

        var bufferSize = getScopeBufferSizeForIdentifier(scopeIdentifier, pseudoAccessToken);

        var pseudonymization = new Pseudonymization(nid, bufferSize);

        var pseudoInfo = pseudonymizeForScope(scopeIdentifier, pseudoAccessToken, pseudonymization.getBlindedCurvePoint());

        return pseudonymization.getPseudoSubject(pseudoInfo);
    }

    private void validateParameters(String nid, String pseudoAccessToken, String scopeIdentifier) {
        if (StringUtils.isBlank(nid)) {
            throw new PartnerException("nid is empty");
        }

        if (StringUtils.isBlank(pseudoAccessToken)) {
            throw new PartnerException("pseudoAccessToken is empty");
        }

        if (StringUtils.isBlank(scopeIdentifier)) {
            throw new PartnerException("scopeIdentifier is empty");
        }
    }

    private PseudoInfo pseudonymizeForScope(String scopeIdentifier, String pseudoAccessToken, ECPoint requestPoint) {
        var pseudonymizeUrl = "%s/scopes/%s/pseudonymize".formatted(pseudoEndpointUrl, scopeIdentifier);

        final BigInteger xPoint = requestPoint.getXCoord().toBigInteger();
        final BigInteger yPoint = requestPoint.getYCoord().toBigInteger();
        var requestBodyTemplate = """
                    {
                      "id": "%s",
                      "crv": "%s",
                      "x": "%s",
                      "y": "%s"
                    }
                """;
        var requestBody = requestBodyTemplate.formatted(
                UUID.randomUUID(),
                CURVE_SPEC,
                getEncoder().encodeToString(xPoint.toByteArray()),
                getEncoder().encodeToString(yPoint.toByteArray()));

        return restClient.post()
                .uri(pseudonymizeUrl)
                .header("Authorization", "Bearer " + pseudoAccessToken)
                .body(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PseudoInfo.class);
    }

    private Integer getScopeBufferSizeForIdentifier(String scopeIdentifier, String pseudoAccessToken) {
        var cache = cacheManager.getCache("pseudoScope");
        if (cache != null) {
            var cachedValue = cache.get(scopeIdentifier, Integer.class);
            if (cachedValue != null) {
                return cachedValue;
            }
        }

        var scopeDetailsUrl = "%s/scopes/%s".formatted(pseudoEndpointUrl, scopeIdentifier);
        var scopeDetails = restClient.get()
                .uri(scopeDetailsUrl)
                .header("Authorization", "Bearer " + pseudoAccessToken)
                .retrieve()
                .body(ScopeDetails.class);

        if (isNull(scopeDetails)) {
            throw new PartnerException("The response from the scope-details endpoint was null");
        }

        if (cache != null) {
            cache.put(scopeIdentifier, scopeDetails.bufferSize);
        }

        return scopeDetails.bufferSize;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScopeDetails(Integer bufferSize) {
    }

}
```

### Edge cases
Blank identifier:
```java
assertThatThrownBy(() -> gateway.convertNidToPseudo("", "pseudo-token", "orders"))
        .isInstanceOf(PartnerException.class)
        .hasMessage("nid is empty");
```
Expected: no outbound call is attempted.

Scope details endpoint returns `null`:
```java
when(restClient.get().retrieve().body(ScopeDetails.class)).thenReturn(null);
```
Expected: the gateway throws a validation exception instead of caching an unavailable buffer size.

Same scope reused:
```java
gateway.convertNidToPseudo("00000000000", "pseudo-token", "orders");
gateway.convertNidToPseudo("11111111111", "pseudo-token", "orders");
```
Expected: the second call reuses the cached scope buffer size but still computes a different pseudonym for the new identifier.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg -li 'pseudo' --glob '*.java'` (161 matches / 29 files incl. tests; 16 files / 8 modules main-only)
