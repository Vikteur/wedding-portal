---
name: bean-config-di
description: Wire collaborators with CDI producers, config mappings and constructor injection. Use when configuring DI.
---
# Bean configuration & dependency injection

> **Generic "how" only.** No concrete bean/config-class names in the body — those live in the code-map.

## When to use
Wiring a collaborator (client, gateway, cache, scheduler) into the CDI container, or choosing an
implementation per environment.

## How
- Declare non-trivial beans as CDI producer methods (`@Produces`) in dedicated configuration classes;
  keep wiring **out of** domain/use-case code.
- Prefer **constructor injection** (final fields, no `@Inject` needed on a single constructor); avoid
  field injection. Mark discovered beans `@ApplicationScoped`, and centralize wiring of clients and
  third-party types in the producer classes.
- Scope environment-specific beans with build profiles or `@IfBuildProfile`; disambiguate multiples
  with qualifiers.
- Externalize values with a `@ConfigMapping` interface (or `@ConfigProperty` for a single value);
  never inline endpoints/secrets.
- Keep config classes thin — they assemble, they don't compute.

## Pattern signals (discovery cues)
Configuration classes with `@Produces` methods; constructor-injected `final` collaborators;
`@IfBuildProfile` / qualifiers; `@ConfigMapping` / `@ConfigProperty` for externalized config.

## Project specifics → see docs
- Code map (config classes, profiles, properties conventions) → `docs/code-maps/bean-config-di.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't wire beans inside domain/use-case classes — that's the config layer's job.
- Don't use field injection; constructor-inject final fields.
- Don't hardcode env-specific values — externalize and profile them.

## Definition of done
- [ ] Collaborators wired by producers via constructor injection; env variants profiled/qualified; values externalized; no wiring in business code.
- [ ] Verified by: the application starting in a `@QuarkusTest`, and no business class importing the DI framework's injection annotation.
