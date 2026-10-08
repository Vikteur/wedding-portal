---
type: C4 Component
title: Password hashing
status: stable
groma:
  id: password-hashing
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/security/Passwords.java
      symbol: Passwords
    - scanner: java
      file: rekord-usecase/src/main/java/app/rekord/usecase/identity/port/PasswordHasher.java
      symbol: PasswordHasher
description: Hashes and verifies passwords with scrypt
---

Implements the use-case password port with scrypt in rekord-api's stored format, with the salt drawn from the token generator. Verification reads the cost, salt and key length from the stored value, so the cost can rise later.
