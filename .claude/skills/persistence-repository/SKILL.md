---
name: persistence-repository
description: Implement a repository port as a Hibernate ORM with Panache adapter, keeping persistence out of inner layers. Use when persisting state.
---
# Persistence repository (adapter)

## When to use
Backing a use-case's repository **port** (interface in the inner layer) with a real datastore — the
adapter that turns domain aggregates into rows and back.

## How
- The **port** (interface) lives in the use-case/inner layer; the adapter class **implements** it
  (a thin `Default<X>Repository` delegating to a package-private Panache repository).
- Use the Panache **repository** pattern, not active-record: behaviour on entities would leak
  persistence into the model.
- Map **domain ↔ entity** at this edge; the domain never sees `@Entity` types and the entity never
  leaks outward.
- Keep queries in the Panache repository / explicit query methods; keep transactions owned by the
  **use-case** ([[usecase-orchestration]]), not the repository.
- Return domain types (or `Optional<domain>`); translate persistence exceptions to typed errors.
- Cover it with a real-database integration test using [[testcontainers]].

## Pattern signals (discovery cues)
A use-case-layer `*Repository` interface implemented by an adapter `Default*Repository`
(`@ApplicationScoped`); a sibling `PanacheRepository`; domain↔entity mapping at the boundary.

## Project specifics → see docs
- Code map (port↔impl pairs, entity mapping, query conventions) → `docs/code-maps/persistence-repository.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't let `@Entity` / persistence types cross into domain or use-case code.
- Don't own the transaction in the repository — the use-case does.
- Don't return entities to callers — return domain types.

## Definition of done
- [ ] Port in the inner layer, `@ApplicationScoped` adapter implements it; domain↔entity mapped at the edge; returns domain types; a Testcontainers test covers a query.
