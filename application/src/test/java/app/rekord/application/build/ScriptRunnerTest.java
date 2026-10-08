package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.application.build.ScriptRunner.BashLookup;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

/**
 * TASK-31.7: the one runner of the build tests that run a bash script, and how it finds bash. The runner replaces the
 * copies that ImageTagsTest and ContractPinTest kept.
 */
class ScriptRunnerTest {

    private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static final String PATH_SEPARATOR = File.pathSeparator;

    @TempDir
    Path tmp;

    // ---- #1 one helper: scrubbed environment, exit code, standard output and standard error ----

    @Test
    void a_script_gives_back_its_exit_code_standard_output_and_standard_error() throws Exception {
        // Given
        script("report.sh", "echo out-line\necho err-line >&2\nexit 3\n");

        // When
        var result = ScriptRunner.in(tmp).run("report.sh");

        // Then
        assertThat(result.exit()).isEqualTo(3);
        assertThat(result.stdout().trim()).isEqualTo("out-line");
        assertThat(result.stderr().trim()).isEqualTo("err-line");
    }

    @Test
    void a_relative_script_runs_in_the_working_directory_and_an_absolute_one_runs_as_it_is() throws Exception {
        // Given
        Path script = script("where.sh", "echo ran\n");

        // When
        var relative = ScriptRunner.in(tmp).run("where.sh");
        var absolute = ScriptRunner.in(tmp.getParent()).run(script.toString());

        // Then
        assertThat(relative.stdout().trim()).isEqualTo("ran");
        assertThat(absolute.stdout().trim()).isEqualTo("ran");
    }

    @Test
    void the_arguments_reach_the_script_one_by_one_including_one_with_a_space_and_an_empty_one() throws Exception {
        // Given
        script("args.sh", "printf '[%s]' \"$@\"\necho \" n=$#\"\n");

        // When
        var result = ScriptRunner.in(tmp).run("args.sh", "a", "b c", "");

        // Then
        assertThat(result.stdout().trim()).isEqualTo("[a][b c][] n=3");
    }

    @Test
    void a_script_that_reads_its_standard_input_reaches_the_end_of_it_and_does_not_wait() throws Exception {
        // Given
        script("stdin.sh", "cat\necho done\n");

        // When
        var result = ScriptRunner.in(tmp).timeout(Duration.ofSeconds(20)).run("stdin.sh");

        // Then
        assertThat(result.exit()).isZero();
        assertThat(result.stdout().trim()).isEqualTo("done");
    }

    @Test
    void a_script_that_prints_far_more_than_a_pipe_holds_does_not_block() throws Exception {
        // Given
        script("flood.sh", "head -c 300000 /dev/zero | tr '\\0' x\nhead -c 300000 /dev/zero | tr '\\0' y >&2\n");

        // When
        var result = ScriptRunner.in(tmp).timeout(Duration.ofSeconds(30)).run("flood.sh");

        // Then
        assertThat(result.exit()).isZero();
        assertThat(result.stdout()).hasSize(300_000);
        assertThat(result.stderr()).hasSize(300_000);
    }

    @Test
    void the_github_variables_of_the_parent_process_are_not_passed_to_the_script() throws Exception {
        // Given: on CI the parent has GITHUB_ACTIONS, GITHUB_REF_NAME and more; elsewhere none of them
        script("github.sh", "echo \"${GITHUB_ACTIONS-unset} ${GITHUB_REF_NAME-unset} ${GITHUB_OUTPUT-unset}\"\n");

        // When
        var result = ScriptRunner.in(tmp).run("github.sh");

        // Then
        assertThat(result.stdout().trim()).isEqualTo("unset unset unset");
    }

    @Test
    void the_scrub_drops_every_github_variable_in_any_case_and_keeps_every_other() {
        // Given
        Map<String, String> environment = new HashMap<>(Map.of(
                "PATH", "/usr/bin",
                "HOME", "/home/x",
                "GITHUBX", "kept",
                "GITHUB_OUTPUT", "/tmp/out",
                "GITHUB_ACTIONS", "true",
                "github_sha", "abc"));

        // When
        ScriptRunner.prepare(environment, Map.of());

        // Then
        assertThat(environment).containsOnlyKeys("PATH", "HOME", "GITHUBX");
    }

