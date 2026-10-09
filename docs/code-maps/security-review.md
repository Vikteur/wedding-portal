---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map - `security-review` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

The [security-review] skill carries the generic review method and deliberately names no project
facts. This leaf carries the half the method is useless without: **which obligations apply here, and
which authority each one comes from.** A finding phrased as "avoid logging identifiers" is advice;
"an NID in an application log breaches GDPR data minimisation for a special-category purchase-data
dataset" is a reason someone can act on and, if they disagree, argue with.

Nothing in a model's training data knows the obligations a regulated commerce platform operates
under. That is why this file exists rather than being inferred, and why a human owns it.

## The authorities

| Short name | What it is | What it binds here |
|---|---|---|
| **GDPR Art. 5(1)(c)** | data minimisation | personal identifiers are collected or handled only where needed for the purpose - an NID in a URI, a log line or a cache key is data handling without a purpose |
| **GDPR Art. 9** | special categories | special-category purchase data (CatalogXml payloads, order history, product restrictions, promotion records, campaign records, catalog summaries) is prohibited data handling unless an exception applies; it never lands in a log or a committed fixture |
| **GDPR Art. 32** | security of handled data | encryption at rest for identifiers, integrity and confidentiality on transport |
| **GDPR Art. 30 / partner-platform audit obligation** | records of handled data | every access to another customer's data is attributable and retained - this is why the audit trail is not optional and why removing an entry is a regulatory act, not a refactor |
| **Partner platform conventions** | NID handling, WS-Security, STS/SAML | NID never appears in a URI; SOAP calls to partner services are signed and encrypted; STS assertions are validated for audience and lifetime |

**NID** (national identifier) is the single highest-risk value in this codebase. It is directly
identifying, it is *not* revocable, and it is the join key to special-category purchase data - which
makes any leak both permanent and high-consequence by association.

## Where the controls actually live

| Control | Where | Notes |
|---|---|---|
| Encryption at rest | [`EncryptionUtils`][enc] (`encryption` module) | `encrypt(String)` / `decrypt(String)`, AES/GCM, key from `properties.shop.encryption-key`. See [aes-gcm-encryption] |
| NID masking at the sink | `logging/src/main/java/com/acme/shop/logging/NidMasker.java` | SHOP-1406. Modulo-97 checksum-validated, so unrelated 11-digit numbers stay readable. Wired **only** into `ROLL_APPLICATION_LOG` via `MaskingJsonGeneratorDecorator` - see the gap below |
| Audit trail | `application/src/main/java/com/acme/shop/audit/LogActorAspect.java` | `@LogUser(action=...)` on a controller method, `@LogParam` on parameters; writes to the **`AUDIT_LOG`** logger |
| Audit sink | `application/src/main/resources/logback-container.xml` | `${AUDIT_PATH}/${SHOP_LOG_ID}-audit-shop-backend.log`, separate from the application log |
| Filter chains | `application/src/main/java/com/acme/shop/configuration/ShopSecurityConfig.java` | **two** chains: `adminFilterChain` `@Order(101)` (`/api/admin/**`), `apiFilterChain` `@Order(102)`. Order is load-bearing |
| Error responses | `application/src/main/java/com/acme/shop/exceptionhandling/ApiExceptionHandler.java` | the single place a client-visible message is shaped. See [exception-to-http] |
| Principal | `ShopAuthentication` (`getActiveNid()`, `getUserNid()`), `AuthenticatedCustomer` | the active-vs-user split is how delegation ("acting for someone else") is represented |
| STS / SAML | `webservice-config/src/main/java/com/acme/shop/webservice/config/sts/` | `NotOnOrAfter` is checked here; see [soap-cxf-gateway] |
| Pseudonymisation | see [identifier-pseudonymization] | the REST-resource / fulfilment path exchanges NID for a pseudonym before it leaves |

### Excerpts
AES/GCM with an externalized key and a fresh IV per message:
```java
public class EncryptionUtils {
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12;
    private final SecretKey encryptionKey;

    public EncryptionUtils(@Value("${properties.shop.encryption-key}") String secretKey) {
        final byte[] decodedKey = secretKey.getBytes();
        this.encryptionKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public String encrypt(String plaintext) {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cipherText.length)
                .put(iv)
                .put(cipherText)
                .array());
    }
}
```

Checksum-aware masking, so only real NID-shaped values are hidden:
```java
public final class NidMasker {
    public static final String MASK = "***********";

    public static String mask(final String input) {
        final Matcher matcher = CANDIDATE.matcher(input);
        while (matcher.find()) {
            if (!isNid(digitsOf(matcher.group()))) {
                continue;
            }
            masked.append(input, copiedUpTo, matcher.start()).append(MASK);
        }
        return masked == null ? input : masked.append(input, copiedUpTo, input.length()).toString();
    }
}
```

