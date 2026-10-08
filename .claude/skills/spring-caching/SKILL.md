---
name: spring-caching
description: Cache read-heavy results with quarkus-cache and invalidate on mutation, with per-cache TTL. Use when a read repeats expensive work.
---
# Caching with quarkus-cache

> **Generic "how" only.** No cache names, TTL values, or property keys in the body — those live in
> the code-map leaf.

## When to use
A read method does expensive or remote work whose result is stable for a while and its inputs repeat:
reference data, access matrices, translations, remote lookups. Add `@CacheResult`; invalidate on writes.

## How
- Add the quarkus-cache extension (Caffeine underneath); nothing to enable. Annotate read methods with
  `@CacheResult(cacheName = "<cacheName>")`. Mark the argument that identifies the value with
  `@CacheKey`; a method with several arguments caches by all of them unless marked.
- Invalidate on every mutation: put `@CacheInvalidateAll(cacheName = "<cacheName>")` (or
  `@CacheInvalidate` with the same `@CacheKey`) on save/delete methods so the cache can't serve stale
  data. Pair invalidation with the same cache name the reads use — a typo silently disables it.
- Configure each named cache explicitly in configuration: `expire-after-write` (TTL) and a bounded
  `maximum-size`. One entry per cache, tuned to how that data changes — not one global default.
  Externalize TTLs that differ by environment through configuration profiles.
- A method that throws caches nothing; do not cache failure-shaped or empty fallback results by
  returning them from the cached method — cache only real values.
- Cache at the right boundary: read-heavy, slow, or remote lookups whose inputs repeat. Don't cache
  fast in-process calls or per-request unique data.

## Pattern signals (discovery cues)
`@CacheResult` / `@CacheInvalidate` / `@CacheInvalidateAll` annotations on repository/gateway
methods; `quarkus.cache.caffeine."<name>".*` keys declaring per-cache TTL and size; tests that
invalidate caches between cases.

## Project specifics → see docs
- Code map (named caches + TTLs, invalidating mutations, test config, exemplars) → `docs/code-maps/spring-caching.md` *(per-repo map, written by the pattern-scanner — resolves once harvested)*

## Guardrails (what NOT to do)
- Don't add `@CacheResult` to a read without a matching invalidation on its mutations — stale data.
- Don't cache failures or empty fallbacks as if they were real results.
- Don't reference a cache name that isn't configured with a TTL and a bound.
- Don't cache user-specific data under a shared key — scope the key to the identifying argument.

## Definition of done
- [ ] Read is `@CacheResult` under a configured cache (TTL + bound); every mutation
  invalidates the same cache; error/empty results aren't cached; a test proves a second call hits the cache
  and a mutation invalidates it.
