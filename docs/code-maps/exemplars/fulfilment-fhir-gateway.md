---
runtime: lazy
source: fulfilment-gateway/src/main/java/com/acme/shop/fulfilment/gateway/FulfilmentFhirGateway.java
serves: [gateway-client-hygiene, fhir-hapi-gateway, resilience4j]
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `fulfilment-gateway/.../FulfilmentFhirGateway.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The richest FHIR/HAPI client in the repo: queries Fulfilment for `FulfilmentPlan`, `Shipment`, and
`Restriction` resources. `@Repository`-annotated adapter, no port interface (called directly by use
cases in this module) — see the Drift note in the skill index.

## HAPI `IGenericClient` usage {#fhir-hapi-gateway}
**Serves:** [`fhir-hapi-gateway`](../fhir-hapi-gateway.md)

`FhirContext` is injected as a bean (built once, config elsewhere), `newRestfulGenericClient(endpoint)`
per call in `createAuthenticatedFhirClient`, auth via `BearerTokenAuthInterceptor` plus a custom
`FhirClientErrorLoggingInterceptor` registered per client instance. Searches are built fluently,
manual pagination follows `Bundle.LINK_NEXT`, and HTTP 404 from `BaseServerResponseException` is
mapped to an empty list rather than an error.

## Gateway resilience {#resilience4j}
**Serves:** [`resilience4j`](../resilience4j.md)

Each public read method carries its own `@CircuitBreaker(name = "fulfilment-fhir", fallbackMethod = "...")`
— one shared breaker name across methods on the same gateway (they share the same upstream), but a
dedicated fallback per method that rethrows `FulfilmentException` because this integration is not
tolerant of silent failure.

## Gateway shape (no port, direct adapter) {#gateway-client-hygiene}
**Serves:** [`gateway-client-hygiene`](../gateway-client-hygiene.md)

Unlike [`DefaultRegistryGateway`](default-registry-gateway.md#gateway-client-hygiene) (which implements a
same-package `*Gateway` port), this class is called directly by its use case — a valid, simpler
shape for a module with a single gateway implementation and no need to swap adapters. Domain-shaped
return types (`FulfilmentPlanWithTasks`, not the raw FHIR bundle) are still enforced at this boundary.

### Source (pseudonymized)
> Original file is >300 lines; the source below keeps only the pattern-relevant members: authenticated
> client creation, representative read paths, pagination, and fallbacks.

```java
package com.acme.shop.fulfilment.gateway;

import com.acme.shop.fulfilment.dto.plan.FulfilmentPlanType;
import com.acme.shop.fulfilment.dto.plan.FulfilmentPlanWithTasks;
import com.acme.shop.fulfilment.exception.FulfilmentException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.util.BundleUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;


@Repository
@Slf4j
public class FulfilmentFhirGateway {

    private static final String NID_SYSTEM = "https://example.invalid/fhir/core/NamingSystem/nid";
    private static final String RESTRICTION_ID_SYSTEM = "https://example.invalid/fhir/restriction/NamingSystem/restriction-id";
    private static final String FULFILMENT_SECURITY_SYSTEM = "https://security.example.invalid";
    private static final String CUSTOMER_ORDER_RECORD = "CUSTOMER_ORDER_RECORD";
    private static final String PSEUDO_SUBJECT_PREFIX = "urn:com:acme:shop:pseudo:v1:";

    private final FhirContext fulfilmentFhirContext;
    private final String fulfilmentFhirEndpoint;

    public FulfilmentFhirGateway(FhirContext fulfilmentFhirContext,
                                 @Value("${properties.fhir.fulfilment.endpoint}") String fulfilmentFhirEndpoint) {
        this.fulfilmentFhirContext = fulfilmentFhirContext;
        this.fulfilmentFhirEndpoint = fulfilmentFhirEndpoint;
    }

    @CircuitBreaker(name = "fulfilment-fhir", fallbackMethod = "getFulfilmentPlansFallback")
    public List<FulfilmentPlanWithTasks> getFulfilmentPlans(String pseudoAccessToken, String pseudoNid, String storefrontId) {
        IGenericClient fhirClient = createAuthenticatedFhirClient(pseudoAccessToken);
        List<FulfilmentPlan> fulfilmentPlans = searchFulfilmentPlans(pseudoNid, fhirClient, storefrontId);
        if (fulfilmentPlans.isEmpty()) {
            return List.of();
        }
        return fulfilmentPlans.stream()
                .map(plan -> new FulfilmentPlanWithTasks(plan, List.of()))
                .toList();
    }

