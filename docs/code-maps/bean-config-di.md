---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `bean-config-di` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`WebServiceConfig.java`](exemplars/web-service-config.md#bean-config-di) | Named `@Configuration`, `@Bean` factory methods composing a project-local factory helper | adapter (`partner-gateway`) |

### Excerpts
Named configuration plus declarative bean creation (full source in the [exemplar leaf](exemplars/web-service-config.md#bean-config-di)):
```java
@Configuration("partnerWebServiceConfig")
public class WebServiceConfig {
    @Bean
    public RegistryPortType registryPortType(Interceptor<SoapMessage> sessionTokenSecurityOutInterceptor) {
        return (RegistryPortType) wsFactory.createProxyFactoryBean(...).create();
    }
}
```

### Edge cases
Two modules define `WebServiceConfig`:
```java
@Configuration("partnerWebServiceConfig")
class WebServiceConfig { ... }
```
Expected: the explicit bean name prevents ambiguous configuration beans with the same class name.

Shared helper bean is absent:
```java
new WebServiceConfig("https://registry.example.invalid", "https://auth.example.invalid", "https://customer.example.invalid", null, bus);
```
Expected: startup fails immediately; bean methods do not hide absent collaborators.

A bean method starts hand-building proxies:
```java
@Bean
public RegistryPortType registryPortType(...) {
    return new JaxWsProxyFactoryBean().create(RegistryPortType.class);
}
```
Expected: reject the change and move the construction logic back into `WSFactory`.

## Local conventions (the project facts the skill omits)
- Package root: `<module>/.../config/`.
- Naming shape: `*Config` (for example `WebServiceConfig`, `SalesRegionCacheConfig`,
  `PartnerRestClientConfig`); an explicit bean name (`@Configuration("partnerWebServiceConfig")`)
  when the class name could otherwise collide across modules.
- Required collaborators / base types: constructor injection of `@Value`-sourced config and shared
  infra beans (for example CXF `Bus`); `@Bean` methods stay declarative, delegating actual construction
  logic to a helper class.
- Config / wiring: `@Profile` is used on scheduler configs to disable jobs under `!test`.

## Frequency & coverage (why this earned a skill)
- Occurrences: `@Configuration` — 70 matches / 67 files / 24 modules; `@Bean` — 90 matches / 43
  files / 24 modules (as of `abc1234`).

## Drift / exceptions
- None observed beyond the explicit-bean-name convention noted above.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg '@Configuration'`, `rg '@Bean'` (both `--glob '*.java'`)
