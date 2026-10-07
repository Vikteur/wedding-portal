package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BuildLayoutTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Set<String> SKIPPED_DIRS = Set.of(".git", ".gradle", ".idea", ".kotlin");
    /** Build outputs sit in the root or a module directory; a source package named {@code build} is still walked. */
    private static final Set<String> SKIPPED_OUTPUT_DIRS = Set.of("build", "out");

    @Test
    void settings_includes_exactly_the_six_modules_in_the_kotlin_dsl() throws IOException {
        assertThat(REPO_ROOT.resolve("settings.gradle.kts")).exists();
        assertThat(REPO_ROOT.resolve("settings.gradle")).doesNotExist();

        assertThat(GradleSettings.includedModules(REPO_ROOT))
                .containsExactlyInAnyOrder(
                        "rekord-domain", "rekord-usecase", "rekord-adapter", "rekord-gateway", "application", "logging");
    }

    @Test
    void every_module_compiles_on_the_java_25_toolchain_with_release_25() throws IOException {
        Path rootScript = REPO_ROOT.resolve("build.gradle.kts");
        String root = read(rootScript);
        int subprojects = root.indexOf("subprojects {");
        assertThat(subprojects).as("a subprojects block in the root build script").isNotNegative();
        String block = root.substring(subprojects);
        assertThat(block).containsPattern("languageVersion\\s*=\\s*JavaLanguageVersion\\.of\\(25\\)");
        assertThat(block).containsPattern("options\\.release\\s*=\\s*25\\b");

        Pattern override = Pattern.compile(
                "languageVersion|JavaLanguageVersion\\.of\\(|options\\.release|sourceCompatibility|targetCompatibility");
        for (Path file : gradleKtsFiles()) {
            if (file.equals(rootScript)) {
                continue;
            }
            assertThat(override.matcher(read(file)).find())
                    .as("%s overrides the toolchain or compiler release", file)
                    .isFalse();
        }

        Pattern javaPlugin = Pattern.compile("(?m)^\\s*(java|`java-library`)\\s*$");
        for (String module : GradleSettings.includedModules(REPO_ROOT)) {
            assertThat(javaPlugin.matcher(read(REPO_ROOT.resolve(module).resolve("build.gradle.kts"))).find())
                    .as("%s applies java or java-library", module)
                    .isTrue();
        }
    }

    @Test
    void only_application_applies_the_quarkus_plugin() throws IOException {
        Pattern quarkusPlugin = Pattern.compile("libs\\.plugins\\.quarkus|id\\(\"io\\.quarkus\"\\)");

        assertThat(quarkusPlugin.matcher(read(REPO_ROOT.resolve("application/build.gradle.kts"))).find())
                .isTrue();

        List<Path> others = gradleKtsFiles().stream()
                .filter(file -> !file.equals(REPO_ROOT.resolve("application/build.gradle.kts")))
                .filter(file -> quarkusPlugin.matcher(readUnchecked(file)).find())
                .toList();
        assertThat(others).isEmpty();
    }

    @Test
    void one_quarkus_platform_bom_is_pinned_in_the_catalog() throws IOException {
        String catalog = read(REPO_ROOT.resolve("gradle/libs.versions.toml"));

        List<String> boms = catalog.lines()
                .filter(line -> line.contains("module = \"io.quarkus.platform:quarkus-bom\""))
                .toList();
        assertThat(boms).hasSize(1);

        Matcher ref = Pattern.compile("version\\.ref\\s*=\\s*\"([^\"]+)\"").matcher(boms.get(0));
        assertThat(ref.find()).as("the bom entry uses a version ref").isTrue();
        Matcher version = Pattern.compile("(?m)^" + Pattern.quote(ref.group(1)) + "\\s*=\\s*\"([^\"]+)\"")
                .matcher(catalog);
        assertThat(version.find()).as("the version ref resolves").isTrue();

        assertThat(version.group(1)).isEqualTo(pinnedQuarkusLine());
    }

    @Test
    void no_module_declares_a_quarkus_artifact_version() throws IOException {
        Pattern versionedArtifact = Pattern.compile("\"io\\.quarkus[\\w.-]*:[\\w.-]+:[^\"]+\"");
        Pattern versionedPlugin = Pattern.compile("id\\(\"io\\.quarkus\"\\)\\s*version");

        for (Path file : gradleKtsFiles()) {
            String content = read(file);
            assertThat(versionedArtifact.matcher(content).find())
                    .as("%s hard-codes a quarkus artifact version", file)
                    .isFalse();
            assertThat(versionedPlugin.matcher(content).find())
                    .as("%s hard-codes a quarkus plugin version", file)
                    .isFalse();
        }
    }

    @Test
    void catalog_declares_no_quarkus_version_besides_the_platform_bom() throws IOException {
        // Sub-tables such as [libraries.quarkus-arc] would hide entries from the line checks below.
        List<String> headings = read(REPO_ROOT.resolve("gradle/libs.versions.toml")).lines()
                .map(String::strip)
                .filter(line -> line.startsWith("["))
                .toList();
        assertThat(headings).isSubsetOf("[versions]", "[libraries]", "[plugins]", "[bundles]");

        // Table notation { module = ..., version = ... } and string notation "group:name:version".
        Pattern quarkusLibrary = Pattern.compile("=\\s*(\"io\\.quarkus[\\w.-]*:|\\{.*\\b(module|group)\\s*=\\s*\"io\\.quarkus)");
        Pattern versioned = Pattern.compile("\\bversion(\\.ref)?\\s*=|=\\s*\"[^\":]+:[^\":]+:[^\"]+\"");
        List<String> versionedQuarkus = catalogSection("libraries")
                .filter(line -> quarkusLibrary.matcher(line).find())
                .filter(line -> versioned.matcher(line).find())
                .toList();
        assertThat(versionedQuarkus).hasSize(1);
        assertThat(versionedQuarkus.get(0)).contains("module = \"io.quarkus.platform:quarkus-bom\"");

        // Table notation { id = "io.quarkus", ... } and string notation "io.quarkus:version".
        Pattern quarkusPlugin = Pattern.compile("=\\s*\"io\\.quarkus:|\\bid\\s*=\\s*\"io\\.quarkus\"");
        List<String> plugin = catalogSection("plugins")
                .filter(line -> quarkusPlugin.matcher(line).find())
                .toList();
        assertThat(plugin).hasSize(1);
        assertThat(plugin.get(0)).containsPattern("version\\.ref\\s*=\\s*\"quarkus\"");
        assertThat(versionedQuarkus.get(0)).containsPattern("version\\.ref\\s*=\\s*\"quarkus\"");
    }

    @Test
    void no_python_file_exists() throws IOException {
        try (Stream<Path> files = repoFiles()) {
            List<Path> python = files.filter(file -> file.getFileName().toString().endsWith(".py")).toList();

            assertThat(python).isEmpty();
        }
    }

    @Test
    void no_workflow_runs_python() throws IOException {
        Pattern python = Pattern.compile("(?i)python|setup-python|\\bpip3?\\b|\\.py\\b");

        for (Path workflow : workflowFiles()) {
            List<String> offending = read(workflow).lines()
                    .filter(line -> python.matcher(line).find())
                    .toList();
            assertThat(offending).as("python in %s", workflow).isEmpty();
        }
    }

    @Test
    void nothing_depends_on_or_ships_rekord_api() throws IOException {
        List<Path> files = Stream.concat(
                        Stream.concat(gradleKtsFiles().stream(), workflowFiles().stream()),
                        Stream.of(
                                REPO_ROOT.resolve("gradle/libs.versions.toml"),
                                REPO_ROOT.resolve("Dockerfile"),
                                REPO_ROOT.resolve(".dockerignore")))
                .toList();

        for (Path file : files) {
            assertThat(read(file)).as("%s", file).doesNotContain("rekord-api");
        }
    }

    @Test
    void gitignore_anchors_build_outputs_so_source_packages_named_build_stay_tracked() throws IOException {
        List<String> unanchored = read(REPO_ROOT.resolve(".gitignore")).lines()
                .map(String::strip)
                .filter(line -> line.matches("(.*/)?(build|out)/?"))
                .filter(line -> !line.startsWith("/"))
                .toList();

        assertThat(unanchored).isEmpty();
    }

    @Test
    void memory_names_the_chosen_executor() throws IOException {
        Path memory = REPO_ROOT.resolve("docs/memory.md");
        assertThat(memory).exists();

        Matcher executor = Pattern.compile("(?m)^Executor \\(UD-17\\):\\s*(\\S.*)$(?:\\R-.*)*")
                .matcher(read(memory));
        assertThat(executor.find()).as("an 'Executor (UD-17):' section").isTrue();
        assertThat(executor.group()).containsIgnoringCase("Archon").doesNotContainIgnoringCase("to be confirmed");
    }

    @Test
    void memory_records_the_pinned_quarkus_line_matching_the_catalog() throws IOException {
        Matcher memoryLine = Pattern.compile("(?m)^Quarkus platform:\\s*(\\S+)")
                .matcher(read(REPO_ROOT.resolve("docs/memory.md")));
        assertThat(memoryLine.find()).as("a 'Quarkus platform:' line").isTrue();

        Matcher catalogLine = Pattern.compile("(?m)^quarkus\\s*=\\s*\"([^\"]+)\"")
                .matcher(read(REPO_ROOT.resolve("gradle/libs.versions.toml")));
        assertThat(catalogLine.find()).as("the quarkus version in the catalog").isTrue();

        assertThat(memoryLine.group(1)).isEqualTo(catalogLine.group(1));
    }

    /** The pinned line is 3.39.1 unless docs/memory.md records a different line chosen by the Quarkus fallback. */
    private static String pinnedQuarkusLine() throws IOException {
        Path memory = REPO_ROOT.resolve("docs/memory.md");
        if (Files.exists(memory)) {
            Matcher line = Pattern.compile("(?m)^Quarkus platform:\\s*(\\S+)").matcher(read(memory));
            if (line.find()) {
                return line.group(1);
            }
        }
        return "3.39.1";
    }

    /** The non-blank, non-comment lines of one {@code [section]} of the version catalog. */
    private static Stream<String> catalogSection(String section) throws IOException {
        List<String> lines = read(REPO_ROOT.resolve("gradle/libs.versions.toml")).lines().toList();
        int start = lines.indexOf("[" + section + "]");
        assertThat(start).as("a [%s] section in the catalog", section).isNotNegative();
        return lines.stream()
                .skip(start + 1)
                .takeWhile(line -> !line.strip().startsWith("["))
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"));
    }

    private static List<Path> gradleKtsFiles() throws IOException {
        try (Stream<Path> files = repoFiles()) {
            return files.filter(file -> file.getFileName().toString().endsWith(".gradle.kts"))
                    .collect(Collectors.toList());
        }
    }

    private static List<Path> workflowFiles() throws IOException {
        Path workflows = REPO_ROOT.resolve(".github/workflows");
        if (!Files.isDirectory(workflows)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(workflows)) {
            return files.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    private static Stream<Path> repoFiles() throws IOException {
        return Files.walk(REPO_ROOT)
                .filter(Files::isRegularFile)
                .filter(file -> {
                    Path relative = REPO_ROOT.relativize(file);
                    for (int i = 0; i < relative.getNameCount() - 1; i++) {
                        String part = relative.getName(i).toString();
                        if (SKIPPED_DIRS.contains(part) || (i <= 1 && SKIPPED_OUTPUT_DIRS.contains(part))) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file);
    }

    private static String readUnchecked(Path file) {
        try {
            return read(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
