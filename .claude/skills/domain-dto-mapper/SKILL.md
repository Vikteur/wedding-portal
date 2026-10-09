---
name: domain-dto-mapper
description: A pure, field-explicit mapper between domain and transport DTO. Use when a controller or gateway crosses that edge.
---
# Domain-to-DTO mapper

## When to use
Crossing the boundary between a domain object and a generated/transport DTO — in a controller
(domain → response DTO) or a gateway (wire DTO → domain).

## How
- **Scaffold the base case first.** If the code map has a `## Scaffold` section, render each listed
  template with the scaffold script, filling its variables from the ticket and plan. Then hand-write only
  what that section says the template leaves out (edge cases, extra rules, imports).
- Put the conversion in a **dedicated mapper** (a class or static methods), not inline in the
  controller/gateway. Keep it **pure** (no I/O, no Spring) so it's unit-testable in isolation.
- Map **field by field, explicitly**; handle nulls/optionals/collections deliberately; translate enums.
- Keep direction clear (separate `toDto` / `toDomain`); if both exist, make the round-trip stable.
- Don't put business logic in the mapper — it only restructures data.
- Pair it with a mapper test ([[domain-to-dto-mapper-tests]]).

## Pattern signals (discovery cues)
Classes named `*DomainToDTOMapper` / `*Mapper` in the adapter layer with `toDto`/`toDomain`-style
methods; no framework/IO deps; a sibling `*MapperTest`.

## Project specifics → see docs
- Code map (mapper exemplars, hand-coded vs generated DTOs) → `docs/code-maps/domain-dto-mapper.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't hand-write what a scaffold template generates, and don't patch around a wrong template in its output — fix the template.
- Don't map inline in controllers/gateways — use the dedicated mapper.
- Don't put business rules or I/O in a mapper.
- Don't silently drop fields — map explicitly, cover edges in the test.

## Definition of done
- [ ] Base case rendered from the code map's scaffold templates; only the edge cases are hand-written.
- [ ] Pure dedicated mapper, explicit field mapping, edges handled, no business logic; a mapper test asserts each field + round-trip where applicable.
