package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * TASK-2.4 AC #8: the hub's Java smoke job (smoke/pom.xml of the rekord-contract checkout that {@code contract.spec}
 * points at) generates with the same openapi-generator version and the same parameters as wedding-portal's
 * {@code openApiGenerate} task, so wedding-portal never meets a generation failure the hub passed.
 *
 * <p>Both sides are read as structure: the pom as XML, wedding-portal's task from its own {@code openApiGenerate}
 * block only (never the rest of the build script), and the version through the version catalog. A side that cannot be
 * read fails the test, so a moved block or a new construct cannot make the comparison pass on nothing.
 *
 * <p>A setting that reaches the generator from a place the reader does not look would show no difference, so the
 * reader refuses those places instead of skipping them (TASK-2.9). Every refusal names the construct and says the test
 * does not read it. A construct that appears only in a comment is not a construct.
 *
 * <p>Refused on the hub side, in {@code smoke/pom.xml}:
 * <ul>
 *   <li>a {@code <parent>}: Maven merges the parent's plugin settings into the pom;</li>
 *   <li>a {@code <profiles>} section: an active profile merges its plugin settings in;</li>
 *   <li>a {@code <build><pluginManagement>} entry for {@code openapi-generator-maven-plugin}: Maven merges it into the
 *       plugin's configuration;</li>
 *   <li>as before: a plugin-level {@code <configuration>}, no or several generator plugins, no or several
 *       {@code <execution>}s, a parameter with child elements, an option set twice, and a version property the pom
 *       does not define.</li>
 * </ul>
 *
 * <p>Refused on the portal side, in {@code rekord-adapter/build.gradle.kts}:
 * <ul>
 *   <li>the generator task reached outside the {@code openApiGenerate { }} block: a lookup of the task by name
 *       ({@code tasks.named<GenerateTask>("openApiGenerate") { }}, {@code tasks.getByName}, {@code .configure { }}), a
 *       reference to the {@code GenerateTask} type ({@code tasks.withType<GenerateTask>().configureEach { }}), the
 *       accessor {@code tasks.openApiGenerate { }}, and {@code configOptions} or {@code generatorName} anywhere outside
 *       the block, {@code afterEvaluate { }} included;</li>
 *   <li>the generator plugin applied other than through {@code alias(libs.plugins.openapi.generator)}, because the
 *       version compared is the one of that catalog entry: by plugin id ({@code id("org.openapi.generator") version
 *       "..."}, {@code apply(plugin = ...)}), through a {@code buildscript} classpath entry, or not at all;</li>
 *   <li>as before: no or several {@code openApiGenerate { }} blocks, a block without {@code configOptions = mapOf(},
 *       a computed value, and an option set twice.</li>
 * </ul>
 *
 * <p>Not refused: the source-root wiring {@code java.srcDir(tasks.named("openApiGenerate").map { ... })}, which names
 * the task without configuring it.
 */
class HubProbeParityTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final String HUB_PLUGIN = "openapi-generator-maven-plugin";
    private static final String OPENAPI_GENERATOR_PLUGIN_ID = "org.openapi.generator";
    /** Where the spec is read and where the output goes differ by environment and do not change what is generated. */
    private static final List<String> HUB_ENVIRONMENT = List.of("inputSpec", "output");

    private static final List<String> PORTAL_ENVIRONMENT = List.of("inputSpec", "outputDir", "cleanupOutput");

    private static final String BUILD_SCRIPT = "rekord-adapter/build.gradle.kts";
    private static final String HUB_POM_FILE = "smoke/pom.xml";
    private static final String GENERATOR_ALIAS_SOURCE = "alias(libs.plugins.openapi.generator)";
    private static final Pattern GENERATOR_ALIAS =
            Pattern.compile("\\balias\\(\\s*libs\\.plugins\\.openapi\\.generator\\s*\\)");
    /** {@code tasks.named("openApiGenerate").map {}} adds the output as a source root; it names the task, sets nothing. */
    private static final Pattern SOURCE_ROOT_WIRING = Pattern.compile(
            "\\btasks\\s*\\.\\s*named\\s*\\(\\s*\"openApiGenerate\"\\s*\\)\\s*\\.\\s*map\\s*\\{");
    private static final Pattern GENERATOR_SETTING_NAME = Pattern.compile("\\b(?:configOptions|generatorName)\\b");

    /** A construct this reader does not follow: what it looks like in the script, and what to call it in the message. */
    private record Construct(Pattern pattern, String description) {}

    /** Ways to reach the generator task or its settings other than the block, in the order they are looked for. */
    private static final List<Construct> OUTSIDE_THE_BLOCK = List.of(
            new Construct(Pattern.compile("\"openApiGenerate\""), "a lookup of the openApiGenerate task by name"),
            new Construct(Pattern.compile("\\bGenerateTask\\b"), "a reference to the GenerateTask type"),
            new Construct(Pattern.compile("\\bopenApiGenerate\\b"), "the openApiGenerate task accessor used outside its block"),
            new Construct(GENERATOR_SETTING_NAME, "configOptions or generatorName outside the openApiGenerate block"));

    /** Ways to apply the generator plugin that bypass the catalog entry the version is read from. */
    private static final List<Construct> PLUGIN_APPLIED_ELSEWHERE = List.of(
            new Construct(Pattern.compile("\"" + Pattern.quote(OPENAPI_GENERATOR_PLUGIN_ID) + "\""), "its plugin id"),
            new Construct(Pattern.compile("openapi-generator-gradle-plugin"), "a buildscript classpath entry"));

    /** The generation settings of one side: the generator version, its plain parameters and its configOptions. */
    record Settings(String version, Map<String, String> parameters, Map<String, String> configOptions) {}

    @Test
    void the_hub_smoke_probe_and_the_portal_openApiGenerate_task_use_the_same_generator_version_and_options()
            throws IOException {
        // Given
        Path pom = hubPom();
        String script = Files.readString(REPO_ROOT.resolve("rekord-adapter/build.gradle.kts"));
        String catalog = Files.readString(REPO_ROOT.resolve("gradle/libs.versions.toml"));

        // When
        Settings hub = hubSettings(Files.readString(pom));
        Settings portal = portalSettings(script, catalog);

        // Then: the comparison must have something to compare
        assertThat(portal.version()).as("generator version read from the catalog").isNotBlank();
        assertThat(portal.parameters()).as("parameters read from openApiGenerate").containsKeys("generatorName", "apiPackage", "modelPackage");
        assertThat(portal.configOptions()).as("configOptions read from openApiGenerate").isNotEmpty();
        assertThat(hub.configOptions()).as("configOptions read from %s", pom).isNotEmpty();
        assertThat(differences(hub, portal))
                .as("differences between the hub's %s and wedding-portal's openApiGenerate", pom)
                .isEmpty();
    }

    @Test
    void the_hub_probe_is_found_next_to_dist_in_the_checkout_contract_spec_points_at() throws IOException {
        // Given / When
        Path pom = hubPom();

        // Then
        assertThat(pom.getFileName().toString()).isEqualTo("pom.xml");
        assertThat(pom.getParent().getFileName().toString()).isEqualTo("smoke");
        assertThat(pom).isRegularFile();
    }

    @Test
    void settings_that_are_alike_have_no_difference() {
        // Given / When
        Settings hub = hubSettings(HUB_POM);
        Settings portal = portalSettings(PORTAL_SCRIPT, CATALOG);

        // Then
        assertThat(hub.version()).isEqualTo("7.25.0");
        assertThat(portal.version()).isEqualTo("7.25.0");
        assertThat(hub.parameters())
                .containsOnly(
                        Map.entry("generatorName", "jaxrs-spec"),
                        Map.entry("apiPackage", "a.api"),
                        Map.entry("modelPackage", "a.api.model"),
                        Map.entry("generateApiTests", "false"),
                        Map.entry("generateModelTests", "false"));
        assertThat(hub.configOptions()).containsOnly(Map.entry("interfaceOnly", "true"), Map.entry("dateLibrary", "java8"));
        assertThat(portal).isEqualTo(hub);
        assertThat(differences(hub, portal)).isEmpty();
    }

    @Test
    void a_changed_option_value_on_one_side_is_reported_by_name_and_values() {
        // Given
        Settings hub = hubSettings(HUB_POM);
        Settings portal = portalSettings(PORTAL_SCRIPT.replace("\"dateLibrary\" to \"java8\"", "\"dateLibrary\" to \"joda\""), CATALOG);

        // When
        var differences = differences(hub, portal);

        // Then
        assertThat(differences).containsExactly("configOptions.dateLibrary: hub java8, wedding-portal joda");
    }

    @Test
    void an_option_that_only_one_side_sets_is_reported_in_both_directions() {
        // Given
        Settings hub = hubSettings(HUB_POM);
        Settings portalWithMore = portalSettings(
                PORTAL_SCRIPT.replace("\"interfaceOnly\" to \"true\",", "\"interfaceOnly\" to \"true\",\n        \"useTags\" to \"true\","), CATALOG);
        Settings portalWithLess =
                portalSettings(PORTAL_SCRIPT.replace("        \"interfaceOnly\" to \"true\",\n", ""), CATALOG);

        // When / Then
        assertThat(differences(hub, portalWithMore))
                .containsExactly("configOptions.useTags: hub (absent), wedding-portal true");
        assertThat(differences(hub, portalWithLess))
                .containsExactly("configOptions.interfaceOnly: hub true, wedding-portal (absent)");
    }

    @Test
    void a_different_generator_version_generator_name_or_package_is_reported() {
        // Given
        Settings hub = hubSettings(HUB_POM);
        Settings otherVersion = portalSettings(PORTAL_SCRIPT, CATALOG.replace("\"7.25.0\"", "\"7.26.0\""));
        Settings otherGenerator = portalSettings(PORTAL_SCRIPT.replace("\"jaxrs-spec\"", "\"jaxrs-cxf\""), CATALOG);
        Settings otherPackage = portalSettings(PORTAL_SCRIPT.replace("\"a.api.model\"", "\"b.api.model\""), CATALOG);
        Settings otherTests = portalSettings(PORTAL_SCRIPT.replace("generateApiTests = false", "generateApiTests = true"), CATALOG);

        // When / Then
        assertThat(differences(hub, otherVersion)).containsExactly("generator version: hub 7.25.0, wedding-portal 7.26.0");
        assertThat(differences(hub, otherGenerator)).containsExactly("generatorName: hub jaxrs-spec, wedding-portal jaxrs-cxf");
        assertThat(differences(hub, otherPackage)).containsExactly("modelPackage: hub a.api.model, wedding-portal b.api.model");
        assertThat(differences(hub, otherTests)).containsExactly("generateApiTests: hub false, wedding-portal true");
    }

    @Test
    void a_parameter_that_only_one_side_sets_is_reported() {
        // Given
        Settings hub = hubSettings(HUB_POM);
        Settings portal = portalSettings(PORTAL_SCRIPT.replace("    cleanupOutput = true\n", "    cleanupOutput = true\n    skipValidateSpec = true\n"), CATALOG);

        // When
        var differences = differences(hub, portal);

        // Then: input, output and cleanupOutput are environment; anything else changes what is generated
        assertThat(differences).containsExactly("skipValidateSpec: hub (absent), wedding-portal true");
    }

    @Test
    void the_version_follows_the_plugin_entry_of_the_catalog_and_not_a_look_alike() {
        // Given
        String catalog = """
                [versions]
                openapi-generator = "7.25.0"
                other = "1.2.3"

                [libraries]
                lookalike = { module = "x:y", version = "9.9.9" }

                [plugins]
                openapi-generator = { id = "org.openapi.generator", version.ref = "other" }
                """;

        // When / Then
        assertThat(portalSettings(PORTAL_SCRIPT, catalog).version()).isEqualTo("1.2.3");
    }

    @Test
    void a_build_script_without_the_openApiGenerate_block_fails_loudly() {
        // Given: the block moved or was renamed, and the word only survives in a comment
        String moved = "// openApiGenerate {\n"
                + PORTAL_SCRIPT.replace("openApiGenerate {", "generateContract {");

        // When / Then
        assertThatThrownBy(() -> portalSettings(moved, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no `openApiGenerate {` block");
    }

    @Test
    void two_openApiGenerate_blocks_fail_loudly() {
        assertThatThrownBy(() -> portalSettings(PORTAL_SCRIPT + "\n" + PORTAL_SCRIPT, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more than one `openApiGenerate {` block");
    }

    @Test
    void an_openApiGenerate_block_without_configOptions_fails_loudly() {
        // Given
        String without = PORTAL_SCRIPT.replaceAll("(?s)configOptions = mapOf\\(.*?\\n    \\)", "");

        // When / Then
        assertThat(without).doesNotContain("configOptions");
        assertThatThrownBy(() -> portalSettings(without, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no `configOptions = mapOf(`");
    }

    @Test
    void an_option_or_parameter_the_reader_cannot_evaluate_fails_loudly_instead_of_being_dropped() {
        // Given
        String computedOption = PORTAL_SCRIPT.replace("\"dateLibrary\" to \"java8\"", "\"dateLibrary\" to dateLibrary");
        String computedParameter = PORTAL_SCRIPT.replace("apiPackage = \"a.api\"", "apiPackage = rootProject.name");
        String mapParameter = PORTAL_SCRIPT.replace("    cleanupOutput = true\n", "    typeMappings = mapOf(\"x\" to \"y\")\n");

        // When / Then
        assertThatThrownBy(() -> portalSettings(computedOption, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot read the configOptions entry");
        assertThatThrownBy(() -> portalSettings(computedParameter, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot read the value of apiPackage");
        assertThatThrownBy(() -> portalSettings(mapParameter, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot read the value of typeMappings");
    }

    @Test
    void an_option_set_twice_fails_loudly() {
        // Given
        String twice = PORTAL_SCRIPT.replace("\"dateLibrary\" to \"java8\"", "\"dateLibrary\" to \"java8\", \"dateLibrary\" to \"joda\"");

        // When / Then
        assertThatThrownBy(() -> portalSettings(twice, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sets dateLibrary twice");
    }

    @Test
    void comments_and_braces_inside_strings_do_not_end_the_block_early() {
        // Given
        String script = PORTAL_SCRIPT
                .replace("    cleanupOutput = true\n", "    cleanupOutput = true // not } the end\n    /* nor } this */\n")
                .replace("\"interfaceOnly\" to \"true\",", "\"interfaceOnly\" to \"true\", // a } comment");

        // When / Then
        assertThat(differences(hubSettings(HUB_POM), portalSettings(script, CATALOG))).isEmpty();
    }

    @Test
    void a_catalog_without_the_generator_plugin_or_its_version_fails_loudly() {
        assertThatThrownBy(() -> portalSettings(PORTAL_SCRIPT, "[versions]\nx = \"1\"\n[plugins]\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one plugin with id org.openapi.generator");
        assertThatThrownBy(() -> portalSettings(
                        PORTAL_SCRIPT, "[versions]\n[plugins]\ng = { id = \"org.openapi.generator\", version.ref = \"nope\" }\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no version named nope");
    }

    @Test
    void a_pom_the_reader_cannot_follow_fails_loudly() {
        // Given
        String twoExecutions = HUB_POM.replace("</executions>", "<execution><configuration/></execution></executions>");
        String noPlugin = HUB_POM.replace(HUB_PLUGIN, "some-other-plugin");
        String undefinedProperty = HUB_POM.replace("<openapi-generator.version>7.25.0</openapi-generator.version>", "");
        String pluginLevelConfiguration = HUB_POM.replace("<executions>", "<configuration><generatorName>x</generatorName></configuration><executions>");
        String complexParameter = HUB_POM.replace("<generatorName>", "<typeMappings><typeMapping>a=b</typeMapping></typeMappings><generatorName>");
        String optionTwice = HUB_POM.replace("<dateLibrary>java8</dateLibrary>", "<dateLibrary>java8</dateLibrary><dateLibrary>joda</dateLibrary>");
        // Maven merges these into the plugin's configuration, and this reader does not follow them (TASK-2.9 AC #1)
        String withParent = HUB_POM.replace(
                "<properties>",
                "<parent><groupId>g</groupId><artifactId>p</artifactId><version>1</version></parent><properties>");
        String withProfiles = HUB_POM.replace(
                "</build>",
                "</build><profiles><profile><id>legacy</id><activation><activeByDefault>true</activeByDefault></activation>"
                        + "<build><plugins><plugin><artifactId>" + HUB_PLUGIN + "</artifactId><executions><execution>"
                        + "<configuration><configOptions><dateLibrary>legacy</dateLibrary></configOptions></configuration>"
                        + "</execution></executions></plugin></plugins></build></profile></profiles>");
        String withPluginManagement = HUB_POM.replace(
                "<build>",
                "<build><pluginManagement><plugins><plugin><groupId>org.openapitools</groupId><artifactId>" + HUB_PLUGIN
                        + "</artifactId><configuration><configOptions><useTags>false</useTags></configOptions></configuration>"
                        + "</plugin></plugins></pluginManagement>");

        // When / Then
        assertThatThrownBy(() -> hubSettings(twoExecutions))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one <execution>");
        assertThatThrownBy(() -> hubSettings(noPlugin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one " + HUB_PLUGIN);
        assertThatThrownBy(() -> hubSettings(undefinedProperty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("property openapi-generator.version");
        assertThatThrownBy(() -> hubSettings(pluginLevelConfiguration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plugin-level <configuration>");
        assertThatThrownBy(() -> hubSettings(complexParameter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot read <typeMappings>");
        assertThatThrownBy(() -> hubSettings(optionTwice))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sets dateLibrary twice");
        // one report for all three, so a reader that misses one still shows the others
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> hubSettings(withParent))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("<parent>")
                    .hasMessageContaining("does not read");
            softly.assertThatThrownBy(() -> hubSettings(withProfiles))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("<profiles>")
                    .hasMessageContaining("does not read");
            softly.assertThatThrownBy(() -> hubSettings(withPluginManagement))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("<pluginManagement>")
                    .hasMessageContaining(HUB_PLUGIN)
                    .hasMessageContaining("does not read");
        });
    }

    @Test
    void a_pom_construct_that_does_not_reach_the_generator_or_sits_in_a_comment_is_not_refused() {
        // Given: pluginManagement for another plugin does not touch the generator; a comment is not an element
        String otherPluginManaged = HUB_POM.replace(
                "<build>",
                "<build><pluginManagement><plugins><plugin><artifactId>maven-compiler-plugin</artifactId>"
                        + "<configuration><release>25</release></configuration></plugin></plugins></pluginManagement>");
        String onlyInComments = HUB_POM.replace(
                "<properties>",
                "<!-- <parent><artifactId>p</artifactId></parent> <profiles><profile/></profiles> "
                        + "<pluginManagement>" + HUB_PLUGIN + "</pluginManagement> --><properties>");

        // When / Then
        assertThat(hubSettings(otherPluginManaged)).isEqualTo(hubSettings(HUB_POM));
        assertThat(hubSettings(onlyInComments)).isEqualTo(hubSettings(HUB_POM));
    }

    // ---- the portal side: what the openApiGenerate reader does not read (TASK-2.9 AC #2 to #4) ---------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatorSettingsOutsideTheBlock")
    void a_build_script_that_configures_the_generator_outside_the_openApiGenerate_block_fails_loudly(
            String form, String construct, String named) {
        // Given
        String script = PORTAL_SCRIPT + "\n" + construct;

        // When / Then
        assertThatThrownBy(() -> portalSettings(script, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(named)
                .hasMessageContaining("does not read");
    }

    static Stream<Arguments> generatorSettingsOutsideTheBlock() {
        return Stream.of(
                Arguments.of("tasks.named<GenerateTask>(\"openApiGenerate\") { ... }", """
                        tasks.named<GenerateTask>("openApiGenerate") {
                            configOptions.put("useTags", "false")
                        }
                        """, "tasks.named<GenerateTask>(\"openApiGenerate\")"),
                Arguments.of("tasks.withType<GenerateTask>().configureEach { ... }", """
                        tasks.withType<GenerateTask>().configureEach {
                            configOptions.put("useTags", "false")
                        }
                        """, "tasks.withType<GenerateTask>()"),
                Arguments.of("tasks.openApiGenerate { ... }", """
                        tasks.openApiGenerate {
                            configOptions.put("useTags", "false")
                        }
                        """, "tasks.openApiGenerate"),
                Arguments.of("configOptions inside afterEvaluate", """
                        afterEvaluate {
                            extensions.getByType<OpenApiGeneratorGenerateExtension>().configOptions.put("useTags", "false")
                        }
                        """, "afterEvaluate"),
                Arguments.of("generatorName inside afterEvaluate", """
                        project.afterEvaluate {
                            extensions.getByType<OpenApiGeneratorGenerateExtension>().generatorName.set("jaxrs-cxf")
                        }
                        """, "afterEvaluate"),
                Arguments.of("tasks.named(\"openApiGenerate\") { ... } without the type", """
                        tasks.named("openApiGenerate") {
                            doFirst { println("x") }
                        }
                        """, "tasks.named(\"openApiGenerate\")"),
                Arguments.of("tasks.named(\"openApiGenerate\").configure { ... }", """
                        tasks.named("openApiGenerate").configure {
                            doFirst { println("x") }
                        }
                        """, "tasks.named(\"openApiGenerate\").configure"),
                Arguments.of("tasks.getByName<GenerateTask>(\"openApiGenerate\") { ... }", """
                        tasks.getByName<GenerateTask>("openApiGenerate") {
                            doFirst { println("x") }
                        }
                        """, "tasks.getByName<GenerateTask>(\"openApiGenerate\")"),
                Arguments.of("the GenerateTask import", """
                        import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
                        """, "import org.openapitools.generator.gradle.plugin.tasks.GenerateTask"),
                Arguments.of("configOptions outside the block", """
                        extensions.getByType<OpenApiGeneratorGenerateExtension>().configOptions.put("useTags", "false")
                        """, "extensions.getByType<OpenApiGeneratorGenerateExtension>().configOptions"));
    }

    @Test
    void the_source_root_wiring_that_names_the_task_without_configuring_it_is_not_refused() {
        // Given: the wiring as rekord-adapter/build.gradle.kts has it
        String wiring = "java.srcDir(tasks.named(\"openApiGenerate\").map { layout.buildDirectory.dir(\"generated/openapi/src/gen/java\") })";
        String script = PORTAL_SCRIPT.replace("java.srcDir(\"generated\")", wiring);

        // When
        Settings portal = portalSettings(script, CATALOG);

        // Then
        assertThat(script).contains(wiring);
        assertThat(differences(hubSettings(HUB_POM), portal)).isEmpty();
    }

    @Test
    void a_construct_that_appears_only_in_a_comment_is_not_refused() {
        // Given
        String script = PORTAL_SCRIPT
                + """

                // tasks.named<GenerateTask>("openApiGenerate") { configOptions.put("useTags", "false") }
                // id("org.openapi.generator") version "7.26.0"
                /* tasks.withType<GenerateTask>().configureEach { generatorName.set("jaxrs-cxf") }
                   tasks.openApiGenerate { }
                   afterEvaluate { configOptions.put("useTags", "false") }
                   buildscript { dependencies { classpath("org.openapitools:openapi-generator-gradle-plugin:7.26.0") } } */
                """;

        // When / Then
        assertThat(differences(hubSettings(HUB_POM), portalSettings(script, CATALOG))).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pluginsNotAppliedByTheCatalogAlias")
    void a_generator_plugin_not_applied_through_the_catalog_alias_fails_loudly(String how, String script, String named) {
        // When / Then
        assertThatThrownBy(() -> portalSettings(script, CATALOG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(named)
                .hasMessageContaining("alias(libs.plugins.openapi.generator)");
    }

    static Stream<Arguments> pluginsNotAppliedByTheCatalogAlias() {
        String alias = "alias(libs.plugins.openapi.generator)";
        return Stream.of(
                Arguments.of(
                        "id(...) version ...",
                        PORTAL_SCRIPT.replace(alias, "id(\"org.openapi.generator\") version \"7.26.0\""),
                        "id(\"org.openapi.generator\") version \"7.26.0\""),
                Arguments.of(
                        "id(...) without a version",
                        PORTAL_SCRIPT.replace(alias, "id(\"org.openapi.generator\")"),
                        "id(\"org.openapi.generator\")"),
                Arguments.of(
                        "apply(plugin = ...)",
                        PORTAL_SCRIPT.replace(alias, "") + "\napply(plugin = \"org.openapi.generator\")\n",
                        "apply(plugin = \"org.openapi.generator\")"),
                Arguments.of(
                        "a buildscript classpath entry",
                        """
                        buildscript {
                            dependencies {
                                classpath("org.openapitools:openapi-generator-gradle-plugin:7.26.0")
                            }
                        }
                        """ + PORTAL_SCRIPT,
                        "buildscript classpath"),
                Arguments.of(
                        "no application at all",
                        PORTAL_SCRIPT.replace("    " + alias + "\n", ""),
                        "does not apply org.openapi.generator"));
    }

    // ---- the dependencies: smoke/pom.xml <dependencies> against the external api(...) of rekord-adapter (TASK-2.10) ----

    @Test
    void the_hub_smoke_probe_and_rekord_adapter_declare_the_same_dependencies() throws IOException {
        // Given
        Path pom = hubPom();
        String script = Files.readString(REPO_ROOT.resolve(BUILD_SCRIPT));

        // When
        Set<String> hub = hubDependencies(Files.readString(pom));
        Set<String> portal = portalDependencies(script);

        // Then: the comparison must have something to compare
        assertThat(hub).as("<dependencies> read from %s", pom).isNotEmpty();
        assertThat(portal).as("external api(...) coordinates read from %s", BUILD_SCRIPT).isNotEmpty();
        assertThat(dependencyDifferences(hub, portal))
                .as("differences between the <dependencies> of the hub's %s and the api(...) of wedding-portal's %s", pom, BUILD_SCRIPT)
                .isEmpty();
    }

    @Test
    void dependencies_that_are_alike_have_no_difference_whatever_else_the_script_declares() {
        // Given / When
        Set<String> hub = hubDependencies(HUB_DEPENDENCIES_POM);
        Set<String> portal = portalDependencies(PORTAL_DEPENDENCIES_SCRIPT);

        // Then: versions, project(...), compileOnly, implementation and the test configurations are not compared
        assertThat(hub).containsExactlyInAnyOrder(
                "jakarta.ws.rs:jakarta.ws.rs-api",
                "jakarta.validation:jakarta.validation-api",
                "jakarta.annotation:jakarta.annotation-api",
                "com.fasterxml.jackson.core:jackson-annotations");
        assertThat(portal).isEqualTo(hub);
        assertThat(dependencyDifferences(hub, portal)).isEmpty();
    }

    @Test
    void a_dependency_only_the_hub_has_is_reported_by_coordinate_and_file() {
        // Given: the probe gained jackson-databind and rekord-adapter did not
        String pom = HUB_DEPENDENCIES_POM.replace("</dependencies>", DATABIND + "</dependencies>");

        // When
        var differences = dependencyDifferences(hubDependencies(pom), portalDependencies(PORTAL_DEPENDENCIES_SCRIPT));

        // Then
        assertThat(pom).isNotEqualTo(HUB_DEPENDENCIES_POM);
        assertThat(differences)
                .containsExactly("com.fasterxml.jackson.core:jackson-databind: in smoke/pom.xml <dependencies>, "
                        + "not in rekord-adapter/build.gradle.kts api(...)");
    }

    @Test
    void an_api_coordinate_removed_from_or_added_to_rekord_adapter_is_reported_by_coordinate_and_file() {
        // Given
        String removed = PORTAL_DEPENDENCIES_SCRIPT.replace("    api(\"jakarta.annotation:jakarta.annotation-api\")\n", "");
        String added = withDependencyLine(PORTAL_DEPENDENCIES_SCRIPT, "api(\"com.fasterxml.jackson.core:jackson-databind\")");
        Set<String> hub = hubDependencies(HUB_DEPENDENCIES_POM);

        // When / Then
        assertThat(removed).isNotEqualTo(PORTAL_DEPENDENCIES_SCRIPT);
        assertThat(dependencyDifferences(hub, portalDependencies(removed)))
                .containsExactly("jakarta.annotation:jakarta.annotation-api: in smoke/pom.xml <dependencies>, "
                        + "not in rekord-adapter/build.gradle.kts api(...)");
        assertThat(dependencyDifferences(hub, portalDependencies(added)))
                .containsExactly("com.fasterxml.jackson.core:jackson-databind: in rekord-adapter/build.gradle.kts api(...), "
                        + "not in smoke/pom.xml <dependencies>");
    }

    @Test
    void a_copy_of_each_real_file_with_a_dependency_added_or_removed_is_reported() throws IOException {
        // Given: the files themselves, so a change of their shape shows here and not only in the fixtures
        String pom = Files.readString(hubPom());
        String script = Files.readString(REPO_ROOT.resolve(BUILD_SCRIPT));
        Matcher externalApi = Pattern.compile("(?m)^[ \\t]*api\\(\"([^\":]+:[^\":]+)\"\\)[ \\t]*\\R").matcher(script);
        assertThat(externalApi.find()).as("an api(\"group:artifact\") line in %s", BUILD_SCRIPT).isTrue();
        String removed = externalApi.group(1);
        String withDatabind = pom.replace("</dependencies>", DATABIND + "</dependencies>");
        String withoutOne = script.substring(0, externalApi.start()) + script.substring(externalApi.end());

        // When / Then
        assertThat(withDatabind).isNotEqualTo(pom);
        assertThat(dependencyDifferences(hubDependencies(withDatabind), portalDependencies(script)))
                .containsExactly("com.fasterxml.jackson.core:jackson-databind: in smoke/pom.xml <dependencies>, "
                        + "not in rekord-adapter/build.gradle.kts api(...)");
        assertThat(dependencyDifferences(hubDependencies(pom), portalDependencies(withoutOne)))
                .containsExactly(removed + ": in smoke/pom.xml <dependencies>, not in rekord-adapter/build.gradle.kts api(...)");
    }

    @Test
    void a_dependency_that_both_sides_declare_differently_is_compared_by_group_and_artifact_only() {
        // Given: a version on the portal side, another version and the optional flag on the hub side
        String pom = HUB_DEPENDENCIES_POM.replace("<version>2.22</version>", "<version>9.9</version><optional>true</optional>");
        String script = PORTAL_DEPENDENCIES_SCRIPT.replace(
                "api(\"com.fasterxml.jackson.core:jackson-annotations\")", "api(\"com.fasterxml.jackson.core:jackson-annotations:2.22\")");

        // When / Then
        assertThat(pom).isNotEqualTo(HUB_DEPENDENCIES_POM);
        assertThat(script).isNotEqualTo(PORTAL_DEPENDENCIES_SCRIPT);
        assertThat(dependencyDifferences(hubDependencies(pom), portalDependencies(script))).isEmpty();
    }

    @Test
    void a_pom_construct_that_does_not_put_a_dependency_on_the_probe_is_not_refused() {
        // Given: a plugin's own <dependencies>, a comment, an explicit compile scope and jar type are not a difference
        String pluginDependencies = HUB_DEPENDENCIES_POM.replace("</plugin>", "<dependencies>" + DATABIND + "</dependencies></plugin>");
        String inComments = HUB_DEPENDENCIES_POM.replace(
                "<dependencies>",
                "<!-- <parent><artifactId>p</artifactId></parent> <profiles/> <dependencyManagement/> --><dependencies>");
        String explicit = HUB_DEPENDENCIES_POM.replace(
                "<version>4.0.0</version>", "<version>4.0.0</version><scope>compile</scope><type>jar</type>");

        // When / Then
        Set<String> expected = hubDependencies(HUB_DEPENDENCIES_POM);
        assertThat(pluginDependencies).isNotEqualTo(HUB_DEPENDENCIES_POM);
        assertThat(hubDependencies(pluginDependencies)).isEqualTo(expected);
        assertThat(hubDependencies(inComments)).isEqualTo(expected);
        assertThat(hubDependencies(explicit)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pomsWhoseDependenciesTheReaderCannotFollow")
    void a_pom_whose_dependencies_the_reader_cannot_follow_fails_loudly(String form, String pom, String named) {
        // When / Then: no empty set to pass on, and the message names the file
        assertThatThrownBy(() -> hubDependencies(pom))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(HUB_POM_FILE)
                .hasMessageContaining(named);
    }

    static Stream<Arguments> pomsWhoseDependenciesTheReaderCannotFollow() {
        String jakartaWsRsAgain = "<dependency><groupId>jakarta.ws.rs</groupId><artifactId>jakarta.ws.rs-api</artifactId></dependency>";
        return Stream.of(
                Arguments.of("no <dependencies> block", HUB_POM, "has no <dependencies> block"),
                Arguments.of(
                        "<dependencies> only inside the plugin",
                        HUB_POM.replace("<executions>", "<dependencies>" + DATABIND + "</dependencies><executions>"),
                        "has no <dependencies> block"),
                Arguments.of(
                        "an empty <dependencies/>",
                        HUB_DEPENDENCIES_POM.replaceAll("(?s)<dependencies>.*?</dependencies>", "<dependencies/>"),
                        "<dependencies> block without a <dependency>"),
                Arguments.of(
                        "a <dependencies> holding only a comment",
                        HUB_DEPENDENCIES_POM.replaceAll("(?s)<dependencies>.*?</dependencies>", "<dependencies><!-- none --></dependencies>"),
                        "<dependencies> block without a <dependency>"),
                Arguments.of(
                        "two <dependencies> blocks",
                        HUB_DEPENDENCIES_POM.replace("</dependencies>", "</dependencies><dependencies>" + DATABIND + "</dependencies>"),
                        "more than one <dependencies> block"),
                Arguments.of(
                        "a <parent>",
                        HUB_DEPENDENCIES_POM.replace(
                                "<dependencies>",
                                "<parent><groupId>g</groupId><artifactId>p</artifactId><version>1</version></parent><dependencies>"),
                        "<parent>, which this test does not read"),
                Arguments.of(
                        "a <profiles> section",
                        HUB_DEPENDENCIES_POM.replace(
                                "</project>",
                                "<profiles><profile><id>more</id><dependencies>" + DATABIND + "</dependencies></profile></profiles></project>"),
                        "<profiles> section, which this test does not read"),
                Arguments.of(
                        "a <dependencyManagement> section",
                        HUB_DEPENDENCIES_POM.replace(
                                "<dependencies>",
                                "<dependencyManagement><dependencies>" + DATABIND + "</dependencies></dependencyManagement><dependencies>"),
                        "<dependencyManagement>, which this test does not read"),
                Arguments.of(
                        "<scope>test</scope>",
                        HUB_DEPENDENCIES_POM.replace("<version>4.0.0</version>", "<version>4.0.0</version><scope>test</scope>"),
                        "cannot read <scope>test</scope> of jakarta.ws.rs:jakarta.ws.rs-api"),
                Arguments.of(
                        "<type>pom</type>",
                        HUB_DEPENDENCIES_POM.replace("<version>3.1.1</version>", "<version>3.1.1</version><type>pom</type>"),
                        "cannot read <type>pom</type> of jakarta.validation:jakarta.validation-api"),
                Arguments.of(
                        "a <classifier>",
                        HUB_DEPENDENCIES_POM.replace("<version>3.0.0</version>", "<version>3.0.0</version><classifier>x</classifier>"),
                        "cannot read <classifier> of jakarta.annotation:jakarta.annotation-api"),
                Arguments.of(
                        "<exclusions>",
                        HUB_DEPENDENCIES_POM.replace(
                                "<version>2.22</version>",
                                "<version>2.22</version><exclusions><exclusion><groupId>x</groupId><artifactId>y</artifactId></exclusion></exclusions>"),
                        "cannot read <exclusions> of com.fasterxml.jackson.core:jackson-annotations"),
                Arguments.of(
                        "a property placeholder in the coordinate",
                        HUB_DEPENDENCIES_POM.replace(
                                "<artifactId>jackson-annotations</artifactId>", "<artifactId>${jackson.artifact}</artifactId>"),
                        "cannot read the coordinate com.fasterxml.jackson.core:${jackson.artifact}, a property placeholder"),
                Arguments.of(
                        "a dependency without an <artifactId>",
                        HUB_DEPENDENCIES_POM.replace("<artifactId>jakarta.ws.rs-api</artifactId>", ""),
                        "expected exactly one <artifactId> in <dependency>"),
                Arguments.of(
                        "a dependency declared twice",
                        HUB_DEPENDENCIES_POM.replace("</dependencies>", jakartaWsRsAgain + "</dependencies>"),
                        "declares jakarta.ws.rs:jakarta.ws.rs-api twice"),
                Arguments.of("XML that is not XML", "<project>", "is not readable XML"));
    }

    @Test
    void a_build_script_with_the_dependencies_block_missing_or_repeated_fails_loudly() {
        // Given: the block moved, only a comment names it, or a second one appeared
        String none = "plugins {\n    `java-library`\n}\n";
        String onlyInComment = "// dependencies {\n//     api(\"x:y\")\n// }\n" + none;
        String twice = PORTAL_DEPENDENCIES_SCRIPT + "\ndependencies {\n    api(\"x:y\")\n}\n";

        // When / Then
        assertThatThrownBy(() -> portalDependencies(none))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BUILD_SCRIPT)
                .hasMessageContaining("no `dependencies {` block");
        assertThatThrownBy(() -> portalDependencies(onlyInComment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no `dependencies {` block");
        assertThatThrownBy(() -> portalDependencies(twice))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BUILD_SCRIPT)
                .hasMessageContaining("more than one `dependencies {` block");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dependenciesInAnotherForm")
    void a_build_script_with_dependencies_in_another_form_fails_loudly(String form, String construct, String named) {
        // Given: the one top-level block is there, and a second way in is added next to it
        String script = PORTAL_DEPENDENCIES_SCRIPT + "\n" + construct;

        // When / Then
        assertThatThrownBy(() -> portalDependencies(script))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BUILD_SCRIPT)
                .hasMessageContaining("in another form")
                .hasMessageContaining(named)
                .hasMessageContaining("does not read");
    }

    static Stream<Arguments> dependenciesInAnotherForm() {
        return Stream.of(
                Arguments.of("subprojects { dependencies { ... } }", """
                        subprojects {
                            dependencies {
                                api("com.fasterxml.jackson.core:jackson-databind")
                            }
                        }
                        """, "dependencies {"),
                Arguments.of("afterEvaluate { dependencies { ... } }", """
                        afterEvaluate {
                            dependencies {
                                api("com.fasterxml.jackson.core:jackson-databind")
                            }
                        }
                        """, "dependencies {"),
                Arguments.of("a buildscript dependencies block", """
                        buildscript {
                            dependencies {
                                classpath("org.example:plugin:1")
                            }
                        }
                        """, "dependencies {"),
                Arguments.of(
                        "project.dependencies.add(...)",
                        "project.dependencies.add(\"api\", \"com.fasterxml.jackson.core:jackson-databind\")\n",
                        "project.dependencies.add(\"api\", \"com.fasterxml.jackson.core:jackson-databind\")"),
                Arguments.of(
                        "configurations.api.get().dependencies",
                        "configurations.api.get().dependencies.add(project.dependencies.create(\"a:b\"))\n",
                        "configurations.api.get().dependencies.add(project.dependencies.create(\"a:b\"))"),
                Arguments.of(
                        "configurations.api { withDependencies { ... } }",
                        "configurations.api { withDependencies { add(project.dependencies.create(\"a:b\")) } }\n",
                        "configurations.api { withDependencies {"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apiArgumentsTheReaderCannotFollow")
    void an_api_argument_the_reader_cannot_follow_fails_loudly(String form, String line, String named, String says) {
        // Given
        String script = withDependencyLine(PORTAL_DEPENDENCIES_SCRIPT, line);

        // When / Then
        assertThat(script).contains(line);
        assertThatThrownBy(() -> portalDependencies(script))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BUILD_SCRIPT)
                .hasMessageContaining(named)
                .hasMessageContaining(says);
    }

    static Stream<Arguments> apiArgumentsTheReaderCannotFollow() {
        String notALiteral = "not a string literal or project(...)";
        String notACoordinate = "not group:artifact or group:artifact:version";
        return Stream.of(
                Arguments.of(
                        "a version catalog accessor",
                        "api(libs.jackson.databind)",
                        "`libs.jackson.databind`",
                        "a version catalog accessor, which this test does not resolve"),
                Arguments.of("a variable", "api(jacksonAnnotations)", "`jacksonAnnotations`", notALiteral),
                Arguments.of(
                        "a string template",
                        "api(\"com.fasterxml.jackson.core:jackson-annotations:$jackson\")",
                        "\"com.fasterxml.jackson.core:jackson-annotations:$jackson\"",
                        "a string template"),
                Arguments.of(
                        "a concatenation",
                        "api(\"com.fasterxml.jackson.core:jackson-annotations:\" + jackson)",
                        "\"com.fasterxml.jackson.core:jackson-annotations:\" + jackson",
                        notALiteral),
                Arguments.of("a platform", "api(platform(libs.jackson.bom))", "`platform(libs.jackson.bom)`", notALiteral),
                Arguments.of(
                        "map notation",
                        "api(group = \"com.fasterxml.jackson.core\", name = \"jackson-databind\")",
                        "`group = \"com.fasterxml.jackson.core\"`",
                        notALiteral),
                Arguments.of(
                        "a classifier",
                        "api(\"com.fasterxml.jackson.core:jackson-annotations:2.22:sources\")",
                        "\"com.fasterxml.jackson.core:jackson-annotations:2.22:sources\"",
                        notACoordinate),
                Arguments.of(
                        "an artifact type",
                        "api(\"com.fasterxml.jackson.core:jackson-annotations:2.22@jar\")",
                        "\"com.fasterxml.jackson.core:jackson-annotations:2.22@jar\"",
                        notACoordinate),
                Arguments.of("a lone name", "api(\"jackson-annotations\")", "\"jackson-annotations\"", notACoordinate),
                Arguments.of(
                        "a configuration block",
                        "api(\"com.fasterxml.jackson.core:jackson-databind\") { exclude(group = \"x\") }",
                        "api(\"com.fasterxml.jackson.core:jackson-databind\")",
                        "with a configuration block"),
                Arguments.of(
                        "a coordinate declared twice",
                        "api(\"jakarta.ws.rs:jakarta.ws.rs-api\")",
                        "jakarta.ws.rs:jakarta.ws.rs-api",
                        "twice"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statementsTheReaderDoesNotKnow")
    void a_statement_or_configuration_the_reader_does_not_know_fails_loudly(String form, String line, String says) {
        // Given
        String script = withDependencyLine(PORTAL_DEPENDENCIES_SCRIPT, line);

        // When / Then
        assertThatThrownBy(() -> portalDependencies(script))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BUILD_SCRIPT)
                .hasMessageContaining(says);
    }

    static Stream<Arguments> statementsTheReaderDoesNotKnow() {
        String statement = "cannot read this statement of the dependencies block";
        return Stream.of(
                Arguments.of(
                        "add(\"api\", ...)",
                        "add(\"api\", \"com.fasterxml.jackson.core:jackson-databind\")",
                        "`add(...)`, a configuration this test does not know"),
                Arguments.of(
                        "compileOnlyApi(...)",
                        "compileOnlyApi(\"com.fasterxml.jackson.core:jackson-databind\")",
                        "`compileOnlyApi(...)`, a configuration this test does not know"),
                Arguments.of(
                        "a conditional",
                        "if (withDatabind) api(\"com.fasterxml.jackson.core:jackson-databind\")",
                        "`if(...)`, a configuration this test does not know"),
                Arguments.of("a string as the configuration", "\"api\"(\"com.fasterxml.jackson.core:jackson-databind\")", statement),
                Arguments.of("the Groovy form", "api \"com.fasterxml.jackson.core:jackson-databind\"", statement),
                Arguments.of("a constraints block", "constraints { api(\"x:y:1\") }", statement),
                Arguments.of("a variable declaration", "val extra = \"com.fasterxml.jackson.core:jackson-databind\"", statement),
                Arguments.of("a call chained onto the dependency", "api(\"x:y\").also { }", statement));
    }

    @Test
    void the_api_forms_the_reader_follows_are_read_and_everything_else_in_the_block_is_left_alone() {
        // Given: calls over several lines, several coordinates in one call, a version, project(...), the other configurations
        String block = """
                dependencies {
                    api(
                        "g1:a1",
                        "g2:a2:1.0",
                    )
                    api("g3:a3", project(":p"))
                    api(project(path = ":q", configuration = "c"))
                    // api("hidden:one")
                    /* api("hidden:two") */
                    implementation("x:y:1") { exclude(group = "q") }
                    implementation(platform(libs.some.bom))
                    compileOnly("hidden:three")
                    testImplementation(libs.assertj.core)
                    runtimeOnly("hidden:four")
                    annotationProcessor("hidden:five")
                    api("g4:a4"); api("g5:a5")
                }
                val note = "see dependencies { api(\\"hidden:six\\") }"
                """;

        // When / Then
        assertThat(portalDependencies(block)).containsExactlyInAnyOrder("g1:a1", "g2:a2", "g3:a3", "g4:a4", "g5:a5");
    }

    @Test
    void a_dependency_construct_that_appears_only_in_a_comment_or_a_string_is_not_refused() {
        // Given
        String script = PORTAL_DEPENDENCIES_SCRIPT
                + """

                // subprojects { dependencies { api("x:y") } }
                /* project.dependencies.add("api", "x:y")
                   configurations.api.get().dependencies */
                val note = "dependencies { api(\\"x:y\\") }"
                """;

        // When / Then
        assertThat(portalDependencies(script)).isEqualTo(hubDependencies(HUB_DEPENDENCIES_POM));
    }

    // ---- the files under test -------------------------------------------------------------------------------------

    /** The hub's smoke job: {@code smoke/pom.xml} next to {@code dist/} in the checkout {@code contract.spec} is in. */
    private static Path hubPom() {
        String spec = System.getProperty("contract.spec");
        assertThat(spec).as("contract.spec system property").isNotBlank();
        Path bundle = Path.of(spec).toAbsolutePath().normalize();
        assertThat(bundle.getFileName().toString()).as("contract.spec file name").isEqualTo("openapi.yaml");
        assertThat(bundle.getParent().getFileName().toString()).as("contract.spec directory name").isEqualTo("dist");
        Path pom = bundle.getParent().getParent().resolve("smoke").resolve("pom.xml");
        assertThat(pom)
                .as("the hub's smoke job, read from the rekord-contract checkout of contract.spec (%s)", spec)
                .isRegularFile();
        return pom;
    }

    // ---- the hub side: smoke/pom.xml ------------------------------------------------------------------------------

    static Settings hubSettings(String pomXml) {
        Element project = parse(pomXml);
        refuseWhatMavenMergesIntoThePlugin(project);
        Map<String, String> properties = new HashMap<>();
        for (Element property : children(single(project, "properties", "<project>"), null)) {
            properties.put(property.getLocalName(), text(property));
        }
        Element plugins = single(single(project, "build", "<project>"), "plugins", "<build>");
        List<Element> generators = children(plugins, "plugin").stream()
                .filter(plugin -> HUB_PLUGIN.equals(text(single(plugin, "artifactId", "<plugin>"))))
                .toList();
        if (generators.size() != 1) {
            throw unreadable("smoke/pom.xml must hold exactly one " + HUB_PLUGIN + ", found " + generators.size());
        }
        Element plugin = generators.get(0);
        if (!children(plugin, "configuration").isEmpty()) {
            throw unreadable("smoke/pom.xml has a plugin-level <configuration>; keep the settings in the one <execution>");
        }
        String version = resolve(text(single(plugin, "version", "<plugin>")), properties);
        List<Element> executions = children(single(plugin, "executions", "<plugin>"), "execution");
        if (executions.size() != 1) {
            throw unreadable("smoke/pom.xml must hold exactly one <execution>, found " + executions.size());
        }
        Element configuration = single(executions.get(0), "configuration", "<execution>");

        Map<String, String> parameters = new TreeMap<>();
        for (Element parameter : children(configuration, null)) {
            String name = parameter.getLocalName();
            if ("configOptions".equals(name) || HUB_ENVIRONMENT.contains(name)) {
                continue;
            }
            if (!children(parameter, null).isEmpty()) {
                throw unreadable("smoke/pom.xml: cannot read <" + name + ">, it has child elements; extend this test");
            }
            put(parameters, name, text(parameter));
        }
        Map<String, String> configOptions = new TreeMap<>();
        for (Element option : children(single(configuration, "configOptions", "<configuration>"), null)) {
            if (!children(option, null).isEmpty()) {
                throw unreadable("smoke/pom.xml: cannot read <" + option.getLocalName() + ">, it has child elements");
            }
            put(configOptions, option.getLocalName(), text(option));
        }
        return new Settings(version, parameters, configOptions);
    }

    /**
     * Maven merges the configuration of the same plugin from a parent POM, from an active profile and from
     * {@code build/pluginManagement} into the pom's own; this reader looks at the pom's own plugin entry only.
     */
    private static void refuseWhatMavenMergesIntoThePlugin(Element project) {
        if (!children(project, "parent").isEmpty()) {
            throw unreadable("smoke/pom.xml has a <parent>, which this test does not read; Maven merges the parent's "
                    + HUB_PLUGIN + " settings into the pom, so keep every generator setting in the pom itself");
        }
        if (!children(project, "profiles").isEmpty()) {
            throw unreadable("smoke/pom.xml has a <profiles> section, which this test does not read; an active profile "
                    + "merges its " + HUB_PLUGIN + " settings into the pom, so keep every generator setting out of profiles");
        }
        for (Element build : children(project, "build")) {
            for (Element management : children(build, "pluginManagement")) {
                for (Element plugins : children(management, "plugins")) {
                    for (Element plugin : children(plugins, "plugin")) {
                        if (children(plugin, "artifactId").stream().anyMatch(id -> HUB_PLUGIN.equals(text(id)))) {
                            throw unreadable("smoke/pom.xml has a <pluginManagement> entry for " + HUB_PLUGIN
                                    + ", which this test does not read; Maven merges it into the plugin's configuration, "
                                    + "so keep the settings in the plugin's one <execution>");
                        }
                    }
                }
            }
        }
    }

    private static Element parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("smoke/pom.xml is not readable XML: " + e.getMessage(), e);
        }
    }

    /** The child elements of {@code parent} with that local name, or all of them for a null name. */
    private static List<Element> children(Element parent, String name) {
        List<Element> found = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && (name == null || name.equals(element.getLocalName()))) {
                found.add(element);
            }
        }
        return found;
    }

    private static Element single(Element parent, String name, String within) {
        List<Element> found = children(parent, name);
        if (found.size() != 1) {
            throw unreadable("smoke/pom.xml: expected exactly one <" + name + "> in " + within + ", found " + found.size());
        }
        return found.get(0);
    }

    private static String text(Element element) {
        return element.getTextContent().trim();
    }

    private static String resolve(String value, Map<String, String> properties) {
        Matcher reference = Pattern.compile("^\\$\\{([^}]+)}$").matcher(value);
        if (!reference.matches()) {
            return value;
        }
        String resolved = properties.get(reference.group(1));
        if (resolved == null || resolved.isBlank()) {
            throw unreadable("smoke/pom.xml: the plugin version names property " + reference.group(1) + ", which <properties> does not define");
        }
        return resolved;
    }

    // ---- the wedding-portal side: openApiGenerate in rekord-adapter/build.gradle.kts and the catalog -----------------

    static Settings portalSettings(String buildScript, String catalog) {
        String code = withoutComments(buildScript);
        Block generate = openApiGenerateBlock(code);
        refuseGeneratorSettingsOutsideTheBlock(generate.outside());
        requireTheGeneratorPluginThroughItsCatalogAlias(code);
        String block = generate.body();

        Matcher options = Pattern.compile("(?m)^[ \\t]*configOptions[ \\t]*=[ \\t]*mapOf\\(").matcher(block);
        if (!options.find()) {
            throw unreadable("no `configOptions = mapOf(` in the openApiGenerate block");
        }
        int close = matching(block, options.end() - 1, '(', ')');
        Map<String, String> configOptions = configOptions(block.substring(options.end(), close));
        String rest = block.substring(0, options.start()) + block.substring(close + 1);

        Map<String, String> parameters = new TreeMap<>();
        for (String line : withoutComments(rest).split("\\R")) {
            String statement = line.trim();
            if (statement.isEmpty()) {
                continue;
            }
            Matcher assignment = Pattern.compile("^(\\w+)\\s*=\\s*(.+)$").matcher(statement);
            if (!assignment.matches()) {
                throw unreadable("cannot read this line of the openApiGenerate block: " + statement);
            }
            String name = assignment.group(1);
            if (PORTAL_ENVIRONMENT.contains(name)) {
                continue;
            }
            Matcher literal = Pattern.compile("^(?:\"([^\"]*)\"|(true|false|-?\\d+))$").matcher(assignment.group(2).trim());
            if (!literal.matches()) {
                throw unreadable("cannot read the value of " + name + " in the openApiGenerate block (not a literal): " + statement);
            }
            put(parameters, name, literal.group(1) != null ? literal.group(1) : literal.group(2));
        }
        return new Settings(portalVersion(catalog), parameters, configOptions);
    }

    /** The one {@code openApiGenerate { ... }} block of a script: the text between its braces, and the rest of the script. */
    private record Block(String body, String outside) {}

    private static Block openApiGenerateBlock(String script) {
        Matcher start = Pattern.compile("(?m)^[ \\t]*openApiGenerate[ \\t]*\\{").matcher(script);
        if (!start.find()) {
            throw unreadable("no `openApiGenerate {` block in " + BUILD_SCRIPT + "; if it moved, point this test at it");
        }
        int head = start.start();
        int open = start.end() - 1;
        if (start.find()) {
            throw unreadable("more than one `openApiGenerate {` block in " + BUILD_SCRIPT);
        }
        int close = matching(script, open, '{', '}');
        return new Block(script.substring(open + 1, close), script.substring(0, head) + script.substring(close + 1));
    }

    /**
     * The rest of the script must not reach the generator: this reader sees the {@code openApiGenerate { }} block only,
     * so a task lookup, the task type, the accessor or a setting name outside it is refused. The source-root wiring
     * {@code tasks.named("openApiGenerate").map { ... }} names the task and sets nothing, so it is the one exception.
     */
    private static void refuseGeneratorSettingsOutsideTheBlock(String outside) {
        String rest = SOURCE_ROOT_WIRING.matcher(outside).replaceAll("{");
        Matcher afterEvaluate = Pattern.compile("\\bafterEvaluate\\b").matcher(rest);
        while (afterEvaluate.find()) {
            int open = rest.indexOf('{', afterEvaluate.end());
            if (open >= 0 && GENERATOR_SETTING_NAME.matcher(rest.substring(open, matching(rest, open, '{', '}'))).find()) {
                throw outsideTheBlock("an afterEvaluate block that sets configOptions or generatorName", rest, afterEvaluate.start());
            }
        }
        for (Construct construct : OUTSIDE_THE_BLOCK) {
            Matcher found = construct.pattern().matcher(rest);
            if (found.find()) {
                throw outsideTheBlock(construct.description(), rest, found.start());
            }
        }
    }

    /**
     * The version compared is the one of the catalog entry the alias names, so the plugin must come in through that
     * alias: an id with its own version, an {@code apply}, or a buildscript classpath entry would run another version.
     */
    private static void requireTheGeneratorPluginThroughItsCatalogAlias(String code) {
        for (Construct application : PLUGIN_APPLIED_ELSEWHERE) {
            Matcher found = application.pattern().matcher(code);
            if (found.find()) {
                throw unreadable(BUILD_SCRIPT + " applies " + OPENAPI_GENERATOR_PLUGIN_ID + " through " + application.description()
                        + " (`" + lineAt(code, found.start()) + "`), not through " + GENERATOR_ALIAS_SOURCE
                        + "; this test takes the generator version from the catalog entry of that alias, so another way "
                        + "in could run a version it does not compare");
            }
        }
        if (!GENERATOR_ALIAS.matcher(code).find()) {
            throw unreadable(BUILD_SCRIPT + " does not apply " + OPENAPI_GENERATOR_PLUGIN_ID + " through " + GENERATOR_ALIAS_SOURCE
                    + ", the catalog entry this test takes the generator version from");
        }
    }

    private static IllegalStateException outsideTheBlock(String what, String code, int at) {
        return unreadable(BUILD_SCRIPT + " has " + what + " (`" + lineAt(code, at) + "`), which this test does not read; "
                + "keep every generator setting in the openApiGenerate { } block, or extend this test to read it");
    }

    /** The line of {@code text} that holds {@code index}, trimmed and cut at 120 characters. */
    private static String lineAt(String text, int index) {
        int from = text.lastIndexOf('\n', index) + 1;
        int to = text.indexOf('\n', index);
        String line = text.substring(from, to < 0 ? text.length() : to).trim();
        return line.length() > 120 ? line.substring(0, 120) + "..." : line;
    }

    /** The index of the bracket that closes the one at {@code open}, skipping strings and comments. */
    private static int matching(String text, int open, char opening, char closing) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                i = endOfString(text, i);
            } else if (c == '/' && text.startsWith("//", i)) {
                int end = text.indexOf('\n', i);
                i = end < 0 ? text.length() : end;
            } else if (c == '/' && text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                i = end < 0 ? text.length() : end + 1;
            } else if (c == opening) {
                depth++;
            } else if (c == closing && --depth == 0) {
                return i;
            }
        }
        throw unreadable("a `" + opening + "` in " + BUILD_SCRIPT + " is never closed");
    }

    private static int endOfString(String text, int quote) {
        for (int i = quote + 1; i < text.length(); i++) {
            if (text.charAt(i) == '\\') {
                i++;
            } else if (text.charAt(i) == '"') {
                return i;
            }
        }
        throw unreadable("a string in " + BUILD_SCRIPT + " is never closed");
    }

    private static String withoutComments(String text) {
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                int end = endOfString(text, i);
                kept.append(text, i, end + 1);
                i = end;
            } else if (text.startsWith("//", i)) {
                int end = text.indexOf('\n', i);
                i = end < 0 ? text.length() : end - 1;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                i = end < 0 ? text.length() : end + 1;
            } else {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    /** The {@code "key" to "value"} pairs inside {@code mapOf( ... )}; anything else in there is refused. */
    private static Map<String, String> configOptions(String entries) {
        String text = withoutComments(entries);
        Map<String, String> options = new TreeMap<>();
        Matcher pair = Pattern.compile("\\G\\s*\"([^\"]+)\"\\s+to\\s+\"([^\"]*)\"\\s*(?:,|$)").matcher(text);
        int read = 0;
        while (pair.find()) {
            put(options, pair.group(1), pair.group(2));
            read = pair.end();
        }
        if (!text.substring(read).isBlank()) {
            throw unreadable("cannot read the configOptions entry near: " + text.substring(read).trim().lines().findFirst().orElse(""));
        }
        return options;
    }

    /** The openapi-generator plugin's version as the catalog resolves it, through {@code version.ref} if it has one. */
    private static String portalVersion(String catalog) {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        String section = "";
        for (String line : catalog.split("\\R")) {
            Matcher header = Pattern.compile("^\\[([^\\]]+)]\\s*$").matcher(line.trim());
            Matcher entry = Pattern.compile("^([\\w.-]+)\\s*=\\s*(.+)$").matcher(line.trim());
            if (header.matches()) {
                section = header.group(1);
            } else if (entry.matches()) {
                sections.computeIfAbsent(section, key -> new LinkedHashMap<>()).put(entry.group(1), entry.group(2));
            }
        }
        List<String> plugins = sections.getOrDefault("plugins", Map.of()).values().stream()
                .filter(value -> value.contains("\"" + OPENAPI_GENERATOR_PLUGIN_ID + "\""))
                .toList();
        if (plugins.size() != 1) {
            throw unreadable("gradle/libs.versions.toml must hold exactly one plugin with id " + OPENAPI_GENERATOR_PLUGIN_ID + ", found " + plugins.size());
        }
        Matcher reference = Pattern.compile("version\\.ref\\s*=\\s*\"([^\"]+)\"").matcher(plugins.get(0));
        if (reference.find()) {
            String version = sections.getOrDefault("versions", Map.of()).get(reference.group(1));
            if (version == null) {
                throw unreadable("gradle/libs.versions.toml has no version named " + reference.group(1));
            }
            return version.replaceAll("^\"|\"$", "");
        }
        Matcher literal = Pattern.compile("\\bversion\\s*=\\s*\"([^\"]+)\"").matcher(plugins.get(0));
        if (literal.find()) {
            return literal.group(1);
        }
        throw unreadable("the " + OPENAPI_GENERATOR_PLUGIN_ID + " plugin entry of gradle/libs.versions.toml names no version");
    }

    // ---- the dependencies: smoke/pom.xml <dependencies> and the external api(...) of rekord-adapter (TASK-2.10) ---------

    /** The {@code groupId:artifactId} of every {@code <dependency>} of the pom's own {@code <dependencies>}. */
    static Set<String> hubDependencies(String pomXml) {
        throw new UnsupportedOperationException("TASK-2.10: hubDependencies is not implemented");
    }

    /** The {@code group:artifact} of every external {@code api("...")} in the script's one top-level {@code dependencies { }}. */
    static Set<String> portalDependencies(String buildScript) {
        throw new UnsupportedOperationException("TASK-2.10: portalDependencies is not implemented");
    }

    /** One line per coordinate only one side declares, naming the file it is in; empty when the two sets are equal. */
    static List<String> dependencyDifferences(Set<String> hub, Set<String> portal) {
        throw new UnsupportedOperationException("TASK-2.10: dependencyDifferences is not implemented");
    }

    // ---- comparison -------------------------------------------------------------------------------------------------

    /** One line per difference, naming the setting and both values; empty when the two sides generate alike. */
    static List<String> differences(Settings hub, Settings portal) {
        List<String> differences = new ArrayList<>();
        if (!Objects.equals(hub.version(), portal.version())) {
            differences.add("generator version: hub " + hub.version() + ", wedding-portal " + portal.version());
        }
        compare(differences, "", hub.parameters(), portal.parameters());
        compare(differences, "configOptions.", hub.configOptions(), portal.configOptions());
        return differences;
    }

    private static void compare(List<String> differences, String prefix, Map<String, String> hub, Map<String, String> portal) {
        var keys = new TreeSet<>(hub.keySet());
        keys.addAll(portal.keySet());
        for (String key : keys) {
            if (!Objects.equals(hub.get(key), portal.get(key))) {
                differences.add(prefix + key + ": hub " + shown(hub.get(key)) + ", wedding-portal " + shown(portal.get(key)));
            }
        }
    }

    private static String shown(String value) {
        return value == null ? "(absent)" : value;
    }

    private static void put(Map<String, String> into, String key, String value) {
        if (into.put(key, value) != null) {
            throw unreadable("a side sets " + key + " twice");
        }
    }

    private static IllegalStateException unreadable(String message) {
        return new IllegalStateException(message);
    }

    // ---- small fixtures, independent of the real files --------------------------------------------------------------

    private static final String HUB_POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <properties>
                <maven.compiler.release>25</maven.compiler.release>
                <openapi-generator.version>7.25.0</openapi-generator.version>
              </properties>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openapitools</groupId>
                    <artifactId>openapi-generator-maven-plugin</artifactId>
                    <version>${openapi-generator.version}</version>
                    <executions>
                      <execution>
                        <goals><goal>generate</goal></goals>
                        <configuration>
                          <inputSpec>${project.basedir}/../dist/openapi.yaml</inputSpec>
                          <generatorName>jaxrs-spec</generatorName>
                          <output>${project.build.directory}/generated</output>
                          <apiPackage>a.api</apiPackage>
                          <modelPackage>a.api.model</modelPackage>
                          <generateApiTests>false</generateApiTests>
                          <generateModelTests>false</generateModelTests>
                          <configOptions>
                            <!-- a comment between options is not an option -->
                            <interfaceOnly>true</interfaceOnly>
                            <dateLibrary>java8</dateLibrary>
                          </configOptions>
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
            """;

    private static final String PORTAL_SCRIPT = """
            plugins {
                alias(libs.plugins.openapi.generator)
            }

            val contractSpec = "spec"

            openApiGenerate {
                generatorName = "jaxrs-spec"
                inputSpec = contractSpec
                outputDir = layout.buildDirectory.dir("generated/openapi")
                cleanupOutput = true
                apiPackage = "a.api"
                modelPackage = "a.api.model"
                generateApiTests = false
                generateModelTests = false
                configOptions = mapOf(
                    "interfaceOnly" to "true",
                    "dateLibrary" to "java8",
                )
            }

            sourceSets.main {
                java.srcDir("generated")
            }
            """;

    private static final String CATALOG = """
            [versions]
            openapi-generator = "7.25.0"

            [plugins]
            openapi-generator = { id = "org.openapi.generator", version.ref = "openapi-generator" }
            """;

    private static final String DATABIND =
            "<dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId>"
                    + "<version>2.22</version></dependency>";

    private static final String HUB_DEPENDENCIES_POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <dependencies>
                <!-- Only what the generated interfaces reference. -->
                <dependency>
                  <groupId>jakarta.ws.rs</groupId>
                  <artifactId>jakarta.ws.rs-api</artifactId>
                  <version>4.0.0</version>
                </dependency>
                <dependency>
                  <groupId>jakarta.validation</groupId>
                  <artifactId>jakarta.validation-api</artifactId>
                  <version>3.1.1</version>
                </dependency>
                <dependency>
                  <groupId>jakarta.annotation</groupId>
                  <artifactId>jakarta.annotation-api</artifactId>
                  <version>3.0.0</version>
                </dependency>
                <dependency>
                  <groupId>com.fasterxml.jackson.core</groupId>
                  <artifactId>jackson-annotations</artifactId>
                  <version>2.22</version>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openapitools</groupId>
                    <artifactId>openapi-generator-maven-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """;

    /** The shape of rekord-adapter/build.gradle.kts: the four external api(...), project(...), and the other configurations. */
    private static final String PORTAL_DEPENDENCIES_SCRIPT = """
            plugins {
                `java-library`
                alias(libs.plugins.openapi.generator)
            }

            dependencies {
                implementation(platform(libs.quarkus.bom))
                compileOnly(platform(libs.quarkus.bom))
                compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
                // The APIs the generated sources import; versions come from the BOM.
                api("jakarta.ws.rs:jakarta.ws.rs-api")
                api("jakarta.validation:jakarta.validation-api")
                api("com.fasterxml.jackson.core:jackson-annotations")
                api("jakarta.annotation:jakarta.annotation-api")
                api(project(":rekord-usecase"))
                api(project(":rekord-domain"))
                implementation(project(":logging"))
                testImplementation("org.junit.jupiter:junit-jupiter")
                testImplementation(libs.assertj.core)
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                testImplementation(project(path = ":rekord-usecase", configuration = "testArtifacts"))
                testCompileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api")
            }
            """;

    /** {@code script} with one more statement in its {@code dependencies { }} block. */
    private static String withDependencyLine(String script, String line) {
        String domain = "    api(project(\":rekord-domain\"))\n";
        return script.replace(domain, "    " + line + "\n" + domain);
    }
}
