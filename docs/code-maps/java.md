# Code map: Java

- **Language level**: Java 25 (LTS), set once as the build toolchain and release level; no preview features (A §1, UD-2, FW-C-70; `docs/memory.md`).
- **Build tool**: Gradle (wrapper 9.8.0, Kotlin DSL), one multi-module build; versions only in `gradle/libs.versions.toml` (UD-4; A §1 "Build" row).
- **Framework**: Quarkus 3.39.1 (catalog entry `quarkus`).
- **Modules** (`settings.gradle.kts`): `rekord-domain`, `rekord-usecase`, `rekord-adapter`, `rekord-gateway`, `application`, `logging`.
- **Allowed libraries today** (`gradle/libs.versions.toml`): the Quarkus BOM, AssertJ 3.27.7, WireMock standalone 3.13.2, plus the Quarkus and Jandex plugins. Further libraries come back only with the feature that needs them, in the module that owns that feature (A §1 "Not carried over"). `rekord-domain` takes no framework library at all (FW-C-01).
- **Idioms**: immutability, records for value objects, `Optional` at boundaries (generic `java` skill).