    private List<FulfilmentPlanWithTasks> getFulfilmentPlansFallback(String pseudoAccessToken, String pseudoNid, String storefrontId, final Throwable e) {
        log.error("Circuit breaker: Failed to fetch catalogue plans from Fulfilment", e);
        throw new FulfilmentException("Unable to get catalogue plans from Fulfilment", e);
    }

    private IGenericClient createAuthenticatedFhirClient(String pseudoAccessToken) {
        final IGenericClient fhirClient = fulfilmentFhirContext.newRestfulGenericClient(fulfilmentFhirEndpoint);
        fhirClient.registerInterceptor(new BearerTokenAuthInterceptor(pseudoAccessToken));
        fhirClient.registerInterceptor(new FhirClientErrorLoggingInterceptor());
        return fhirClient;
    }

    private List<FulfilmentPlan> searchFulfilmentPlans(String pseudoNid, IGenericClient fhirClient, String storefrontId) {
        var subject = PSEUDO_SUBJECT_PREFIX + pseudoNid;

        try {
            return Arrays.stream(FulfilmentPlanType.values())
                    .map(FulfilmentPlanType::getCode)
                    .map(code -> fhirClient.search().forResource(FulfilmentPlan.class)
                            .where(FulfilmentPlan.CUSTOMER.hasChainedProperty(Customer.IDENTIFIER.exactly().systemAndIdentifier(NID_SYSTEM, subject)))
                            .withSecurity(FULFILMENT_SECURITY_SYSTEM, "PUBLIC_CATALOG")
                            .and(FulfilmentPlan.CATEGORY.exactly().systemAndCode("https://codes.example.invalid/catalog", code))
                            .usingStyle(SearchStyleEnum.POST)
                            .withAdditionalHeader("X-Storefront", storefrontId)
                            .returnBundle(Bundle.class)
                            .execute())
                    .map(bundle -> BundleUtil.toListOfResources(fhirClient.getFhirContext(), bundle))
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .filter(FulfilmentPlan.class::isInstance)
                    .map(FulfilmentPlan.class::cast)
                    .toList();
        } catch (BaseServerResponseException e) {
            return switch (e.getStatusCode()) {
                case 404 -> {
                    log.error("HTTP error {} while searching fulfilment plans: {}", e.getStatusCode(), e.getMessage());
                    yield emptyList();
                }
                default -> {
                    log.error("Unexpected error while searching fulfilment plans", e);
                    throw new FulfilmentException("Unexpected error while searching fulfilment plans", e);
                }
            };
        }
    }

    @CircuitBreaker(name = "fulfilment-fhir", fallbackMethod = "getShipmentsFallback")
    public List<Shipment> getShipments(String pseudoAccessToken, String pseudoNid, String storefrontId) {
        IGenericClient fhirClient = createAuthenticatedFhirClient(pseudoAccessToken);
        return searchShipments(pseudoNid, fhirClient, storefrontId);
    }

    private List<Shipment> getShipmentsFallback(String pseudoAccessToken, String pseudoNid, String storefrontId, final Throwable e) {
        log.error("Circuit breaker: Failed to fetch shipments from Fulfilment", e);
        throw new FulfilmentException("Unable to get shipments from Fulfilment", e);
    }