Audit logging records acting-vs-subject identity on the dedicated logger only:
```java
if (auth instanceof ShopAuthentication shopAuthentication) {
    final LogUser annotation = method.getAnnotation(LogUser.class);
    if (annotation != null) {
        String statement = "%s | %s | %s".formatted(
                shopAuthentication.getActiveNid(),
                shopAuthentication.getUserNid(),
                annotation.action());
        LOGGER.info(statement);
    }
}
```

The admin and app filter chains are separate and explicitly ordered:
```java
@Bean
@Order(101)
public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/admin/**")
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers("/api/admin/**").authenticated());
    return http.build();
}

@Bean
@Order(102)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers("/api/page").permitAll()
                    .requestMatchers("/api/**").authenticated());
    return http.build();
}
```

### Edge cases
Valid NID vs unrelated 11-digit number in the same log line:
```java
assertThat(NidMasker.mask("nid=12010112335 order=12345678901"))
        .isEqualTo("nid=*********** order=12345678901");
```
Expected: only the checksum-valid NID is masked; the unrelated 11-digit order number stays readable.

Checksum-invalid test fixture:
```java
assertThat(NidMasker.mask("fixture=12010112399 checksum-invalid"))
        .isEqualTo("fixture=12010112399 checksum-invalid");
```
Expected: invalid examples remain visible, so fake fixtures do not look like real identifiers.

NID in a URI:
```java
GET /api/customer/12010112335/orders
```
Expected: critical review finding. The identifier would leak through proxies, access logs and caches.

NID in an exception message:
```java
throw new IllegalStateException("Could not load account for 12010112335");
```
Expected: critical review finding. `ApiExceptionHandler` can echo messages into responses and logs.

Audit logger vs application logger:
```java
AUDIT_LOG.info("%s | %s | viewed order".formatted(activeNid, userNid));
LOGGER.info("activeNid={}", activeNid);
```
Expected: the first line is deliberate compliance logging on the segregated sink; the second is a leak.

NID in a cache key:
```java
@Cacheable(cacheNames = "orders", key = "#nid")
```
Expected: critical review finding. Cache keys outlive the request and show up in diagnostics.

## Critical - block the merge

**NID and PII**
- **NID persisted unencrypted.** A column holding an NID is written through `EncryptionUtils.encrypt`
  and read through `decrypt`. A repository that puts a raw NID on a JPA entity field is critical.
  *Authority: GDPR Art. 32.*
- **NID in an application log, exception message, or response body** - including at DEBUG, and
  including via `toString()` on a domain object or DTO that carries one. *Authority: GDPR Art. 5(1)(c).*
  The one legitimate sink is the `AUDIT_LOG` logger, which records it deliberately and is
  the obligation, not a leak - which is why `AUDIT-FILE` is `additivity="false"` and carries no
  masking decorator.
  **`NidMasker` does not make this rule optional.** It is defence in depth at one sink, not a licence
  to pass identifiers to loggers: it only decorates the JSON encoder on `ROLL_APPLICATION_LOG`, so a
  value that reaches any other appender is unmasked. Keep the NID out of the statement.
- **NID as a path or query parameter.** Use the authenticated session, or a non-identifying handle.
  A URI is logged by every proxy, gateway and access log between the client and the service, none of
  which are in scope for this system's retention rules. *Authority: partner-platform convention + Art. 5(1)(c).*
- **NID in a cache key** (`ConcurrentMapCacheManager` and peers). Keys are dumped in diagnostics and
  survive well past the request. Use a hash or surrogate.
- **Special-category purchase data in a log, a test fixture, or a committed file.** Decrypted
  CatalogXml, catalog summaries, order records, product restrictions, promotion records and campaign
  records may pass through transformation; they must not be persisted anywhere outside their store.
  *Authority: GDPR Art. 9.*

**Authentication and authorization**
- **A new `permitAll()`, a removed `.authenticated()`, or a CSRF/CORS relaxation** in
  `ShopSecurityConfig` - call out the exact path and the surface it exposes. The `permitAll` list is
  long and each entry was a decision; a new one must be too.
- **Reordering or merging the two filter chains.** `@Order(101)` admin before `@Order(102)` api is
  the boundary between the two audiences.
- **An operation on another subject's data without a principal-vs-target check.** Taking an
  identifier from the request and trusting it is privilege escalation - this system has a genuine
  delegation model (`getActiveNid()` vs `getUserNid()`), so "the caller is authenticated" is never
  the same question as "the caller may act for this customer".
- **A credential or key in committed config**, placeholder or not. A committed secret is compromised
  regardless of its value.

**Partner integration**
- **WS-Security signing or encryption disabled** on any `partner-gateway` / `partner-hubservices` /
  `fulfilment-gateway` / `payments-gateway` call - a commented-out interceptor or an `<wsp:Optional>` counts.
- **STS token cached past its lifetime.** A `@Cacheable` on token issuance without a TTL bounded by
  `NotOnOrAfter` is critical.
