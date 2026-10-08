package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TestTaskInputsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Pattern EXCLUDE_ARGUMENTS = Pattern.compile("exclude\\(([^)]*)\\)");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern NAMED_FILES = Pattern.compile("rootProject\\.files\\(([^)]*)\\)");
    /**
     * The Ant default excludes Gradle applies to every {@code fileTree} on top of its own excludes
     * ({@code DirectoryScanner.getDefaultExcludes()}): a tracked file they match is an input only when named itself.
     */
    private static final List<String> GRADLE_DEFAULT_EXCLUDES = List.of(
            "**/%*%", "**/.#*", "**/._*", "**/#*#", "**/*~", "**/.DS_Store",
            "**/CVS", "**/CVS/**", "**/.cvsignore", "**/SCCS", "**/SCCS/**", "**/vssver.scc",
            "**/.bzr", "**/.bzr/**", "**/.bzrignore",
            "**/.git", "**/.git/**", "**/.gitattributes", "**/.gitignore", "**/.gitmodules",
            "**/.hg", "**/.hg/**", "**/.hgignore", "**/.hgsub", "**/.hgsubstate", "**/.hgtags",
            "**/.svn", "**/.svn/**");

    @Test
    void the_test_task_declares_the_repository_tree_as_a_relative_input() throws IOException {
        // Given the tasks.test block of the application build script
        String block = testTaskBlock();

        // Then it declares the repository tree as a relative-path-sensitive input named repoFiles
        assertThat(block)
                .contains("inputs.files(fileTree(rootDir)")
                .contains("withPropertyName(\"repoFiles\")")
                .contains("PathSensitivity.RELATIVE");
    }

    @Test
    void every_tracked_file_is_an_input_of_the_test_task() throws Exception {
        // Given the excludes of the repository tree input, the files it names itself, and the files git tracks
        String block = testTaskBlock();
        List<String> excludes = excludes(block);
        List<String> named = namedFiles(block);
        List<String> tracked = trackedFiles();

        // When the excludes, and Gradle's default excludes unless a file is named itself, meet every tracked path
        List<String> dropped = tracked.stream()
                .filter(path -> isExcluded(excludes, path)
                        || (isExcluded(GRADLE_DEFAULT_EXCLUDES, path) && !named.contains(path)))
                .toList();

        // Then no tracked file is dropped, and the ticket's examples are tracked
        assertThat(excludes).isNotEmpty();
        assertThat(dropped).isEmpty();
        assertThat(tracked)
                .contains(
                        ".github/workflows/ci.yml",
                        "Dockerfile",
                        "gradle.properties",
                        "rekord-adapter/build.gradle.kts",
                        "gradle/libs.versions.toml",
                        "docs/code-maps/jvm-testing.md",
                        "docs/memory.md",
                        ".github/scripts/image-check.sh",
                        "application/src/test/java/app/rekord/application/build/CiWorkflowTest.java");
    }

    @Test
    void build_outputs_and_tool_state_are_not_inputs_of_the_test_task() throws IOException {
        // Given the excludes of the repository tree input
        List<String> excludes = excludes(testTaskBlock());

        // Then outputs of the build and tool state are dropped, so a run does not make the next one stale
        assertThat(excludes).isNotEmpty();
        assertThat(List.of(
                        "application/build/test-results/test/TEST-x.xml",
                        "rekord-adapter/build/generated/openapi/x.java",
                        "build/reports/problems/problems-report.html",
                        ".gradle/x",
                        ".git",
                        ".git/index",
                        ".idea/x",
                        ".kotlin/x",
                        "application.iml",
                        "out/x",
                        "logging/out/x",
                        "contract/dist/openapi.yaml"))
                .allSatisfy(path -> assertThat(isExcluded(excludes, path))
                        .as("%s is excluded", path)
                        .isTrue());
    }

    @Test
    void an_unanchored_build_exclude_would_drop_the_build_test_package() {
        // Given an unanchored build exclude
        List<String> unanchored = List.of("**/build/**");

        // Then the matcher drops the test package named build, so the tracked-file check is not vacuous
        assertThat(isExcluded(
                        unanchored, "application/src/test/java/app/rekord/application/build/CiWorkflowTest.java"))
                .isTrue();
    }

    @Test
    void gradle_drops_the_git_files_tests_read_from_a_file_tree_by_default() {
        // Given the files GitAttributesTest, BuildLayoutTest and ContractPinTest read
        // Then Gradle's default excludes drop them from fileTree, so the tracked-file check must account for them
        assertThat(isExcluded(GRADLE_DEFAULT_EXCLUDES, ".gitattributes")).isTrue();
        assertThat(isExcluded(GRADLE_DEFAULT_EXCLUDES, ".gitignore")).isTrue();
        assertThat(isExcluded(GRADLE_DEFAULT_EXCLUDES, "docs/memory.md")).isFalse();
    }

    @Test
    void the_contract_inputs_come_from_the_contract_spec_property() throws IOException {
        // Given the tasks.test block of the application build script
        String block = testTaskBlock();

        // Then the contract files are derived from the same provider as the -Dcontract.spec argument
        assertThat(block)
                .contains("inputs.files(contractSpec")
                .contains("smoke/pom.xml")
                .contains("withPropertyName(\"contractFiles\")");

        // And no literal checkout path or tag, so raising the contract pin cannot touch them
        assertThat(contractInputsDeclaration(block))
                .doesNotContain("contract/dist")
                .doesNotContain("rekord-contract")
                .doesNotContain("rekordContractTag")
                .doesNotContainPattern("v\\d+\\.");
    }

    @Test
    void the_contract_inputs_are_optional_so_help_configures_without_the_property() throws IOException {
        // Given the contractFiles declaration
        String declaration = contractInputsDeclaration(testTaskBlock());

        // Then it is optional and its provider falls back to an empty list
        assertThat(declaration).contains("orElse(listOf())").contains(".optional()");
        assertThat(declaration.indexOf(".optional()")).isGreaterThan(declaration.indexOf("contractFiles"));
    }

    private static String contractInputsDeclaration(String block) {
        int start = block.indexOf("inputs.files(contractSpec");
        assertThat(start).as("a contractSpec input declaration").isNotNegative();
        int end = block.indexOf(".optional()", start);
        return block.substring(start, end < 0 ? block.length() : end + ".optional()".length());
    }

    private static List<String> trackedFiles() throws Exception {
        Process process = new ProcessBuilder("git", "ls-files", "-z")
                .directory(REPO_ROOT.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).isZero();
        List<String> files = new ArrayList<>();
        for (String path : output.split("\0")) {
            if (!path.isEmpty()) {
                files.add(path);
            }
        }
        assertThat(files).isNotEmpty();
        return files;
    }

    private static boolean isExcluded(List<String> excludes, String path) {
        Path relative = Path.of(path);
        for (String exclude : excludes) {
            if (matches(exclude, relative)) {
                return true;
            }
            // Ant patterns let a leading **/ match zero directories; Java globs do not
            if (exclude.startsWith("**/") && matches(exclude.substring(3), relative)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String glob, Path relative) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(relative);
    }

    /** The literals of {@code rootProject.files(...)}: files declared one by one, past Gradle's default excludes. */
    private static List<String> namedFiles(String block) {
        List<String> literals = new ArrayList<>();
        Matcher calls = NAMED_FILES.matcher(block);
        while (calls.find()) {
            Matcher strings = STRING_LITERAL.matcher(calls.group(1));
            while (strings.find()) {
                literals.add(strings.group(1));
            }
        }
        return literals;
    }

    private static List<String> excludes(String block) {
        List<String> literals = new ArrayList<>();
        Matcher calls = EXCLUDE_ARGUMENTS.matcher(block);
        while (calls.find()) {
            Matcher strings = STRING_LITERAL.matcher(calls.group(1));
            while (strings.find()) {
                literals.add(strings.group(1));
            }
        }
        return literals;
    }

    private static String testTaskBlock() throws IOException {
        String script = Files.readString(REPO_ROOT.resolve("application/build.gradle.kts"));
        List<String> blocks = new ArrayList<>();
        Matcher opening = Pattern.compile("(?m)^\\s*tasks\\.test\\s*\\{").matcher(script);
        while (opening.find()) {
            blocks.add(script.substring(opening.start(), blockEnd(script, opening.end())));
        }
        assertThat(blocks).as("tasks.test blocks in application/build.gradle.kts").hasSize(1);
        return blocks.get(0);
    }

    /** Index after the brace closing the one before {@code from}, skipping strings and comments. */
    private static int blockEnd(String script, int from) {
        int depth = 1;
        int i = from;
        while (i < script.length()) {
            char c = script.charAt(i);
            if (script.startsWith("//", i)) {
                int eol = script.indexOf('\n', i);
                i = eol < 0 ? script.length() : eol;
            } else if (script.startsWith("/*", i)) {
                int end = script.indexOf("*/", i + 2);
                i = end < 0 ? script.length() : end + 2;
            } else if (script.startsWith("\"\"\"", i)) {
                int end = script.indexOf("\"\"\"", i + 3);
                i = end < 0 ? script.length() : end + 3;
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < script.length() && script.charAt(i) != c) {
                    i += script.charAt(i) == '\\' ? 2 : 1;
                }
                i++;
            } else {
                if (c == '{') {
                    depth++;
                } else if (c == '}' && --depth == 0) {
                    return i + 1;
                }
                i++;
            }
        }
        throw new IllegalStateException("unbalanced braces in tasks.test");
    }
}
