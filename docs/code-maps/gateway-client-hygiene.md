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
# Code map — `gateway-client-hygiene` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`DefaultRegistryGateway.java`](exemplars/default-registry-gateway.md#gateway-client-hygiene) | Same-package `*Gateway` port + package-private adapter | adapter/gateway (`partner-gateway`) |
| [`FulfilmentFhirGateway.java`](exemplars/fulfilment-fhir-gateway.md#gateway-client-hygiene) | Direct-adapter shape (no port) for a single-implementation gateway | adapter/gateway (`fulfilment-gateway`) |

### Excerpts
Package-private port implementation (full source in the [exemplar leaf](exemplars/default-registry-gateway.md#gateway-client-hygiene)):
```java
class DefaultRegistryGateway implements RegistryGateway {
    @Override
    @Cacheable("registry")
    @CircuitBreaker(name = "registry", fallbackMethod = "getRegistryLinksFallback")
    public List<RegistryLink> getRegistryLinks(final String nid) {
        final GetCustomerLinksResponse customerLinks =
                registryPortType.getCustomerLinks(buildCustomerLinksRequest(nid));
        return registryMapper.mapRegistryLinks(customerLinks.getRegistryList().getEntries());
    }
}
```

Direct gateway with no interface:
```java
@Repository
@Slf4j
public class FulfilmentFhirGateway {
    public List<Shipment> getShipments(String pseudoAccessToken, String pseudoNid, String storefrontId) {
        IGenericClient fhirClient = createAuthenticatedFhirClient(pseudoAccessToken);
        return searchShipments(pseudoNid, fhirClient, storefrontId);
    }
}
```

### Edge cases
SOAP acknowledgment contains an upstream error:
```java
when(registryPortType.getCustomerLinks(any())).thenReturn(responseWithError("duplicate customer reference"));

assertThatThrownBy(() -> gateway.getRegistryLinks("00000000000"))
        .isInstanceOf(RegistryException.class);
```
Expected: the adapter translates the wire-level failure into a domain exception and never returns a partial result.

Single implementation, no interface:
```java
var gateway = new FulfilmentFhirGateway(fhirContext, "https://api.example.invalid/fhir");
var result = gateway.getShipments("pseudo-token", "pseudo-nid", "storefront");
```
Expected: direct use of the concrete gateway is acceptable when the module has exactly one adapter and no swap seam is needed.

Generated client returns a wire model with extra fields:
```java
GetCustomerLinksResponse response = responseWithUnexpectedMetadata();
var links = gateway.getRegistryLinks("00000000000");
```
Expected: only mapped domain values cross the boundary; extra wire metadata is ignored inside the gateway.

## Local conventions (the project facts the skill omits)
- Package root: `<capability>-adapter/.../gateway/` (in-module gateways, for example `partner-gateway`) or
  a dedicated `*-gateway` module (`fulfilment-gateway`, `payments-gateway`, `loyalty-gateway`,
  `captcha-gateway`, `hubservice-gateway-api`) for shared/external integrations.
- Naming shape: `*Gateway` for both the port interface and (usually `Default*Gateway`) its
  implementation when a port exists; plain `*Gateway` with no interface when the module has a
  single implementation (see Drift below).
- Required collaborators / base types: constructor-injected generated client (SOAP `*PortType`,
  HAPI `IGenericClient`, or a plain REST client), domain-shaped return types — never leak the wire
  type past the gateway.
- Config / wiring: `@Repository` stereotype (not `@Component`) is the convention for gateway
  adapters in this codebase.

## Frequency & coverage (why this earned a skill)
- Occurrences: 25 files named `class *Gateway` across 13 modules, plus 8 `interface *Gateway` port
  definitions across 2 modules (`partner-gateway`, `hubservice-gateway-api`) (as of `abc1234`).

## Drift / exceptions
- **Not every gateway has a port interface.** `partner-gateway`'s registry/consent/etc. integrations
  define an explicit `*Gateway` interface + `Default*Gateway` impl; `fulfilment-gateway`'s FHIR/REST
  gateways are called directly by use cases with no interface. Both are intentional — add a port
  when the module needs to swap/mock at that seam beyond what a Mockito `@Mock` on the concrete
  class already gives you.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'class[[:space:]]+[A-Za-z]*Gateway\b'` (25 files / 13 modules), `rg 'interface[[:space:]]+[A-Za-z]*Gateway\b'` (8 files / 2 modules)