- **A SAML assertion accepted without checking audience and `NotBefore`/`NotOnOrAfter`.**

**Audit**
- **An audit entry removed for an action that had one** - `@LogUser` deleted, or a controller method
  moved out from under `LogActorAspect`'s pointcut. Audit completeness is a regulatory obligation, so
  this needs explicit justification in the PR, not a reviewer's shrug. *Authority: GDPR Art. 30.*

## Warnings - should mitigate

- A use case operating on customer data whose controller entry point carries no `@LogUser` - the
  trail has a hole that only shows up when someone asks who accessed what.
- Test data using a **real-looking NID**. National NIDs carry a checksum, so a realistic-looking
  fixture is plausibly someone's. Use a known-invalid one.
- A new `*Gateway` with no integration test - gateways drift silently against partner-side schema
  changes, and the failure mode is a security control quietly not applying.
- Responses that differ depending on whether a record exists (enumeration), or validation errors that
  echo the sensitive value back.
- Cached partner-service responses keyed without a per-customer segregator - cross-customer cache bleed.

## What is enforced mechanically (so review can spend its attention elsewhere)

- **`policy-gates`** (`.github/workflows/policy-gates.yml` -> `.github/agentic-gates/check-policy.sh`)
  on every PR and push to `main`, across the **whole repo** - not path-filtered to the agentic
  surface, because a leak lands in a gateway module. It checks changed files for three things: a
  logging call that interpolates an NID-named expression (the `AUDIT_LOG` logger is exempt - it is
  the obligation, not a leak), an NID-shaped literal, and an obviously committed credential.
  It is a **grep, not a scanner.** It sees the literal form and nothing else: an NID reaching a log
  through a variable, a `toString()`, a DTO field or a formatter passes it cleanly. A green run means
  the obvious mistakes are absent, not that the diff is safe - the sections above are still the job.
  It scans the **diff**, not the tree: a full-tree run currently reports 84 pre-existing findings
  (5 identifier-in-log, 69 NID-shaped literals mostly in test fixtures, 10 credentials), so a
  blocking whole-tree gate would be unmergeable on day one. Run
  `bash .github/agentic-gates/check-policy.sh --all` to see that backlog. (Was 96 before SHOP-1406,
  which removed the NID from 20 log statements in `DefaultCustomerConsentGateway` and
  `RegistryLookupGateway` rather than relying on the masker - the right fix, and the reason that
  count moved.)
- **Worth adding:** a real secret scanner (gitleaks / trufflehog). Deliberately not added here as an
  unpinned third-party action - this repo pins every action by SHA, and a supply-chain hole in a
  security gate is a bad trade for a better regex.
- **`:application:archTest`** - `BatchJobLoggingConventionTest` requires `@BatchJob` on every
  `@Scheduled` method, so a batch job cannot run unlogged.

## Drift / exceptions

- **`CONSOLE` is not masked, and it is on the root logger.** `MaskingJsonGeneratorDecorator` is a
  *JSON* decorator, so it can only apply to `ROLL_APPLICATION_LOG`; `CONSOLE` uses a plain
  `${CONSOLE_LOG_PATTERN}` layout and root fans out to both. Latent rather than live - application
  code no longer logs NIDs - but it means the masker is not a backstop for the ones that slip in.
  Whether it matters depends on whether container stdout is collected and retained, which is a
  platform question this file cannot answer. **Worth confirming with whoever owns log shipping.**
- The `permitAll()` list in `apiFilterChain` covers public content (`/api/page`, `/api/faq`,
  `/api/widget`, `/api/search/**`) and public checkout flows, which are reachable by code rather
  than by login. Treat additions as security changes even though the pattern exists.
- `getActiveNid()` and `getUserNid()` differing is normal (delegation), not a bug.

## Provenance

Hand-written 2026-08-06 to close audit finding SHOP-C6 (`docs/conclusion.md`): 52 skill bodies
contained zero mentions of GDPR, partner-platform obligations, retention, residency, consent or
lawfulness, and the `<rule>` placeholder in the security-review skill had no authority to resolve
to. Content was ported from an untracked machine-local harness and **re-verified against this repo**
- several symbols it named (`Nid.masked()`, `AuditLogPublisher`, `@PreAuthorize`) do not exist here
and were replaced with what does. Anything below Critical that could not be verified was dropped
rather than carried over.

**This file needs a human who knows the actual obligations.** The mechanism half is checkable from
the code; the authority half is not, and a wrong citation is worse than none because it will be
quoted in a review.

[security-review]: ../../.github/skills/security-review/SKILL.md
[aes-gcm-encryption]: aes-gcm-encryption.md
[exception-to-http]: exception-to-http.md
[identifier-pseudonymization]: identifier-pseudonymization.md
[soap-cxf-gateway]: soap-cxf-gateway.md
[enc]: exemplars/encryption-utils.md
