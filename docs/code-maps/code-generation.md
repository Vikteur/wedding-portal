# Code map: code generation

Decided in architecture-conventions §4 (line 232) and built in TASK-2.3: task `openApiGenerate` in `rekord-adapter`, OpenAPI Generator 7.25.0 pinned in the version catalog (`gradle/libs.versions.toml`). The task sets `cleanupOutput`, so each run empties the output and a tag or schema the spec no longer has leaves no stale type. Output lands in `rekord-adapter/build/generated/openapi/src/gen/java`, wired as a source root of `main`. Every compiling run needs `-Pcontract.spec=<rekord-contract checkout>/dist/openapi.yaml` (CI passes `contract/dist/openapi.yaml`). The contract tag in use is `rekordContractTag` in `gradle.properties` (v0.2.0 since TASK-2.4); CI reads it with `contract-pin.sh` and checks that tag out into `contract/`. `CiContractSpecTest` fails when a CI job that starts the application lacks the pin, the checkout or a `-Pcontract.spec` pointing into it.

| Setting | Decision | Source |
|---|---|---|
| Task / plugin | Gradle `org.openapi.generator`, task `openApiGenerate`, in `rekord-adapter` | A §4 (line 232) |
| Generator | `jaxrs-spec` | A §4 |
| Version | 7.25.0, pinned in the version catalog (`gradle/libs.versions.toml`) | A §4 |
| Options | `interfaceOnly=true`, `useJakartaEe=true`, `returnResponse=false`, `useSwaggerAnnotations=false`, `openApiNullable=false`, `dateLibrary=java8`, `useTags=true` | A §4 |
| Packages | `app.rekord.api`, `app.rekord.api.model` | A §4 |
| Output | `rekord-adapter/build/generated/openapi`, set explicitly as `outputDir` (the plugin default is elsewhere) so the write guard covers it (18 C-23, line 550); `sourceFolder` is `src/gen/java`, and `cleanupOutput` is on | A §4 |
| Input | the hub's bundled `dist/openapi.yaml` at the pinned tag, passed as `-Pcontract.spec` | A §4 |
| Hub smoke job | `smoke/pom.xml` in the hub generates from the same spec with the same generator version, packages and options; `HubProbeParityTest` reads it from the pinned checkout and fails when the two differ | TASK-2.4 |

Generated code is never edited or committed; a wrong shape is fixed in the contract (P4). A red build on regenerated but unimplemented interfaces is the expected start of a contract feature.

`CiContractSpecTest` treats as starting the application every Gradle run that starts a task other than a diagnostic or `clean`, and every image build. It reads these run shapes: prefixes (`sudo`, `env`, `bash`, `time`, ...), the wrappers `timeout`, `nice` and `xvfb-run`, the shell keywords `if`, `then`, `elif`, `else`, `do`, `while`, `until` and `!`, `gradlew.bat`, `#` comments, a Dockerfile `RUN` in shell and in exec form (an exec form reads as its shell form, so `["sh", "-c", "./gradlew build"]` is checked), `docker build` from the repository root, and `docker compose up` or `run`. It exempts diagnostics, `clean`, commands that only mention the wrapper (`chmod +x gradlew`, `echo`, `ls`, `git`, `COPY`), and the setup actions it knows (`actions/checkout`, `actions/setup-java`, `actions/setup-node`, `actions/upload-artifact`, and `gradle/actions/setup-gradle` and `gradle/gradle-build-action` without `with.arguments`). It refuses everything else, so an unread shape cannot pass silently: a command or Dockerfile line that names the wrapper in a shape it cannot classify, an image build from another context, with `bake` or with `docker compose build`, any other `uses:` action, a reusable workflow, and a pin or contract checkout whose `if:` the build steps do not share.
