---
name: spring-boot-slice-tests
description: Quarkus tests for the web and persistence adapters, collaborators mocked. Use for adapter tests.
---
# Quarkus adapter tests

> **Generic "how" only.** No concrete resource/entity names in the body — those live in the code-map leaf.

## When to use
Testing adapter-layer code through the real framework runtime: a REST resource's
request/response/validation/error mapping, or a repository's queries/mappings. Quarkus has no
web-slice context, so a plain unit test of a resource would skip routing, bean validation and the
error mapper — use a `@QuarkusTest`.

## How
- **Resources**: `@QuarkusTest` + REST Assured; mock the use-case collaborators with `@InjectMock`;
  assert status, body and error mapping. A test that makes a real outbound HTTP call stubs it with
  [[wiremock-gateway-stubs]].
- **Repositories**: `@QuarkusTest` against a real database started by Dev Services on Testcontainers
  ([[testcontainers]], so dialect/migrations are exercised), not an in-memory substitute when the
  prod DB differs.
- Share setup through an abstract base test per kind (`AbstractResourceTest` / `AbstractRepositoryTest`)
  so config (test profile, security test setup, container) is declared once.
- Keep these tests in the slow set; unit tests of domain and use-cases stay plain JUnit.
- Build inputs with object-mother/builders ([[object-mother-builders]]); keep each test one behavior,
  named `given…_when…_then…` — the discipline is [[jvm-testing]]'s, an adapter test does not relax it.

## Pattern signals (discovery cues)
`@QuarkusTest` annotations; `given().when().get(...)` REST Assured chains; `@InjectMock`; `@TestProfile`;
abstract `*ResourceTest` / `*RepositoryTest` base classes; Dev Services or testcontainers for the DB;
`given_when_then` method names.

## Project specifics → see docs
- Code map (base test classes, container setup, exemplars) → `docs/code-maps/spring-boot-slice-tests.md` *(per-repo map; if marked `kind: worked-example`, follow its structure and map its pseudonymized names to this repo's own)*

## Guardrails (what NOT to do)
- Don't use a `@QuarkusTest` where a plain unit test proves the behavior — it is the slow set.
- Don't mock the repository under test — exercise the real query.
- Don't assert on full JSON blobs where a targeted field/status assertion is clearer.

## Definition of done
- [ ] The test kind matches the unit under test; collaborators mocked (resource) or DB real (repo);
  base test reused; mothers for inputs; behaviors are red before the production code makes them green.
