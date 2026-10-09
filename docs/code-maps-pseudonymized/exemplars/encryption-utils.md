---
runtime: lazy
generated-by: pattern-scanner
source: encryption/src/main/java/com/acme/shop/encryption/EncryptionUtils.java
serves: [aes-gcm-encryption]
---
<!-- AI_DISCLAIMER v1.0 -->
# Exemplar — `encryption/.../EncryptionUtils.java` (project: `shop-backend`)

> This file has been created (totally or partially) with the assistance of artificial intelligence tools.
> All content has been generated under the direct supervision of a named individual,
> and under the AI.Backbone Orchestrator Compliance framework

## What this artifact is
The single, shared AES/GCM implementation for the repo (`encryption` module), consumed by
`merchant-adapter` and `vendor-relation-adapter` rather than reimplemented per module.

## AES/GCM with a fresh random IV per message {#aes-gcm-encryption}
**Serves:** [`aes-gcm-encryption`](../aes-gcm-encryption.md)

`Cipher.getInstance("AES/GCM/NoPadding")`, a 12-byte IV drawn from `SecureRandom` fresh per
`encrypt()` call (never reused with the same key — enforced by construction, not just commented),
`GCMParameterSpec(128, iv)` for a 128-bit auth tag. IV is prepended to the ciphertext
(`ByteBuffer` of `iv + cipherText`) and both are Base64-encoded together, so `decrypt()` can slice
the first `GCM_IV_LENGTH` bytes back off without a separate IV parameter or column. Key is sourced
once via `@Value("${properties.shop.encryption-key}")` (externalized, not hardcoded) into a
`SecretKeySpec`. Both directions null-pass-through (`null` in → `null` out) and wrap any crypto
failure in a domain `EncryptionException` rather than leaking `GeneralSecurityException`.

### Source (pseudonymized)
```java
package com.acme.shop.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionUtils {
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12;
    private final SecretKey encryptionKey;

    public EncryptionUtils(@Value("${properties.shop.encryption-key}") String secretKey) {
        final byte[] decodedKey = secretKey.getBytes();
        this.encryptionKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            var message = "Error encrypting message";
            log.error(message, e);
            throw new EncryptionException(message, e);
        }
    }

    public String decrypt(String cipherMessage) {
        if (cipherMessage == null) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(cipherMessage);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, bytes, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmIv);

            byte[] plainText = cipher.doFinal(bytes, GCM_IV_LENGTH, bytes.length - GCM_IV_LENGTH);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            var message = "Error decrypting message: " + cipherMessage;
            log.error(message, e);
            throw new EncryptionException(message, e);
        }
    }
}
```

### Edge cases
Null passthrough:
```java
assertThat(encryptionUtils.encrypt(null)).isNull();
assertThat(encryptionUtils.decrypt(null)).isNull();
```
Expected: the utility preserves `null` values rather than emitting sentinel ciphertext.

Tampered payload:
```java
assertThatThrownBy(() -> encryptionUtils.decrypt("not-base64"))
        .isInstanceOf(EncryptionException.class);
```
Expected: malformed or tampered values fail with `EncryptionException` after logging.

Repeat encryption of the same plaintext:
```java
var first = encryptionUtils.encrypt("Jane Doe");
var second = encryptionUtils.encrypt("Jane Doe");
assertThat(first).isNotEqualTo(second);
```
Expected: a fresh IV yields different ciphertext for the same plaintext.

## Provenance
- Scanned at: `abc1234` · tool/query: `rg 'EncryptionUtils' --glob '*.java'`
