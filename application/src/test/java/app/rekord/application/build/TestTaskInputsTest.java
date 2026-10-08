package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TestTaskInputsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Pattern EXCLUDE_ARGUMENTS = Pattern.compile("\\bexclude\\s*\\(([^)]*)\\)");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern NAMED_FILES = Pattern.compile("rootProject\\.files\\(([^)]*)\\)");
    /** Characters a java.nio glob reads as syntax and Ant reads as themselves. */
    private static final Pattern GLOB_SYNTAX = Pattern.compile("[{}\\[\\]\\\\]");

    /** A way of narrowing the tree that the tracked-file check does not read, and why it is refused. */
    private record Narrowing(Pattern shape, String why) {}

    private static final List<Narrowing> NARROWINGS = List.of(
            new Narrowing(Pattern.compile("\\binclude\\w*"), "an include keeps only the files it names"),
            new Narrowing(Pattern.compile("\\bmatching\\b"), "matching { } narrows the tree by a closure"),
            new Narrowing(Pattern.compile("\\bfilter\\b"), "filter { } narrows the tree by a closure"),
            new Narrowing(
                    Pattern.compile("(?i)excludes"), "excludes (a setter or +=) changes the patterns outside exclude(...)"),
            new Narrowing(Pattern.compile("\\bexclude\\s*\\{"), "an exclude closure is code, not a pattern"));
    /**
     * A copy of the Ant default excludes Gradle applies to every {@code fileTree} on top of its own excludes
     * ({@code DirectoryScanner.getDefaultExcludes()}), as of Gradle 9.8.0 (gradle/wrapper/gradle-wrapper.properties):
     * keep it in step on a Gradle upgrade. A tracked file they match is an input only when named itself.
     * No build script may change the defaults it copies: Gradle allows that in settings.gradle.kts only, and
     * {@code no_build_script_changes_gradles_default_excludes} reads every tracked *.gradle.kts for it.
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
        String declaration = repoFilesDeclaration(testTaskBlock());
        List<String> excludes = excludes(declaration);
        List<String> named = namedFiles(declaration);
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
    void the_repository_tree_input_uses_only_shapes_the_tracked_file_check_can_model() throws IOException {
        // Given the repoFiles declaration, comments left out
        String declaration = repoFilesDeclaration(testTaskBlock());

        // Then it narrows the tree by literal excludes only: a shape this test cannot read is refused (TASK-2.4 ADR-05),
        // because the tracked-file check would count a dropped file as an input and pass
        assertThat(refusals(declaration))
                .as("shapes of the repoFiles declaration that the tracked-file check cannot read")
                .isEmpty();
    }

    @Test
    void shapes_that_drop_tracked_files_where_the_check_cannot_see_it_are_refused() {
        // Given declarations that narrow the tree other than by a literal exclude, or by one the glob matcher misreads
        List<String> refused = List.of(
                treeWith("include(\"docs/**\")", ""),
                treeWith("exclude(\"build/**\")", ".matching { include(\"docs/**\") }"),
                treeWith("", ".filter { it.name != \"ci.yml\" }"),
                treeWith("excludes += listOf(\"docs/**\")", ""),
                treeWith("setExcludes(listOf(\"docs/**\"))", ""),
                treeWith("exclude { it.file.name == \"ci.yml\" }", ""),
                treeWith("exclude(outputDirs)", ""),
                treeWith("exclude(*patterns)", ""),
                treeWith("exclude(\"docs\" + \"/**\")", ""),
                treeWith("exclude(\"docs/**\", ignored)", ""),
                treeWith("exclude(\"docs)/**\")", ""),
                treeWith("exclude(\"$dir/**\")", ""),
                treeWith("exclude(\"docs/\")", ""),
                treeWith("exclude(\"docs/**/drafts\")", ""),
                treeWith("exclude(\"**/**/drafts\")", ""),
                treeWith("exclude(\"docs/{a,b}\")", ""),
                treeWith("exclude(\"docs/[ab]\")", ""),
                treeWith("exclude(\"docs\\\\drafts\")", ""));

        // Then each is refused, so the tracked-file check cannot pass on them
        assertThat(refused).allSatisfy(declaration -> assertThat(refusals(declaration))
                .as("%s is refused", declaration)
                .isNotEmpty());
    }

    @Test
    void shapes_the_tracked_file_check_models_are_accepted_and_comments_are_not_read() {
        // Given literal excludes in the forms the declaration uses, and refused words that only a comment mentions
        String declaration = treeWith(
                "exclude(\".gradle/**\", \"**/.gradle/**\", \"**/*.iml\", \"*/build/**\", \"contract/**\")\n"
                        + "    // include(\"docs/**\") and excludes += x: not code\n"
                        + "    /* .filter { } */",
                "");

        // Then nothing is refused, and the comments are gone before the declaration is read
        assertThat(refusals(withoutComments(declaration))).isEmpty();
        assertThat(withoutComments(declaration)).doesNotContain("include", "filter", "excludes");
        assertThat(excludes(withoutComments(declaration)))
                .containsExactly(".git", ".gradle/**", "**/.gradle/**", "**/*.iml", "*/build/**", "contract/**");
    }

    @Test
    void named_files_are_read_from_the_repo_files_declaration_and_not_from_comments() {
        // Given a declaration that no longer names .gitattributes, and a comment that does
        String declaration = "inputs.files(fileTree(rootDir) { exclude(\".git\") }, rootProject.files(\".gitignore\"))\n"
                + "    // rootProject.files(\".gitignore\", \".gitattributes\")\n";

        // Then only the call in code counts
        assertThat(namedFiles(withoutComments(declaration))).containsExactly(".gitignore");
    }

    @Test
    void build_outputs_and_tool_state_are_not_inputs_of_the_test_task() throws IOException {
        // Given the excludes of the repository tree input
        List<String> excludes = excludes(repoFilesDeclaration(testTaskBlock()));

        // Then outputs of the build and tool state are dropped, so a run does not make the next one stale
        assertThat(excludes).isNotEmpty();
        assertThat(List.of(
                        "rekord-adapter/build/generated/openapi/x.java",
                        "build/reports/problems/problems-report.html",
                        ".gradle/x",
                        ".git",
                        ".git/index",
                        ".idea/x",
                        ".kotlin/x",
                        "application.iml",
                        "out/x",
                        "contract/dist/openapi.yaml"))
                .allSatisfy(path -> assertThat(isExcluded(excludes, path))
                        .as("%s is excluded", path)
                        .isTrue());
    }

    @Test
    void every_gitignore_rule_is_excluded_from_the_test_task_inputs() throws IOException {
        // Given the excludes of the repository tree input, and for each .gitignore rule a path it ignores:
        // at the root, and for a rule that is not anchored also one module deep
        List<String> excludes = excludes(repoFilesDeclaration(testTaskBlock()));
        String module = GradleSettings.includedModules(REPO_ROOT).iterator().next();
        List<String> probes = new ArrayList<>();
        for (String rule : gitignoreRules()) {
            String probe = probePath(rule);
            probes.add(probe);
            if (!isAnchored(rule)) {
                probes.add(module + "/" + probe);
            }
        }

        // When the excludes meet every probe
        List<String> kept = probes.stream().filter(path -> !isExcluded(excludes, path)).toList();

        // Then git ignores each path, so the test task does not take it as an input: no output of a run is an input
        assertThat(probes).isNotEmpty();
        assertThat(kept).as("paths .gitignore ignores that are still inputs").isEmpty();
    }

    @Test
    void build_outputs_of_every_included_module_are_excluded_from_the_test_task_inputs() throws IOException {
        // Given the excludes of the repository tree input and the modules settings.gradle.kts includes
        List<String> excludes = excludes(repoFilesDeclaration(testTaskBlock()));
        Set<String> modules = GradleSettings.includedModules(REPO_ROOT);

        // When the build and out directory of each module meet the excludes
        List<String> kept = modules.stream()
                .map(TestTaskInputsTest::moduleDirectory)
                .flatMap(directory -> Stream.of(directory + "/build/x", directory + "/out/x"))
                .filter(path -> !isExcluded(excludes, path))
                .toList();

        // Then none is an input, however the module is named: a module added later is covered, or this fails
        assertThat(modules).isNotEmpty();
        assertThat(kept).isEmpty();
    }

    @Test
    void a_nested_module_would_leave_its_build_outputs_as_inputs() {
        // Given the anchored build excludes and a module directory two levels down
        List<String> excludes = List.of("build/**", "*/build/**", "out/**", "*/out/**");

        // Then its build output is not excluded, so the module check above is not vacuous
        assertThat(moduleDirectory(":libs:newmod")).isEqualTo("libs/newmod");
        assertThat(isExcluded(excludes, "libs/newmod/build/x")).isFalse();
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
    void no_build_script_changes_gradles_default_excludes() throws Exception {
        // Given the Kotlin build scripts git tracks
        List<String> scripts =
                trackedFiles().stream().filter(path -> path.endsWith(".gradle.kts")).toList();

        // Then none adds or removes a default exclude (DirectoryScanner.addDefaultExclude and removeDefaultExclude),
        // so GRADLE_DEFAULT_EXCLUDES is the list fileTree applies in this build
        assertThat(scripts).isNotEmpty();
        for (String script : scripts) {
            assertThat(Files.readString(REPO_ROOT.resolve(script)))
                    .as("%s changes Gradle's default excludes, which GRADLE_DEFAULT_EXCLUDES copies", script)
                    .doesNotContain("DefaultExclude");
        }
    }

    @Test
    void the_contract_inputs_come_from_the_contract_spec_property() throws IOException {
        // Given the tasks.test block of the application build script
        String block = testTaskBlock();

        // Then the contract files are derived from the same provider as the -Dcontract.spec argument
        assertThat(block).contains("inputs.files(contractSpec").contains("withPropertyName(\"contractFiles\")");

        // And the hub's smoke job is resolved two levels above the spec: <checkout>/dist/openapi.yaml names
        // <checkout>/smoke/pom.xml, where HubProbeParityTest.hubPom() reads it (the other half of this pair).
        // A hop lost here would name dist/smoke/pom.xml, which Gradle fingerprints as missing without an error
        // (the input is optional), so the exact resolution is pinned; ?. leaves a spec at a filesystem root out
        assertThat(contractInputsDeclaration(block))
                .contains(
                        "listOfNotNull(File(it), File(it).parentFile?.parentFile?.resolve(\"smoke/pom.xml\"))");

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

    /** The non-negated rules of .gitignore; a shape the probe builder cannot read is refused (TASK-2.4 ADR-05). */
    private static List<String> gitignoreRules() throws IOException {
        List<String> rules = new ArrayList<>();
        for (String line : Files.readAllLines(REPO_ROOT.resolve(".gitignore"))) {
            String rule = line.strip();
            if (rule.isEmpty() || rule.startsWith("#") || rule.startsWith("!")) {
                continue;
            }
            assertThat(rule)
                    .as("a .gitignore rule the probe builder can read: no **, [ ] or escapes")
                    .doesNotContain("**", "[", "\\");
            rules.add(rule);
        }
        return rules;
    }

    /** A rule is anchored to the .gitignore's directory when it has a / other than a trailing one. */
    private static boolean isAnchored(String rule) {
        return (rule.endsWith("/") ? rule.substring(0, rule.length() - 1) : rule).contains("/");
    }

    /** A path the rule ignores: wildcards become a letter, and below a directory rule a file is added. */
    private static String probePath(String rule) {
        boolean directory = rule.endsWith("/");
        String path = rule.substring(rule.startsWith("/") ? 1 : 0, directory ? rule.length() - 1 : rule.length())
                .replace('*', 'x')
                .replace('?', 'x');
        return directory ? path + "/x" : path;
    }

    /** The directory of a module as settings.gradle.kts names it: {@code :libs:newmod} is {@code libs/newmod}. */
    private static String moduleDirectory(String module) {
        return module.replaceFirst("^:", "").replace(':', '/');
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
    private static List<String> namedFiles(String declaration) {
        List<String> literals = new ArrayList<>();
        Matcher calls = NAMED_FILES.matcher(declaration);
        while (calls.find()) {
            Matcher strings = STRING_LITERAL.matcher(calls.group(1));
            while (strings.find()) {
                literals.add(strings.group(1));
            }
        }
        return literals;
    }

    private static List<String> excludes(String declaration) {
        List<String> literals = new ArrayList<>();
        Matcher calls = EXCLUDE_ARGUMENTS.matcher(declaration);
        while (calls.find()) {
            Matcher strings = STRING_LITERAL.matcher(calls.group(1));
            while (strings.find()) {
                literals.add(strings.group(1));
            }
        }
        return literals;
    }

    /**
     * Why the tracked-file check cannot read the declaration: every way of narrowing the tree other than {@code
     * exclude} with string literals that mean the same to Ant (Gradle) and to a java.nio glob. Empty when it can.
     */
    private static List<String> refusals(String declaration) {
        List<String> reasons = new ArrayList<>();
        for (Narrowing narrowing : NARROWINGS) {
            if (narrowing.shape().matcher(declaration).find()) {
                reasons.add(narrowing.why());
            }
        }
        Matcher calls = EXCLUDE_ARGUMENTS.matcher(declaration);
        while (calls.find()) {
            String rest = STRING_LITERAL.matcher(calls.group(1)).replaceAll("").replaceAll("[\\s,]", "");
            if (!rest.isEmpty()) {
                reasons.add("exclude(" + calls.group(1).strip() + ") takes string literals only, but has " + rest);
            }
        }
        for (String exclude : excludes(declaration)) {
            if (exclude.endsWith("/")) {
                reasons.add(exclude + ": Ant reads a trailing / as /**, a glob matches nothing");
            }
            String[] segments = exclude.split("/", -1);
            for (int i = 1; i < segments.length - 1; i++) {
                if (segments[i].equals("**")) {
                    reasons.add(exclude + ": a /**/ that is not the leading **/ matches zero directories in Ant only");
                    break;
                }
            }
            if (GLOB_SYNTAX.matcher(exclude).find()) {
                reasons.add(exclude + ": {}[]\\ are glob syntax here and plain characters in Ant");
            }
            if (exclude.contains("$")) {
                reasons.add(exclude + ": a string template is not a literal");
            }
        }
        return reasons;
    }

    /** The {@code repoFiles} declaration, from {@code inputs.files(fileTree(rootDir)} to its property name, without comments. */
    private static String repoFilesDeclaration(String block) {
        int start = block.indexOf("inputs.files(fileTree(rootDir)");
        assertThat(start).as("a repoFiles declaration starting with inputs.files(fileTree(rootDir)").isNotNegative();
        int end = block.indexOf("withPropertyName(\"repoFiles\")", start);
        assertThat(end).as("a repoFiles declaration ending with withPropertyName(\"repoFiles\")").isNotNegative();
        return withoutComments(block.substring(start, end));
    }

    /** A {@code repoFiles}-shaped declaration with one line inside the tree closure and a call chained after it. */
    private static String treeWith(String inClosure, String chained) {
        return "inputs.files(fileTree(rootDir) {\n    exclude(\".git\")\n    " + inClosure + "\n}" + chained
                + ", rootProject.files(\".gitignore\"))";
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
            int after = endOfCommentOrLiteral(script, i);
            if (after > i) {
                i = after;
                continue;
            }
            char c = script.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i + 1;
            }
            i++;
        }
        throw new IllegalStateException("unbalanced braces in tasks.test");
    }

    /** The script without its comments; strings stay whole, so a {@code //} inside one is not a comment. */
    private static String withoutComments(String script) {
        StringBuilder code = new StringBuilder();
        int i = 0;
        while (i < script.length()) {
            int after = endOfCommentOrLiteral(script, i);
            if (after == i) {
                code.append(script.charAt(i++));
            } else {
                boolean comment = script.startsWith("//", i) || script.startsWith("/*", i);
                code.append(comment ? " " : script.substring(i, after));
                i = after;
            }
        }
        return code.toString();
    }

    /** Index after the comment, string or char literal that starts at {@code i}; {@code i} when none does. */
    private static int endOfCommentOrLiteral(String script, int i) {
        char c = script.charAt(i);
        if (script.startsWith("//", i)) {
            int eol = script.indexOf('\n', i);
            return eol < 0 ? script.length() : eol;
        }
        if (script.startsWith("/*", i)) {
            int end = script.indexOf("*/", i + 2);
            return end < 0 ? script.length() : end + 2;
        }
        if (script.startsWith("\"\"\"", i)) {
            int end = script.indexOf("\"\"\"", i + 3);
            return end < 0 ? script.length() : end + 3;
        }
        if (c == '"' || c == '\'') {
            int j = i + 1;
            while (j < script.length() && script.charAt(j) != c) {
                j += script.charAt(j) == '\\' ? 2 : 1;
            }
            return Math.min(j + 1, script.length());
        }
        return i;
    }
}
