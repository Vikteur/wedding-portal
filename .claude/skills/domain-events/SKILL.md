---
name: domain-events
description: Raise framework-free domain events in the domain and publish them from the use case through a port. Use for event-driven work.
---
# Domain Events

## When to use
Adding a side-effect that should fire when a domain fact occurs (indexing, notifying, projecting,
cache invalidation) without coupling the originating logic to that side-effect or to the framework.

## How
- Define a **marker event interface** in the inner layer — a plain empty type all events implement.
  The domain depends only on this marker, never on a framework event base class.
- Model each event as an **immutable value** (a record) named in the **past tense** ("what happened",
  e.g. `*Created` / `*Updated` / `*Deleted`) carrying just the facts a handler needs.
- The **domain raises** events: the aggregate records them as part of the change. It never publishes
  and never calls a port.
- The **use case publishes** them through a port: an interface (owned inside) with a single
  `publish(event)` method. The use-case depends on this port, not on any framework event bus.
- Implement the port **once at the edge** with an adapter that fires a CDI `Event`. This is the only
  place that knows the framework.
- **Subscribe with observers at the edge**, one per side-effect; keep them thin — translate the
  event into a call on the relevant collaborator. Side-effects never live in the publisher.
  `@Observes` runs inside the transaction; `@Observes(during = TransactionPhase.AFTER_SUCCESS)` runs
  after commit — use it when the side-effect must not react to rolled-back work.
- Publish **after the state change is valid** (only on the success path).

## Pattern signals (discovery cues — how the scanner recognizes this in any codebase)
- A marker `*Event` interface in an inner package with **no framework imports**.
- Event types as past-tense records implementing that marker.
- A publisher **port interface** inside + a single adapter implementing it over CDI `Event`.
- Edge observers (`@Observes`) subscribing to event types, one per side-effect.
- Use-cases depending on the publisher port, never on a framework event type.

## Project specifics → see docs
- Code map (marker interface, event types, publisher port + adapter, observers, exemplars) → `docs/code-maps/domain-events.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't make domain events extend or import a framework event type — keep the marker framework-free.
- Don't publish from the domain or fire a framework type directly from a use-case; go through the port.
- Don't put side-effect logic in the publisher — that belongs in observers.
- Don't publish before the change is valid/committed (avoid acting on rolled-back work).

## Definition of done
- [ ] Marker event interface is framework-free; events are past-tense immutable values raised in the domain.
- [ ] Publishing is by the use case behind a port; one edge adapter bridges to CDI `Event`.
- [ ] Side-effects live in thin observers; after-commit work uses `TransactionPhase.AFTER_SUCCESS`.
- [ ] Verified by: a domain-layer test passing with no framework event type on the classpath, and an observer test asserting the side-effect fires only after commit.