    private List<Shipment> searchShipments(String pseudoNid, IGenericClient fhirClient, String storefrontId) {
        var subject = PSEUDO_SUBJECT_PREFIX + pseudoNid;

        try {
            final Bundle bundle = fhirClient.search().forResource(Shipment.class)
                    .where(Shipment.CUSTOMER.hasChainedProperty(Customer.IDENTIFIER.exactly().systemAndIdentifier(NID_SYSTEM, subject)))
                    .withSecurity(FULFILMENT_SECURITY_SYSTEM, CUSTOMER_ORDER_RECORD)
                    .usingStyle(SearchStyleEnum.POST)
                    .withAdditionalHeader("X-Storefront", storefrontId)
                    .returnBundle(Bundle.class)
                    .execute();

            List<Shipment> allShipments = BundleUtil.toListOfResourcesOfType(
                    fhirClient.getFhirContext(),
                    bundle,
                    Shipment.class
            );

            Bundle currentBundle = bundle;
            while (currentBundle.getLink(Bundle.LINK_NEXT) != null) {
                currentBundle = fhirClient.loadPage()
                        .next(currentBundle)
                        .execute();
                allShipments.addAll(BundleUtil.toListOfResourcesOfType(
                        fhirClient.getFhirContext(),
                        currentBundle,
                        Shipment.class
                ));
            }

            return allShipments.stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (BaseServerResponseException e) {
            return switch (e.getStatusCode()) {
                case 404 -> {
                    log.error("HTTP error {} while searching shipments: {}", e.getStatusCode(), e.getMessage());
                    yield emptyList();
                }
                default -> {
                    log.error("Unexpected error while searching shipments", e);
                    throw new FulfilmentException("Unexpected error while searching shipments", e);
                }
            };
        }
    }

    @CircuitBreaker(name = "fulfilment-fhir", fallbackMethod = "getRestrictionsFallback")
    public List<Restriction> getRestrictions(final String pseudoAccessToken, final String pseudoNid, final String storefrontId) {
        final IGenericClient fhirClient = createAuthenticatedFhirClient(pseudoAccessToken);
        return searchRestrictions(pseudoNid, fhirClient, storefrontId);
    }

    private List<Restriction> getRestrictionsFallback(String pseudoAccessToken, String pseudoNid, String storefrontId, final Throwable e) {
        log.error("Circuit breaker: Failed to fetch restrictions from Fulfilment", e);
        throw new FulfilmentException("Unable to get restrictions from Fulfilment", e);
    }

    private static ICriterion<?> customerIsPseudonymizedNid(final String pseudoNid) {
        return Restriction.CUSTOMER.hasChainedProperty(
                Customer.IDENTIFIER.exactly().systemAndIdentifier(NID_SYSTEM, PSEUDO_SUBJECT_PREFIX + pseudoNid));
    }

    private List<Restriction> searchRestrictions(final String pseudoNid,
                                                 final IGenericClient fhirClient,
                                                 final String storefrontId) {
        try {
            final Bundle bundle = fhirClient.search()
                    .forResource(Restriction.class)
                    .where(customerIsPseudonymizedNid(pseudoNid))
                    .withSecurity(FULFILMENT_SECURITY_SYSTEM, CUSTOMER_ORDER_RECORD)
                    .usingStyle(SearchStyleEnum.POST)
                    .withAdditionalHeader("X-Storefront", storefrontId)
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Restriction> restrictions = BundleUtil.toListOfResourcesOfType(
                    fhirClient.getFhirContext(),
                    bundle,
                    Restriction.class
            );

            Bundle currentBundle = bundle;
            while (currentBundle.getLink(Bundle.LINK_NEXT) != null) {
                currentBundle = fhirClient.loadPage()
                        .next(currentBundle)
                        .execute();
                restrictions.addAll(BundleUtil.toListOfResourcesOfType(
                        fhirClient.getFhirContext(),
                        currentBundle,
                        Restriction.class
                ));
            }

            return restrictions.stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (BaseServerResponseException e) {
            if (e.getStatusCode() == 404) {
                log.error("HTTP error {} while searching restrictions: {}", e.getStatusCode(), e.getMessage());
                return emptyList();
            }
            log.error("Unexpected error while searching restrictions", e);
            throw new FulfilmentException("Unexpected error while searching restrictions", e);
        }
    }
}
```

### Edge cases
404 while searching shipments:
```java
wiremock.stubFor(baseSearchStub("/Shipment/_search", "CUSTOMER_ORDER_RECORD")
        .willReturn(jsonResponse(404)));

assertThat(gateway.getShipments("pseudo-token", "pseudo-nid", "storefront")).isEmpty();
```
Expected: the gateway returns an empty list instead of surfacing a not-found error.

Two-page shipment bundle:
```java
wiremock.stubFor(get(urlMatching(".*_getpages=page-1-bundle.*"))
        .willReturn(jsonOkResponse(page2)));
```
Expected: the gateway follows the next link and aggregates both pages.

Transport failure on a critical read:
```java
wiremock.stubFor(baseSearchStub("/Restriction/_search", "CUSTOMER_ORDER_RECORD")
        .willReturn(jsonResponse(503)));

assertThatThrownBy(() -> gateway.getRestrictions("pseudo-token", "pseudo-nid", "storefront"))
        .isInstanceOf(FulfilmentException.class);
```
Expected: the fallback rethrows because the caller cannot safely continue without this data.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@CircuitBreaker'` (9 matches / 5 files / 2 modules), `rg 'IGenericClient|FhirContext'` (33 matches / 5 files / 3 modules)
