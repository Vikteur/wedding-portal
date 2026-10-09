---
runtime: lazy
kind: worked-example
---
> **Worked example, not project facts.** The structure is the pattern to follow: the layers, the
> classes and their roles, the call order, the tests. Every name is a pseudonymized placeholder: the project, the
> packages, the classes, the methods, the parameters and the fields. Map each one to this repo's own name, and never
> copy a placeholder into code.

<!-- AI_DISCLAIMER v1.0 -->
# Code map — `aes-gcm-encryption` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## Where this pattern lives (exemplars)
| Exemplar | What it shows | Layer / module |
|----------|---------------|----------------|
| [`EncryptionUtils.java`](exemplars/encryption-utils.md#aes-gcm-encryption) | AES/GCM with a fresh random IV per message, IV prepended to ciphertext, both Base64-encoded together | `encryption` module |

### Excerpts
Fresh IV per encrypt call (full source in the [exemplar leaf](exemplars/encryption-utils.md#aes-gcm-encryption)):
```java
byte[] iv = new byte[GCM_IV_LENGTH];
secureRandom.nextBytes(iv);
final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
```

Single blob carrying IV + ciphertext:
```java
ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
byteBuffer.put(iv);
byteBuffer.put(cipherText);
return Base64.getEncoder().encodeToString(byteBuffer.array());
```

### Edge cases
`null` input:
```java
assertThat(encryptionUtils.encrypt(null)).isNull();
assertThat(encryptionUtils.decrypt(null)).isNull();
```
Expected: the utility preserves `null` passthrough instead of manufacturing placeholder ciphertext.

Tampered or malformed payload:
```java
assertThatThrownBy(() -> encryptionUtils.decrypt("not-base64"))
        .isInstanceOf(EncryptionException.class);
```
Expected: crypto failures are wrapped in the repo's domain exception after logging.

Same plaintext encrypted twice:
```java
var first = encryptionUtils.encrypt("Jane Doe");
var second = encryptionUtils.encrypt("Jane Doe");
assertThat(first).isNotEqualTo(second);
```
Expected: ciphertext changes because each message gets a fresh random IV.

## Local conventions (the project facts the skill omits)
- Package root: dedicated `encryption` module — a single shared implementation, not per-capability.
- Naming shape: `EncryptionUtils` (`@Service`), `encrypt(String)`/`decrypt(String)`, an
  `EncryptionException` wrapping any crypto failure.
- Required collaborators / base types: `SecureRandom` field, `SecretKey` built once from
  `@Value("${properties.shop.encryption-key}")`.
- Config / wiring: key externalized via config property, never hardcoded; 12-byte IV, 128-bit auth
  tag (`AES/GCM/NoPadding`).

## Frequency & coverage (why this earned a skill)
- Occurrences: `EncryptionUtils` referenced in 15 files across 3 modules — `encryption`,
  `merchant-adapter`, `vendor-relation-adapter` (as of `abc1234`).

## Drift / exceptions
- None observed — one shared implementation, consumed rather than reimplemented.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'EncryptionUtils' --glob '*.java'`
