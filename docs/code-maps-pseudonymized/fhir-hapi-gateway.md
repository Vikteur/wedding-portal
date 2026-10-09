---
runtime: lazy
generated-by: pattern-scanner
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map — `fhir-hapi-gateway` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`FulfilmentFhirGateway.java`](exemplars/fulfilment-fhir-gateway.md#fhir-hapi-gateway) | `IGenericClient` search/pagination, bearer auth, 404-as-empty-list error mapping | gateway (`fulfilment-gateway`) |

### Excerpts
Authenticated client creation (full source in the [exemplar leaf](exemplars/fulfilment-fhir-gateway.md#fhir-hapi-gateway)):
```java
private IGenericClient createAuthenticatedFhirClient(String pseudoAccessToken) {
    final IGenericClient fhirClient = fulfilmentFhirContext.newRestfulGenericClient(fulfilmentFhirEndpoint);
    fhirClient.registerInterceptor(new BearerTokenAuthInterceptor(pseudoAccessToken));
    fhirClient.registerInterceptor(new FhirClientErrorLoggingInterceptor());
    return fhirClient;
}
```

Manual pagination over shipment resources:
```java
Bundle currentBundle = bundle;
while (currentBundle.getLink(Bundle.LINK_NEXT) != null) {
    currentBundle = fhirClient.loadPage().next(currentBundle).execute();
    allShipments.addAll(BundleUtil.toListOfResourcesOfType(
            fhirClient.getFhirContext(),
            currentBundle,
            Shipment.class
    ));
}
```

### Edge cases
Upstream returns 404 for an empty result:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", "CUSTOMER_ORDER_RECORD")
        .willReturn(jsonResponse(404)));

assertThat(gateway.getShipments("pseudo-token", "pseudo-nid", "storefront")).isEmpty();
```
Expected: HTTP 404 becomes an empty list, not an exception.

Upstream returns multiple pages:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", "CUSTOMER_ORDER_RECORD")
        .willReturn(jsonOkResponse(page1)));
wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
        .willReturn(jsonOkResponse(page2)));
```
Expected: the gateway follows `Bundle.LINK_NEXT` until exhaustion and returns resources from every page.

Upstream returns a non-404 error:
```java
wiremock.stubFor(baseSearchStub("/FulfilmentPlan/_search", "PUBLIC_CATALOG")
        .willReturn(jsonResponse(500)));

assertThatThrownBy(() -> gateway.getFulfilmentPlans("pseudo-token", "pseudo-nid", "storefront"))
        .isInstanceOf(FulfilmentException.class);
```
Expected: non-404 transport failures are wrapped and rethrown.

## Local conventions (the project facts the skill omits)
- Package root: `fulfilment-gateway/src/main/java/.../gateway/` (and
  `product-restriction-adapter`, `order-adapter` for the consumer side).
- Naming shape: `*FhirGateway`, one `FhirContext` bean built once, a new `IGenericClient` per call
  via `newRestfulGenericClient`.
- Required collaborators / base types: `BearerTokenAuthInterceptor` for auth,
  a custom `*ErrorLoggingInterceptor` registered per client; manual bundle-link pagination
  (`bundle.getLink(Bundle.LINK_NEXT)` + `fhirClient.loadPage().next(...)`).
- Config / wiring: FHIR endpoint sourced via `@Value("${properties.fhir.<gateway>.endpoint}")`.

## Frequency & coverage (why this earned a skill)
- Occurrences: 33 matches (`IGenericClient|FhirContext`) across 5 files, 3 modules —
  `product-restriction-adapter`, `order-adapter`, `fulfilment-gateway` (as of `abc1234`).

## Drift / exceptions
- HTTP 404 is treated as "no results" (returns empty), any other
  `BaseServerResponseException` status is wrapped and rethrown — don't let 404 bubble as an error.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'IGenericClient|FhirContext' --glob '*.java'`
