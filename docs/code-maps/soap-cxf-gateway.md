---
runtime: lazy
generated-by: pattern-scanner
---
<!-- AI_DISCLAIMER v1.0 -->
# Code map — `soap-cxf-gateway` (project: `shop-backend`)

> Thin by design: below the code-map frequency threshold, but recorded rather than
> left a bare [_candidates] row because the below-threshold count is itself the local convention
> (centralization), not a gap.

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`WebServiceConfig.java`](exemplars/web-service-config.md#soap-cxf-gateway) | A `@Configuration` consumer of the centralized `WSFactory` — never builds `JaxWsProxyFactoryBean` itself | adapter (`partner-gateway`) |

### Excerpts
SOAP client bean delegated to the shared factory (full source in the [exemplar leaf](exemplars/web-service-config.md#soap-cxf-gateway)):
```java
@Bean
public RegistryPortType registryPortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
    return (RegistryPortType) wsFactory.createProxyFactoryBean(
            registryBaseUrl,
            "/wsdls/RegistryWebService-v2.wsdl",
            "{urn:com:acme:shop:partner:registry:protocol:v2}RegistryService",
            RegistryPortType.class,
            "{urn:com:acme:shop:partner:registry:protocol:v2}RegistryPort",
            sessionTokenSecurityOutInterceptor
    ).create();
}
```

### Edge cases
Adding a fourth SOAP client:
```java
@Bean
public SupplierServicePortType supplierServicePortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
    return (SupplierServicePortType) wsFactory.createProxyFactoryBean(...).create();
}
```
Expected: the new client still goes through `WSFactory`; no per-gateway `JaxWsProxyFactoryBean` wiring appears.

Absent security interceptor bean:
```java
contextRunner.run(context -> context.getBean("sessionTokenSecurityOutInterceptor"));
```
Expected: startup fails loudly at wiring time instead of creating a partially secured SOAP client.

Endpoint swap per environment:
```java
properties.endpoint.partner.registry=https://registry.example.invalid
```
Expected: only configuration changes; the factory wiring stays identical.

## Local conventions (the project facts the skill omits)
- Package root: `webservice-config/src/main/java/com/acme/shop/webservice/config/ws/WSFactory.java`
  is the **only** place in the repo that touches `JaxWsProxyFactoryBean` — every SOAP gateway's
  `@Configuration` class calls into it rather than building a proxy factory bean itself.
- Naming shape: consumers are regular `*Config` / `*WebServiceConfig` classes (see
  [bean-config-di]); `WSFactory` itself has no `*Gateway`/`*Client` naming convention
  since it's a factory, not a gateway.
- Required collaborators / base types: a WSDL path + target service interface, passed to `WSFactory`,
  returns a configured CXF port client.
- Config / wiring: centralized on purpose — **do not** add a second `JaxWsProxyFactoryBean` call
  elsewhere; extend `WSFactory` instead.

## Frequency & coverage (why this stays below the code-map threshold)
- `JaxWsProxyFactoryBean` occurs in exactly 2 files / 1 module (as of `abc1234`): `WSFactory.java`
  (main) and `WSFactoryTest.java` (test) — deliberately centralized, so the low count is the expected
  shape, not an under-adopted pattern. Re-scan if a second `*ProxyFactoryBean` call appears outside
  `webservice-config`.

## Drift / exceptions
- **Probe correction:** the first harvest's [_candidates] recorded 4 files / 1 module for this
  signal; re-scanning finds 2 files (1 main + 1 test) — the higher prior count came from a looser probe.
  The module count (1) was correct.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'JaxWsProxyFactoryBean'` (2 files / 1 module)

[_candidates]: _candidates.md
[bean-config-di]: bean-config-di.md
