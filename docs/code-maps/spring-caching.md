# Code map: caching

The file name is kept because the generic skill `spring-caching` links it; this stack is Quarkus, not Spring.

- **Provider**: `quarkus-cache` backed by Caffeine (18 C-28, docs/rewrite/analysis/18-framework-code-rulebook.md:555; A §1 stack table, "Cache" row, line 68 onward).
- **Annotations**: `@CacheResult` and `@CacheInvalidate` stand in for `@Cacheable` / `@CacheEvict`; per-cache expiry and maximum size are set in configuration (A §1, FW-C-51).
- **Not built yet**: no cache exists in this repository and `quarkus-cache` is not a dependency yet. Add it only with the first feature that needs a cache, and keep the TTL and size bounds in configuration.
