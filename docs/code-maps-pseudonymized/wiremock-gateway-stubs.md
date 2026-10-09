---
runtime: lazy
generated-by: pattern-scanner
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map — `wiremock-gateway-stubs` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`FulfilmentFhirGatewayTest.java`](exemplars/fulfilment-fhir-gateway-test.md#wiremock-gateway-stubs) | Shared `WireMockServer` base class, `@DynamicPropertySource` port wiring, Logback `ListAppender` log assertions | gateway test (`fulfilment-gateway`) |

### Excerpts
Dynamic port wiring into the client endpoint (full source in the [exemplar leaf](exemplars/fulfilment-fhir-gateway-test.md#wiremock-gateway-stubs)):
```java
@DynamicPropertySource
static void registerEndpointFulfilment(DynamicPropertyRegistry registry) {
    registry.add("properties.endpoint.fulfilment", () -> "host:port");
    registry.add("properties.fhir.fulfilment.endpoint",
            () -> "http://localhost:%s".formatted(wiremock.port()));
}
```

Pagination stub and verification:
```java
wiremock.stubFor(baseSearchStub(SHIPMENT_SEARCH_ENDPOINT, CUSTOMER_ORDER_RECORD)
        .willReturn(jsonOkResponse(page1Response)));
wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
        .willReturn(jsonOkResponse(page2Response)));

wiremock.verify(1, postRequestedFor(urlEqualTo(SHIPMENT_SEARCH_ENDPOINT)));
wiremock.verify(1, getRequestedFor(urlMatching(".*_getpages=page-1-bundle.*")));
```

### Edge cases
404 from the stubbed upstream:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", CUSTOMER_ORDER_RECORD)
        .willReturn(jsonResponse(404)));
```
Expected: the gateway test asserts an empty list rather than an exception.

Large error payload:
```java
wiremock.stubFor(baseSearchStub("/Restriction/_search", CUSTOMER_ORDER_RECORD)
        .willReturn(jsonResponse(500).withBody("x".repeat(50_000))));
```
Expected: the log assertion proves the interceptor truncates the response body and does not leak the token.

Paginated second page disappears:
```java
wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
        .willReturn(jsonResponse(500)));
```
Expected: the test fails on the broken pagination seam instead of silently returning page one only.

## Local conventions (the project facts the skill omits)
- Package root: `*-gateway/src/test/java/.../gateway/`, with a shared
  `AbstractFulfilmentGatewayTest`-style base per gateway module holding the `WireMockServer` instance.
- Naming shape: `*GatewayTest` extending the shared abstract base.
- Required collaborators / base types: `WireMockServer` started once in `@BeforeAll`; stubs built
  via `WireMock.stubFor(...)`; the server's dynamic port wired into the client endpoint via
  `@DynamicPropertySource`/`DynamicPropertyRegistry`, never hardcoded.
- Config / wiring: none beyond the base class + `@TestConfiguration`.

## Frequency & coverage (why this earned a skill)
- Occurrences: 77 `WireMock` matches across 14 files, 4 modules — `application`,
  `captcha-gateway`, `partner-gateway`, `fulfilment-gateway` (as of `abc1234`).

## Drift / exceptions
- None observed in the sampled exemplar.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'WireMock' --glob '*.java'`
