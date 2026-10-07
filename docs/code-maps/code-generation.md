# Code map: code generation

Decided in architecture-conventions §4 (line 232); not built yet: no `openApiGenerate` task exists in the tree today, and the generator version is not yet in `gradle/libs.versions.toml`. The contract tag in use is `rekordContractTag` in `gradle.properties`.

| Setting | Decision | Source |
|---|---|---|
| Task / plugin | Gradle `org.openapi.generator`, task `openApiGenerate`, in `rekord-adapter` | A §4 (line 232) |
| Generator | `jaxrs-spec` | A §4 |
| Version | pinned in the version catalog (`gradle/libs.versions.toml`); 7.25.0 is the starting point | A §4 |
| Options | `interfaceOnly=true`, `useJakartaEe=true`, `returnResponse=false`, `useSwaggerAnnotations=false`, `openApiNullable=false`, `dateLibrary=java8`, `useTags=true` | A §4 |
| Packages | `app.rekord.api`, `app.rekord.api.model` | A §4 |
| Output | `rekord-adapter/build/generated/openapi`, set explicitly as `outputDir` (the plugin default is elsewhere) so the write guard covers it (18 C-23, line 550) | A §4 |
| Input | the hub's bundled `dist/openapi.yaml` at the pinned tag | A §4 |

Generated code is never edited or committed; a wrong shape is fixed in the contract (P4). A red build on regenerated but unimplemented interfaces is the expected start of a contract feature.
