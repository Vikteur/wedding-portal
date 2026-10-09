---
name: usecase-orchestration
description: Transactional use-case over domain and ports, free of web and persistence types. Use for the use-case layer.
---
# Use-case orchestration

## When to use
Implementing application logic that coordinates domain objects and repository ports for one business
operation — the layer between controllers and the domain.

## How
- **Scaffold the base case first.** If the code map has a `## Scaffold` section, render each listed
  template with the scaffold script, filling its variables from the ticket and plan. Then hand-write only
  what that section says the template leaves out (edge cases, extra rules, imports).
- One use-case = one operation. It takes a **request object**, calls domain + repository **ports**, and
  returns a **response object** (or domain type) — never a web DTO or a persistence entity.
- Own the **transaction boundary** here (`@Transactional` at the use-case), not in the controller or
  repository. Keep the transaction tight.
- Depend on **interfaces** (repository/gateway ports) defined in this/the inner layer; the adapter
  provides the implementations.
- Put business rules/invariants in the **domain**; the use-case orchestrates, it doesn't reimplement them.
- Validate the request up front; surface failures as typed domain/application exceptions.

## Pattern signals (discovery cues)
Classes named `*UseCase`/`*Service` in a use-case module with `@Transactional`; constructor-injected
repository/gateway interfaces; request/response value objects; no web/JPA imports.

## Project specifics → see docs
- Code map (use-case exemplars, transaction + port conventions) → `docs/code-maps/usecase-orchestration.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't hand-write what a scaffold template generates, and don't patch around a wrong template in its output — fix the template.
- Don't accept/return web DTOs or persistence entities — use request/response + domain types.
- Don't put the transaction boundary in the controller or repository.
- Don't reimplement domain invariants in the use-case — delegate to the domain.

## Definition of done
- [ ] Base case rendered from the code map's scaffold templates; only the edge cases are hand-written.
- [ ] One operation per use-case; transaction owned here; depends on ports; request/response objects; rules live in the domain; a test drives it with mocked ports.
