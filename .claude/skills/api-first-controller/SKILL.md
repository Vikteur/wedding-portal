---
name: api-first-controller
description: Thin controller implementing an OpenAPI-generated Api interface. Use for contract-first REST.
---
# API-first controller

## When to use
Building a REST endpoint in a contract-first project where the controller interface + DTOs are
**generated** from an OpenAPI spec at build time.

## How
- **Scaffold the base case first.** If the code map has a `## Scaffold` section, render each listed
  template with the scaffold script, filling its variables from the ticket and plan. Then hand-write only
  what that section says the template leaves out (edge cases, extra rules, imports).
- Implement the generated `*Api` interface; keep the controller **thin** — validate inputs, call one
  use-case, map domain → generated DTO, return. No business logic here.
- Never hand-edit generated sources; if the shape is wrong, change the **spec** and regenerate.
- Map domain↔DTO in a dedicated mapper (testable in isolation), not inline.
- Let exceptions propagate to the central error handler ([[exception-to-http]]); don't catch-and-swallow.
- Inject the use-case via the constructor; the controller depends inward, never on another adapter.

## Pattern signals (discovery cues)
Controllers `implements <Name>Api` where `*Api` is under a generated source set; an OpenAPI codegen
plugin wired into the build; DTOs from a generated `model` package; a sibling `*DomainToDTOMapper`.

## Project specifics → see docs
- Code map (codegen plugin, tag→controller exemplars, mapper convention) → `docs/code-maps/api-first-controller.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't hand-write what a scaffold template generates, and don't patch around a wrong template in its output — fix the template.
- Don't put business logic or persistence in the controller — delegate to the use-case.
- Don't hand-edit generated interfaces/DTOs — edit the contract.
- Don't map domain→DTO inline — use a tested mapper.

## Definition of done
- [ ] Base case rendered from the code map's scaffold templates; only the edge cases are hand-written.
- [ ] Controller implements the generated interface, delegates to one use-case, maps via a tested mapper, errors flow to the central handler.