    @Test
    void a_variable_the_caller_sets_reaches_the_script_even_when_it_is_a_github_one() throws Exception {
        // Given
        script("set.sh", "echo \"${TASK_FLAG-unset} ${GITHUB_OUTPUT-unset}\"\n");

        // When
        var result = ScriptRunner.in(tmp)
                .with("TASK_FLAG", "on")
                .with(Map.of("GITHUB_OUTPUT", "/some/file"))
                .run("set.sh");

        // Then
        assertThat(result.stdout().trim()).isEqualTo("on /some/file");
    }

    @Test
    void a_variable_the_caller_removes_is_gone_for_the_script_even_after_the_caller_set_it() throws Exception {
        // Given: the cases TASK-31.6 adds to ImageTagsTest run the tag script with GITHUB_OUTPUT unset
        script("unset.sh", "echo \"${TASK_FLAG-unset} ${GITHUB_OUTPUT-unset}\"\n");

        // When
        var result = ScriptRunner.in(tmp)
                .with("TASK_FLAG", "on")
                .with("GITHUB_OUTPUT", "/some/file")
                .without("TASK_FLAG")
                .without("GITHUB_OUTPUT")
                .run("unset.sh");

        // Then
        assertThat(result.stdout().trim()).isEqualTo("unset unset");
    }

    @Test
    void a_runner_is_not_changed_by_the_calls_that_derive_another_from_it() throws Exception {
        // Given: a base runner that a test class could keep in a constant
        script("base.sh", "echo \"${TASK_FLAG-unset}\"\n");
        var base = ScriptRunner.in(tmp);

        // When
        var derived = base.with("TASK_FLAG", "on").timeout(Duration.ofSeconds(5));

        // Then
        assertThat(derived).isNotSameAs(base);
        assertThat(derived.run("base.sh").stdout().trim()).isEqualTo("on");
        assertThat(base.run("base.sh").stdout().trim()).isEqualTo("unset");
    }

    @Test
    void removing_a_variable_works_on_one_the_parent_has_and_the_last_call_for_a_name_wins() {
        // Given
        Map<String, String> environment = new HashMap<>(Map.of("PATH", "/usr/bin", "HOME", "/home/x"));
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("HOME", null);
        changes.put("A", "1");
        changes.put("A", null);
        changes.put("B", null);
        changes.put("B", "2");

        // When
        ScriptRunner.prepare(environment, changes);

        // Then
        assertThat(environment).containsOnly(Map.entry("PATH", "/usr/bin"), Map.entry("B", "2"));
    }

    // ---- #2 a script that never exits is killed after a fixed timeout and the test fails naming both ----

