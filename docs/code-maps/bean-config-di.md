---
runtime: lazy
kind: worked-example
---

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `bean-config-di` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`WebServiceConfig.java`](exemplars/web-service-config.md#bean-config-di) | `@ApplicationScoped` config class, `@Produces` + `@Named` methods composing a project-local factory helper | adapter (`partner-gateway`) |

### Excerpts
Config class plus declarative bean production (full source in the [exemplar leaf](exemplars/web-service-config.md#bean-config-di)):
```java
@ApplicationScoped
public class WebServiceConfig {
    @Produces
    @Singleton
    @Named("partnerSessionTokenOutInterceptor")
    Interceptor<SoapMessage> partnerSessionTokenOutInterceptor() {
        return wsFactory.createSessionTokenOutInterceptor();
    }
}
```
The SOAP port clients are declared in `application.properties` (`quarkus.cxf.client."registry".*`) and
injected with `@CXFClient("registry")`.

### Edge cases
Two modules define `WebServiceConfig`:
```java
@ApplicationScoped
class WebServiceConfig { ... }   // com.acme.shop.partner.config and com.acme.shop.fulfilment.config
```
Expected: no ambiguity — CDI resolves by type and qualifier; the module-prefixed `@Named` values on the
producers are what must stay unique.

Shared helper bean is absent:
```java
public WebServiceConfig(WSFactory wsFactory) { ... }   // no WSFactory bean on the classpath
```
Expected: the build/startup fails immediately with an unsatisfied dependency; producer methods do not
hide absent collaborators.

A producer method starts hand-building proxies:
```java
@Produces
RegistryPortType registryPortType() {
    return new JaxWsProxyFactoryBean().create(RegistryPortType.class);
}
```
Expected: reject the change — declare the client under `quarkus.cxf.client."<name>".*`, inject it with
`@CXFClient`, and keep interceptor construction in `WSFactory`.

## Local conventions (the project facts the skill omits)
- Package root: `<module>/.../config/`.
- Naming shape: `*Config` (for example `WebServiceConfig`, `SalesRegionCacheConfig`,
  `PartnerRestClientConfig`); produced beans carry a module-prefixed `@Named("partner...")` (or a custom
  qualifier) when the same type is produced in more than one module.
- Required collaborators / base types: constructor injection of `@ConfigProperty`/`@ConfigMapping`-sourced
  config and shared infra beans (the CXF `Bus` is managed by the Quarkus CXF extension, not injected);
  `@Produces` methods stay declarative, delegating actual construction logic to a helper class.
- Config / wiring: properties live in `application.properties` with `%dev.`/`%test.`/`%prod.` prefixes;
  scheduler configs use `@IfBuildProfile`/`@UnlessBuildProfile("test")` to disable jobs under test.

## Frequency & coverage (why this earned a skill)
- Occurrences: config classes (`@ApplicationScoped` in `*Config.java`) — 70 matches / 67 files / 24
  modules; `@Produces` — 90 matches / 43 files / 24 modules (as of `abc1234`).

## Drift / exceptions
- None observed beyond the module-prefixed `@Named` convention noted above.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@ApplicationScoped' --glob '*Config.java'`, `rg '@Produces' --glob '*.java'`