    @Test
    void a_script_that_never_exits_is_killed_and_the_failure_names_the_script_and_the_timeout() {
        // Given
        script("hang.sh", "sleep 600\n");
        var runner = ScriptRunner.in(tmp).timeout(Duration.ofSeconds(2));

        // When / Then
        assertThatThrownBy(() -> runner.run("hang.sh"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("hang.sh")
                .hasMessageContaining("2 seconds")
                .hasMessageContaining("killed");
    }

    @Test
    void the_timeout_failure_carries_what_the_script_had_printed() {
        // Given
        script("chatty.sh", "echo said-on-stdout\necho said-on-stderr >&2\nsleep 600\n");
        var runner = ScriptRunner.in(tmp).timeout(Duration.ofSeconds(3));

        // When / Then
        assertThatThrownBy(() -> runner.run("chatty.sh"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("said-on-stdout")
                .hasMessageContaining("said-on-stderr");
    }

    @Test
    void the_killed_script_and_the_processes_it_started_stop_running() throws Exception {
        // Given: a background loop that appends to a file; the shell only waits for it
        script("spin.sh", "( while true; do echo tick >> ticks.txt; sleep 0.1; done ) &\nwait\n");
        Path ticks = tmp.resolve("ticks.txt");
        var runner = ScriptRunner.in(tmp).timeout(Duration.ofSeconds(3));

        // When
        assertThatThrownBy(() -> runner.run("spin.sh")).isInstanceOf(AssertionError.class);

        // Then: the loop ran, and no longer grows the file
        assertThat(ticks).exists();
        Thread.sleep(500);
        long settled = Files.size(ticks);
        Thread.sleep(700);
        assertThat(settled).as("ticks written while the script ran").isPositive();
        assertThat(Files.size(ticks)).as("ticks written after the kill").isEqualTo(settled);
    }

    @Test
    void the_default_timeout_is_finite_and_a_zero_or_negative_one_is_refused() {
        // Then
        assertThat(ScriptRunner.DEFAULT_TIMEOUT).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(2));
        assertThatIllegalArgumentException().isThrownBy(() -> ScriptRunner.in(tmp).timeout(Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> ScriptRunner.in(tmp).timeout(Duration.ofSeconds(-1)));
    }

    // ---- #3 bash is found on the PATH or next to git; the test is skipped only when there is none ----

    @Test
    void on_windows_bash_is_found_next_to_the_git_executable_when_git_bash_is_not_where_it_used_to_be()
            throws IOException {
        // Given: Git for Windows in a folder that is not C:\Program Files\Git; git.exe is in cmd
        touch("Tools/Git/cmd/git.exe");
        Path bash = touch("Tools/Git/bin/bash.exe");
        String path = tmp.resolve("Tools/Git/cmd") + PATH_SEPARATOR + tmp.resolve("Elsewhere");

        // When
        var lookup = BashLookup.search(true, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(bash);
    }

    @Test
    void on_windows_bash_is_found_when_git_exe_is_in_mingw64_bin() throws IOException {
        // Given
        touch("Tools/Git/mingw64/bin/git.exe");
        Path bash = touch("Tools/Git/bin/bash.exe");

        // When
        var lookup = BashLookup.search(true, tmp.resolve("Tools/Git/mingw64/bin").toString(), List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(bash);
    }

    @Test
    void on_windows_a_bash_exe_on_the_path_is_found_when_there_is_no_git_exe() throws IOException {
        // Given
        Path bash = touch("Msys/usr/bin/bash.exe");
        String path = tmp.resolve("Empty") + PATH_SEPARATOR + tmp.resolve("Msys/usr/bin");

        // When
        var lookup = BashLookup.search(true, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(bash);
    }

    @Test
    void on_windows_the_bash_next_to_git_comes_before_another_bash_on_the_path() throws IOException {
        // Given
        touch("Tools/Git/cmd/git.exe");
        Path gitBash = touch("Tools/Git/bin/bash.exe");
        touch("Other/bash.exe");
        String path = tmp.resolve("Other") + PATH_SEPARATOR + tmp.resolve("Tools/Git/cmd");

        // When
        var lookup = BashLookup.search(true, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(gitBash);
    }

    @Test
    void on_windows_the_bash_exe_of_the_windows_folder_is_never_taken_because_it_is_the_wsl_launcher()
            throws IOException {
        // Given: the WSL launcher is first on the PATH; Git Bash is further down
        Path windows = tmp.resolve("Windows");
        Path wsl = touch("Windows/System32/bash.exe");
        Path gitBash = touch("Tools/Git/bin/bash.exe");
        String path = wsl.getParent() + PATH_SEPARATOR + tmp.resolve("Tools/Git/bin");

        // When
        var withBoth = BashLookup.search(true, path, List.of(windows), List.of());
        var withOnlyTheLauncher = BashLookup.search(true, wsl.getParent().toString(), List.of(windows), List.of());

        // Then
        assertThat(withBoth.found()).contains(gitBash);
        assertThat(withOnlyTheLauncher.found()).isEmpty();
        assertThat(withOnlyTheLauncher.notFoundMessage()).contains("WSL").contains(wsl.toString());
    }

    @Test
    void on_windows_a_known_install_folder_is_the_last_place_to_look() throws IOException {
        // Given: nothing usable on the PATH
        Path installed = touch("Program Files/Git/bin/bash.exe");

        // When
        var lookup = BashLookup.search(true, tmp.resolve("Empty").toString(), List.of(), List.of(installed));

        // Then
        assertThat(lookup.found()).contains(installed);
    }

    @Test
    void a_path_with_blank_quoted_or_invalid_entries_is_searched_without_failing() throws IOException {
        // Given
        touch("Tools/Git/cmd/git.exe");
        Path bash = touch("Tools/Git/bin/bash.exe");
        String path = "\0bad" + PATH_SEPARATOR + PATH_SEPARATOR + " " + PATH_SEPARATOR + '"'
                + tmp.resolve("Tools/Git/cmd") + '"';

        // When
        var lookup = BashLookup.search(true, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(bash);
    }

    @Test
    void elsewhere_the_first_bash_on_the_path_is_used() throws IOException {
        // Given: as on CI
        Path first = touchExecutable("first/bash");
        touchExecutable("second/bash");
        String path = tmp.resolve("none") + PATH_SEPARATOR + first.getParent() + PATH_SEPARATOR + tmp.resolve("second");

        // When
        var lookup = BashLookup.search(false, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).contains(first);
    }

    @Test
    void when_there_is_no_bash_the_lookup_says_where_it_looked_and_the_test_is_skipped_not_failed()
            throws IOException {
        // Given: a Windows PATH with a git.exe but no bash, and one known install that does not exist
        touch("Tools/Git/cmd/git.exe");
        Path onPath = tmp.resolve("Tools/Git/cmd");
        Path known = tmp.resolve("Program Files/Git/bin/bash.exe");

        // When
        var lookup = BashLookup.search(true, onPath.toString(), List.of(), List.of(known));

        // Then
        assertThat(lookup.found()).isEmpty();
        assertThat(lookup.looked()).isNotEmpty();
        assertThat(lookup.notFoundMessage())
                .contains("no bash")
                .contains(tmp.resolve("Tools/Git/bin/bash.exe").toString())
                .contains(onPath.resolve("bash.exe").toString())
                .contains(known.toString());
        assertThatThrownBy(lookup::required)
                .isInstanceOf(TestAbortedException.class)
                .hasMessage(lookup.notFoundMessage());
    }

    @Test
    void a_missing_bash_names_the_path_directories_it_looked_in_elsewhere_too() {
        // Given
        String path = tmp.resolve("one") + PATH_SEPARATOR + tmp.resolve("two");

        // When
        var lookup = BashLookup.search(false, path, List.of(), List.of());

        // Then
        assertThat(lookup.found()).isEmpty();
        assertThat(lookup.notFoundMessage())
                .contains(tmp.resolve("one").resolve("bash").toString())
                .contains(tmp.resolve("two").resolve("bash").toString());
    }

    @Test
    void a_runner_whose_bash_is_missing_skips_the_test_instead_of_failing_it() {
        // Given
        script("never.sh", "echo never\n");
        var nowhere = BashLookup.search(false, tmp.resolve("one").toString(), List.of(), List.of());
        var runner = ScriptRunner.in(tmp).using(nowhere);

        // When / Then
        assertThatThrownBy(() -> runner.run("never.sh"))
                .isInstanceOf(TestAbortedException.class)
                .hasMessageContaining("no bash")
                .hasMessageContaining(tmp.resolve("one").resolve("bash").toString());
    }

    @Test
    void on_linux_and_macos_the_bash_of_the_path_is_the_one_used() {
        // Given: CI
        Assumptions.assumeFalse(WINDOWS, "the PATH lookup of a Unix machine");

        // When
        var lookup = BashLookup.ofThisMachine();

        // Then
        assertThat(lookup.found()).hasValueSatisfying(bash -> assertThat(bash.getFileName()).hasToString("bash"));
    }

    @Test
    void on_this_windows_machine_bash_is_a_git_bash_and_never_the_wsl_launcher() {
        // Given
        Assumptions.assumeTrue(WINDOWS, "the Git Bash lookup of a Windows machine");

        // When
        var lookup = BashLookup.ofThisMachine();

        // Then
        Path systemRoot = Path.of(System.getenv("SystemRoot"));
        assertThat(lookup.found())
                .as(lookup.notFoundMessage())
                .hasValueSatisfying(bash -> assertThat(bash).isRegularFile().satisfies(found -> assertThat(
                                found.startsWith(systemRoot))
                        .as("%s under the Windows folder is the WSL launcher", found)
                        .isFalse()));
    }

    private Path script(String name, String content) {
        try {
            return Files.writeString(tmp.resolve(name), content);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private Path touch(String relative) throws IOException {
        Path file = tmp.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.exists(file) ? file : Files.createFile(file);
    }

    private Path touchExecutable(String relative) throws IOException {
        Path file = touch(relative);
        assertThat(file.toFile().setExecutable(true)).as("%s made executable", file).isTrue();
        return file;
    }
}
