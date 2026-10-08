package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * TASK-2.4 (AC 9): every CI job that starts the application checks out the rekord-contract ref the build file pins
 * and passes {@code -Pcontract.spec} pointing into that checkout, so no job depends on a sibling path CI lacks.
 * "Starts the application" is every Gradle run that is not a diagnostic (test, integrationTest, build, check,
 * quarkusBuild, ...) and every image build, whose Dockerfile is read for the Gradle lines it runs.
 *
 * <p>Fails closed, as {@code HubProbeParityTest} does: a shape the test can read is checked, and any other shape that
 * could start the application (a command that names the wrapper in a form it cannot classify, an image build from
 * another context, a {@code uses:} action or reusable workflow it does not know, a pin or checkout step whose
 * {@code if:} the build steps do not share) is reported with a request to extend the test. It never passes silently.
 *
 * <p>Complements {@code ContractSpecWiringTest}, which pins the exact lines of {@code ci.yml}; this reads every
 * workflow by job, derives the expected spec path from the checkout step instead of repeating it, and proves on
 * fixtures that each way of leaving the spec out is found.
 */
class CiContractSpecTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    private static final String SPEC_FILE = "dist/openapi.yaml";
    private static final String SPEC_PROPERTY = "-Pcontract.spec=";
    /** What every refusal ends with: a shape the test does not read is reported, never passed. */
    private static final String EXTEND = "extend CiContractSpecTest to read it";
    private static final Pattern PIN_SCRIPT = Pattern.compile("contract-pin\\.sh\\s+gradle\\.properties\\b");
    /** A Dockerfile RUN in exec form; instructions are case-insensitive. */
    private static final Pattern EXEC_RUN = Pattern.compile(
            "^(\\s*RUN(?:\\s+--\\S+)*)\\s*(\\[.*\\])\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper JSON = new ObjectMapper();
    /** Where one command ends: not the {@code &} of {@code 2>&1}, {@code >&2} or {@code &>}. */
    private static final Pattern SEPARATOR = Pattern.compile("&&|\\|\\||(?<![<>])&(?!>)|[;|]|\\R");
    /** A word that is the Gradle wrapper or gradle, bare or under a path. */
    private static final Pattern GRADLE = Pattern.compile("(.*[/\\\\])?(gradlew|gradle)(\\.bat)?");
    /**
     * The wrapper or gradle inside a longer word, as in {@code (gradle}, {@code $(gradle} or {@code [./gradlew,}: not
     * the tail of a name ({@code services.gradle}, {@code setup-gradle}) or the head of one ({@code gradle.properties},
     * {@code gradle-wrapper}, {@code gradle/libs}). Used only to refuse, never to read a command.
     */
    private static final Pattern GRADLE_IN_WORD =
            Pattern.compile("(?<![\\w.\\-:])(?:gradlew(?:\\.bat)?|gradle)(?![\\w.\\-/:])");
    /**
     * What may stand in front of the wrapper without making it a mere argument. RUN, the Dockerfile instruction, is
     * also one in any case (see {@link #isPrefix}).
     */
    private static final Set<String> PREFIXES = Set.of("exec", "sudo", "time", "nohup", "env", "bash", "sh");
    /** Shell keywords that may stand in front of a command, as in {@code if ./gradlew build; then}. */
    private static final Set<String> KEYWORDS = Set.of("if", "then", "elif", "else", "do", "while", "until", "!");
    /**
     * Actions known not to start the application. Fail closed: any other {@code uses:} is refused until the test names
     * it here or learns to read it. Local ({@code ./...}) and {@code docker://} actions are never known.
     */
    private static final Set<String> SETUP_ACTIONS = Set.of(
            "actions/checkout", "actions/setup-java", "actions/setup-node", "actions/upload-artifact",
            "gradle/actions/setup-gradle", "gradle/gradle-build-action");
    /** Commands that take the wrapper as an argument without running it. */
    private static final Set<String> MENTIONS =
            Set.of("echo", "printf", "chmod", "test", "[", "ls", "cat", "git", "COPY", "ADD");
    /** Wrappers that run the command after them, with the options of theirs that take a value as the next word. */
    private static final Map<String, Set<String>> WRAPPERS = Map.of(
            "timeout", Set.of("-s", "--signal", "-k", "--kill-after"),
            "nice", Set.of("-n"),
            "xvfb-run", Set.of("-n", "-w", "-f", "-e", "-p"));
    private static final Pattern DURATION = Pattern.compile("\\d+(\\.\\d+)?[smhd]?");
    /** Options of docker build that take their value as the next word (--opt=value is one word). */
    private static final Set<String> BUILD_VALUE_OPTIONS = Set.of(
            "-t", "--tag", "--build-arg", "--target", "--platform", "--label", "--secret", "--ssh", "--cache-from",
            "--cache-to", "-o", "--output", "--network", "--progress", "--iidfile", "--metadata-file",
            "--build-context", "--add-host", "--shm-size", "--ulimit", "--builder");
    /**
     * Options that docker, docker compose and docker buildx take in front of their subcommand, with their value as the
     * next word (--opt=value is one word). Not exhaustive: an option missing here, with the flags below, is refused,
     * because its value might be taken for the subcommand.
     */
    private static final Set<String> DOCKER_VALUE_OPTIONS = Set.of(
            "-H", "--host", "-c", "--context", "--config", "-l", "--log-level", "--tlscacert", "--tlscert", "--tlskey",
            "-f", "--file", "-p", "--project-name", "--profile", "--env-file", "--project-directory", "--builder");
    /** Options in front of the docker subcommand that take no value. */
    private static final Set<String> DOCKER_FLAGS =
            Set.of("-D", "--debug", "--tls", "--tlsverify", "--dry-run", "--version", "-v", "--help");
    /** Subcommands that take another subcommand after them. */
    private static final Set<String> DOCKER_GROUPS = Set.of("compose", "buildx", "image", "builder");
    /** Tasks that only report. A bare word after one is another task Gradle runs (help build builds). */
    private static final Set<String> DIAGNOSTIC_TASKS = Set.of(
            "help", "tasks", "projects", "properties", "dependencies", "dependencyInsight", "buildEnvironment",
            "components", "outgoingVariants", "resolvableConfigurations", "javaToolchains", "wrapper");
    /**
     * Gradle options, and options of the diagnostic tasks (help --task test), that take their value as the next word.
     * Not exhaustive: the value of an option missing here reads as a task, so the run counts as starting.
     */
    private static final Set<String> VALUE_OPTIONS = Set.of(
            "-x", "--exclude-task", "-p", "--project-dir", "-b", "--build-file", "-c", "--settings-file", "-g",
            "--gradle-user-home", "-I", "--init-script", "--include-build", "--project-cache-dir",
            "--task", "--group", "--configuration", "--dependency", "--variant", "--gradle-version",
            "--distribution-type", "--distribution-url");

    private static final String PIN =
            "{id: contract-pin, run: 'bash .github/scripts/contract-pin.sh gradle.properties'}";
    private static final String CHECKOUT = "{uses: 'actions/checkout@v4', with: {repository: Vikteur/rekord-contract,"
            + " ref: '${{ steps.contract-pin.outputs.ref }}', path: contract}}";
    private static final String SPEC = "-Pcontract.spec=contract/dist/openapi.yaml";
    private static final String BUILD = "./gradlew test integrationTest build --stacktrace " + SPEC;
    private static final String DOCKERFILE = "FROM x\n"
            + "COPY gradlew settings.gradle.kts build.gradle.kts ./\n"
            + "RUN ./gradlew --no-daemon --version\n"
            + "RUN --mount=type=cache,target=/root/.gradle \\\n"
            + "    ./gradlew --no-daemon :application:quarkusBuild -x test " + SPEC + "\n";

    private static final String NO_PIN = "ci.yml job build: no step before the first one that starts the application"
            + " reads the pin (contract-pin.sh gradle.properties)";
    private static final String NO_CHECKOUT = "ci.yml job build: no step after the pin step and before the first one"
            + " that starts the application checks out rekord-contract to a path of its own at the ref the pin step"
            + " outputs";

    /** What DOCKERFILE reports when its spec argument is taken out. */
    private static final String DOCKERFILE_WITHOUT_SPEC = "ci.yml job build: Dockerfile"
            + " `./gradlew --no-daemon :application:quarkusBuild -x test` does not pass -Pcontract.spec";

    private static final String ON_PUSH = "github.event_name == 'push'";

    @Test
    void every_ci_job_that_starts_the_application_checks_out_the_pinned_contract_and_passes_its_spec()
            throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);
        String dockerfile = Files.readString(REPO_ROOT.resolve("Dockerfile"));
        List<String> started = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        // When
        for (Path file : workflows) {
            String name = file.getFileName().toString();
            JsonNode workflow = Workflows.read(file);
            started.addAll(jobsStartingTheApplication(name, workflow));
            violations.addAll(violations(name, workflow, dockerfile));
        }

        // Then
        assertThat(workflows).isNotEmpty();
        assertThat(started).as("jobs that start the application").contains("ci.yml: build", "ci.yml: image");
        assertThat(violations).isEmpty();
    }

    @Test
    void a_job_with_the_pin_the_checkout_and_the_spec_of_the_checkout_has_no_violation() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run(BUILD));

        // When / Then
        assertThat(jobsStartingTheApplication("ci.yml", workflow)).containsExactly("ci.yml: build");
        assertThat(violations(workflow)).isEmpty();
    }

    @Test
    void a_gradle_run_without_the_spec_argument_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("./gradlew test integrationTest build --stacktrace"));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `./gradlew test integrationTest build --stacktrace`"
                        + " does not pass -Pcontract.spec");
    }

    @Test
    void a_spec_inside_a_comment_is_not_passed() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("./gradlew build # " + SPEC));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `./gradlew build` does not pass -Pcontract.spec");
    }

    @Test
    void a_hash_inside_quotes_does_not_hide_the_gradle_run_after_it() throws IOException {
        // Given: a # starts a comment only at the start of a word outside quotes, so CI still runs the Gradle part
        var scripts = List.of(
                List.of("echo \"Build #1\" && ./gradlew build", "./gradlew build"),
                List.of("echo 'step #2'; ./gradlew build -x test", "./gradlew build -x test"),
                List.of("echo \"say \\\"hi\\\" #3\" && ./gradlew check", "./gradlew check"));

        // When / Then
        for (var script : scripts) {
            assertThat(violations(build(PIN, CHECKOUT, run(script.get(0))))).as(script.get(0))
                    .containsExactly("ci.yml job build: `" + script.get(1) + "` does not pass -Pcontract.spec");
        }
    }

    @Test
    void a_comment_after_quoted_text_is_still_dropped() throws IOException {
        // Given: the quote closes, so the # after the Gradle run starts a comment and its spec is not passed
        JsonNode comment = build(PIN, CHECKOUT, run("echo \"a #b\" && ./gradlew build # " + SPEC));
        JsonNode mention = build(run("echo \"a #b\" # ./gradlew build"));

        // When / Then
        assertThat(violations(comment))
                .containsExactly("ci.yml job build: `./gradlew build` does not pass -Pcontract.spec");
        assertThat(jobsStartingTheApplication("ci.yml", mention)).isEmpty();
        assertThat(violations(mention)).isEmpty();
    }

    @Test
    void a_gradle_run_behind_a_wrapper_a_shell_keyword_or_the_windows_wrapper_is_checked() throws IOException {
        // Given: each shape with the Gradle part of it as written
        var shapes = List.of(
                List.of("timeout 30m ./gradlew build", "./gradlew build"),
                List.of("timeout -s KILL 30m ./gradlew build", "./gradlew build"),
                List.of("xvfb-run ./gradlew test", "./gradlew test"),
                List.of("xvfb-run -a ./gradlew test", "./gradlew test"),
                List.of("timeout -k 5 30m ./gradlew build", "./gradlew build"),
                List.of("timeout --kill-after 5 30m ./gradlew build", "./gradlew build"),
                List.of("nice ./gradlew build", "./gradlew build"),
                List.of("nice -n 10 ./gradlew build", "./gradlew build"),
                List.of("if ./gradlew build; then echo ok; fi", "./gradlew build"),
                List.of("if true; then ./gradlew build; fi", "./gradlew build"),
                List.of("if false; then :; elif ./gradlew build; then :; fi", "./gradlew build"),
                List.of("if false; then :; else ./gradlew build; fi", "./gradlew build"),
                List.of("while ./gradlew build; do break; done", "./gradlew build"),
                List.of("until ./gradlew build; do sleep 1; done", "./gradlew build"),
                List.of("for m in a b; do ./gradlew build; done", "./gradlew build"),
                List.of("! ./gradlew check", "./gradlew check"),
                List.of(".\\gradlew.bat build", ".\\gradlew.bat build"),
                List.of("gradlew.bat build", "gradlew.bat build"),
                List.of("GRADLE_OPTS=-Xmx2g ./gradlew build", "./gradlew build"));

        // When / Then
        for (var shape : shapes) {
            JsonNode workflow = build(PIN, CHECKOUT, run(shape.get(0)));
            assertThat(jobsStartingTheApplication("ci.yml", workflow))
                    .as(shape.get(0))
                    .containsExactly("ci.yml: build");
            assertThat(violations(workflow)).as(shape.get(0))
                    .containsExactly("ci.yml job build: `" + shape.get(1) + "` does not pass -Pcontract.spec");

            JsonNode passing = build(PIN, CHECKOUT, run(shape.get(0).replace(shape.get(1), shape.get(1) + " " + SPEC)));
            assertThat(violations(passing)).as(shape.get(0) + " with the spec").isEmpty();
        }
    }

    @Test
    void a_command_that_names_the_wrapper_in_a_shape_the_test_cannot_read_is_refused() throws IOException {
        // Given: the command as the test reads it, the words joined by one space, the quotes dropped
        var scripts = List.of(
                List.of("sudo -u ci ./gradlew build", "sudo -u ci ./gradlew build"),
                List.of("stdbuf -oL ./gradlew build", "stdbuf -oL ./gradlew build"),
                List.of("parallel ./gradlew ::: build", "parallel ./gradlew ::: build"),
                List.of("xvfb-run -s \"-screen 0 1x1x24\" ./gradlew test",
                        "xvfb-run -s -screen 0 1x1x24 ./gradlew test"));

        // When / Then: in a job that starts nothing else, so the refusal does not depend on a starting step
        for (var script : scripts) {
            assertThat(violations(build(run(script.get(0))))).as(script.get(0))
                    .containsExactly("ci.yml job build: `" + script.get(1) + "` names the Gradle wrapper or `gradle` in"
                            + " a shape this test cannot classify; " + EXTEND);
        }
    }

    @Test
    void a_gradle_installation_run_by_its_path_is_checked() throws IOException {
        // Given: a path in front of gradle is as good as one in front of the wrapper
        JsonNode workflow = build(PIN, CHECKOUT, run("/opt/gradle/bin/gradle build"));

        // When / Then
        assertThat(jobsStartingTheApplication("ci.yml", workflow)).containsExactly("ci.yml: build");
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `/opt/gradle/bin/gradle build` does not pass -Pcontract.spec");
        assertThat(violations(build(PIN, CHECKOUT, run("/opt/gradle/bin/gradle build " + SPEC)))).isEmpty();
    }

    @Test
    void a_single_ampersand_separates_commands() throws IOException {
        // Given: && and the redirections 2>&1 and &> are not separators, so those commands are read whole
        JsonNode background = build(PIN, CHECKOUT, run("echo start & ./gradlew build"));
        JsonNode merged = build(PIN, CHECKOUT, run("./gradlew build 2>&1"));
        JsonNode both = build(PIN, CHECKOUT, run("./gradlew build &> build.log"));

        // When / Then
        assertThat(violations(background))
                .containsExactly("ci.yml job build: `./gradlew build` does not pass -Pcontract.spec");
        assertThat(violations(merged))
                .containsExactly("ci.yml job build: `./gradlew build 2>&1` does not pass -Pcontract.spec");
        assertThat(violations(both))
                .containsExactly("ci.yml job build: `./gradlew build &> build.log` does not pass -Pcontract.spec");
    }

    @Test
    void a_gradle_run_glued_to_punctuation_is_refused_not_passed() throws IOException {
        // Given: no word of these is the wrapper or gradle, but one ends in it
        var scripts = List.of("(gradle build)", "out=$(gradle build)", "`gradle build`");

        // When / Then: in a job that starts nothing else, so the refusal does not depend on a starting step
        for (String script : scripts) {
            assertThat(violations(build(run(script)))).as(script)
                    .containsExactly("ci.yml job build: `" + script + "` names the Gradle wrapper or `gradle` in a"
                            + " shape this test cannot classify; " + EXTEND);
        }
    }

    @Test
    void a_name_that_only_contains_gradle_is_not_refused() throws IOException {
        // Given: gradle as the head or the tail of a longer name is not the command, whatever stands next to it
        var scripts = List.of(
                "rm -rf ~/.gradle", "docker pull ci-gradle", "chown build:gradle /out", "cp gradle.properties /tmp/",
                "unzip gradle-8.5-bin.zip", "mkdir -p gradle/wrapper", "chown gradle:build /out",
                "rm -f mygradle gradles");

        // When / Then
        for (String script : scripts) {
            assertThat(violations(build(run(script)))).as(script).isEmpty();
        }
    }

    @Test
    void a_spec_in_a_sibling_directory_instead_of_the_checkout_is_found() throws IOException {
        // Given
        String sibling = "-Pcontract.spec=../rekord-contract/dist/openapi.yaml";
        JsonNode workflow = build(PIN, CHECKOUT, run("./gradlew test " + sibling));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `./gradlew test " + sibling + "`"
                        + " passes ../rekord-contract/dist/openapi.yaml, not contract/dist/openapi.yaml,"
                        + " the spec in the contract checkout");
    }

    @Test
    void a_spec_outside_the_path_the_contract_was_checked_out_to_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("./gradlew build -Pcontract.spec=spec/dist/openapi.yaml"));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `./gradlew build -Pcontract.spec=spec/dist/openapi.yaml`"
                        + " passes spec/dist/openapi.yaml, not contract/dist/openapi.yaml,"
                        + " the spec in the contract checkout");
    }

    @Test
    void the_last_spec_argument_is_the_one_gradle_uses() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("./gradlew build -Pcontract.spec=x/dist/openapi.yaml " + SPEC));

        // When / Then
        assertThat(violations(workflow)).isEmpty();
    }

    @Test
    void a_job_without_the_contract_checkout_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    @Test
    void a_job_without_the_pin_step_is_found() throws IOException {
        // Given
        JsonNode workflow = build(CHECKOUT, run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_PIN, NO_CHECKOUT);
    }

    @Test
    void a_checkout_that_is_not_at_the_ref_the_pin_step_outputs_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT.replace("'${{ steps.contract-pin.outputs.ref }}'", "main"), run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    @Test
    void a_checkout_without_a_path_of_its_own_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT.replace(", path: contract", ""), run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    @Test
    void a_checkout_of_another_repository_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT.replace("Vikteur/rekord-contract", "Vikteur/other"), run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    @Test
    void a_pin_step_without_an_id_is_found() throws IOException {
        // Given: no id, so no later step can name its output
        JsonNode workflow = build(PIN.replace("id: contract-pin, ", ""), CHECKOUT, run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_PIN, NO_CHECKOUT);
    }

    @Test
    void a_checkout_after_the_step_that_starts_the_application_is_found() throws IOException {
        // Given
        JsonNode workflow = build(run(BUILD), PIN, CHECKOUT);

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_PIN, NO_CHECKOUT);
    }

    @Test
    void a_checkout_before_the_pin_step_is_found() throws IOException {
        // Given: the ref output is still empty when the checkout runs, so actions/checkout takes the default branch
        JsonNode workflow = build(CHECKOUT, PIN, run(BUILD));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    @Test
    void a_checkout_path_outside_the_workspace_is_found() throws IOException {
        // Given: a parent directory, an absolute path on either system, and the workspace itself
        var paths = List.of("../contract", "/tmp/contract", "C:/contract", ".");

        // When / Then
        for (String path : paths) {
            JsonNode workflow = build(
                    PIN,
                    CHECKOUT.replace("path: contract", "path: '" + path + "'"),
                    run("./gradlew build -Pcontract.spec=" + path + "/dist/openapi.yaml"));
            assertThat(violations(workflow)).as(path)
                    .containsExactly("ci.yml job build: the contract is checked out to " + path
                            + ", outside the workspace");
        }
    }

    @Test
    void every_gradle_task_that_is_not_a_diagnostic_starts_the_application() throws IOException {
        // Given
        var scripts = List.of(
                "./gradlew test",
                "./gradlew integrationTest",
                "./gradlew build",
                "./gradlew check",
                "./gradlew :application:quarkusBuild",
                "./gradlew quarkusDev",
                "./gradlew assemble",
                "./gradlew --no-daemon -x test build",
                "./gradlew clean build",
                "./gradlew help build",
                "./gradlew tasks test",
                "./gradlew -q dependencies --configuration runtimeClasspath integrationTest",
                "./gradlew :application:help --task quarkusBuild quarkusBuild",
                "./gradlew javaToolchains build",
                "echo start && ./gradlew test",
                "docker build -t x .",
                "docker buildx build -t x .",
                "docker compose up");

        // When / Then
        for (String script : scripts) {
            assertThat(jobsStartingTheApplication("ci.yml", build(run(script))))
                    .as(script)
                    .containsExactly("ci.yml: build");
        }
    }

    @Test
    void a_diagnostic_or_a_command_that_only_names_the_wrapper_does_not_start_the_application() throws IOException {
        // Given
        var scripts = List.of(
                "./gradlew -q help --task test",
                "./gradlew -q :application:help --task quarkusBuild",
                "./gradlew --version",
                "./gradlew --no-daemon --version",
                "./gradlew tasks",
                "./gradlew tasks --all --group build",
                "./gradlew -q dependencyInsight --dependency jackson --configuration runtimeClasspath",
                "./gradlew help clean",
                "./gradlew clean",
                "docker login ghcr.io",
                "docker run --rm x",
                "docker push x",
                "docker tag a b",
                "docker --version",
                "docker -v",
                "docker --context ci push x",
                "docker --config /tmp/docker login ghcr.io",
                "docker compose -f ci.yml down",
                "docker compose --profile ci pull",
                "docker buildx --builder ci ls",
                "chmod +x gradlew",
                "test -f gradlew",
                "echo ./gradlew test",
                "# ./gradlew build",
                "sudo chmod +x gradlew",
                "ls -l gradlew",
                "cat gradlew",
                "printf %s ./gradlew",
                "[ -x gradlew ]",
                "git update-index --chmod=+x gradlew");

        // When / Then
        for (String script : scripts) {
            JsonNode workflow = build(run(script));
            assertThat(jobsStartingTheApplication("ci.yml", workflow)).as(script).isEmpty();
            assertThat(violations(workflow)).as(script).isEmpty();
        }
    }

    @Test
    void a_continued_or_chained_gradle_command_is_read_whole() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - id: contract-pin
                        run: bash .github/scripts/contract-pin.sh gradle.properties
                      - uses: actions/checkout@v4
                        with:
                          repository: Vikteur/rekord-contract
                          ref: ${{ steps.contract-pin.outputs.ref }}
                          path: contract
                      - run: |
                          echo start
                          ./gradlew test \\
                            integrationTest
                      - run: ./gradlew build && ./gradlew check -Pcontract.spec=contract/dist/openapi.yaml
                      - run: >
                          ./gradlew test
                          -Pcontract.spec=contract/dist/openapi.yaml
                """);

        // When / Then
        assertThat(violations("ci.yml", workflow, DOCKERFILE))
                .containsExactly(
                        "ci.yml job build: `./gradlew test integrationTest` does not pass -Pcontract.spec",
                        "ci.yml job build: `./gradlew build` does not pass -Pcontract.spec");
    }

    @Test
    void an_image_build_whose_dockerfile_runs_gradle_without_the_spec_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = DOCKERFILE.replace(" " + SPEC, "");

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly(DOCKERFILE_WITHOUT_SPEC);
        assertThat(violations("ci.yml", workflow, DOCKERFILE)).isEmpty();
    }

    @Test
    void an_image_build_whose_dockerfile_runs_gradle_in_exec_form_is_checked() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String withoutSpec = """
                FROM x
                RUN ["./gradlew", "--no-daemon", ":application:quarkusBuild", "-x", "test"]
                """;
        String withSpec = """
                FROM x
                RUN ["./gradlew", "--no-daemon", ":application:quarkusBuild", "-x", "test", "-Pcontract.spec=contract/dist/openapi.yaml"]
                """;
        String withMount = """
                FROM x
                RUN --mount=type=cache,target=/root/.gradle ["./gradlew", "build"]
                """;

        // When / Then
        assertThat(violations("ci.yml", workflow, withoutSpec))
                .containsExactly(DOCKERFILE_WITHOUT_SPEC);
        assertThat(violations("ci.yml", workflow, withSpec)).isEmpty();
        assertThat(violations("ci.yml", workflow, withMount))
                .containsExactly("ci.yml job build: Dockerfile `./gradlew build` does not pass -Pcontract.spec");
    }

    @Test
    void an_image_build_whose_dockerfile_runs_gradle_in_exec_form_through_a_shell_is_checked() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = """
                FROM x
                RUN ["sh", "-c", "./gradlew --no-daemon build"]
                """;

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `./gradlew --no-daemon build` does not pass"
                        + " -Pcontract.spec");
        assertThat(violations("ci.yml", workflow, dockerfile.replace("build\"", "build " + SPEC + "\""))).isEmpty();
    }

    @Test
    void a_dockerfile_that_only_copies_or_adds_the_wrapper_has_no_violation() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = DOCKERFILE + "ADD gradlew /tmp/\nCOPY gradlew /tmp/\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile)).isEmpty();
    }

    @Test
    void a_dockerfile_line_that_names_the_wrapper_in_a_shape_the_test_cannot_read_is_refused() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = DOCKERFILE + "RUN stdbuf -oL ./gradlew build " + SPEC + "\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `RUN stdbuf -oL ./gradlew build " + SPEC + "` names the"
                        + " Gradle wrapper or `gradle` in a shape this test cannot classify; " + EXTEND);
    }

    @Test
    void a_lowercase_run_instruction_is_checked_in_exec_and_in_shell_form() throws IOException {
        // Given: Dockerfile instructions are case-insensitive
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String exec = "FROM x\nrun [\"./gradlew\", \"build\"]\n";
        String shell = "FROM x\nrun ./gradlew build\n";

        // When / Then
        for (String dockerfile : List.of(exec, shell)) {
            assertThat(violations("ci.yml", workflow, dockerfile)).as(dockerfile)
                    .containsExactly("ci.yml job build: Dockerfile `./gradlew build` does not pass -Pcontract.spec");
        }
    }

    @Test
    void a_dockerfile_exec_form_that_is_not_json_is_refused_not_passed() throws IOException {
        // Given: Docker reads brackets that are not a JSON array as the shell form, and this test cannot tell what runs
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = "FROM x\nRUN [./gradlew, build]\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `RUN [./gradlew, build]` names the Gradle wrapper or"
                        + " `gradle` in a shape this test cannot classify; " + EXTEND);
    }

    @Test
    void a_spec_inside_a_dockerfile_comment_is_not_passed() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = "FROM x\nRUN ./gradlew --no-daemon build # " + SPEC + "\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `./gradlew --no-daemon build` does not pass"
                        + " -Pcontract.spec");
    }

    @Test
    void a_hash_inside_quotes_in_the_dockerfile_does_not_hide_the_gradle_run_after_it() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = "FROM x\nRUN echo \"step #1\" && ./gradlew build\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `./gradlew build` does not pass -Pcontract.spec");
    }

    @Test
    void a_spec_after_a_hash_inside_an_exec_form_shell_command_is_not_passed() throws IOException {
        // Given: sh -c reads the # in its command string as the start of a comment
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = "FROM x\nRUN [\"sh\", \"-c\", \"./gradlew build # " + SPEC + "\"]\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `./gradlew build` does not pass -Pcontract.spec");
    }

    @Test
    void an_image_build_whose_dockerfile_takes_the_spec_from_a_sibling_path_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = DOCKERFILE.replace(SPEC, "-Pcontract.spec=../rekord-contract/dist/openapi.yaml");

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .singleElement()
                .asString()
                .startsWith("ci.yml job build: Dockerfile `./gradlew --no-daemon :application:quarkusBuild -x test")
                .contains("passes ../rekord-contract/dist/openapi.yaml, not contract/dist/openapi.yaml");
    }

    @Test
    void a_dockerfile_that_misses_the_spec_is_reported_once_per_job_however_many_images_the_job_builds()
            throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."), run("docker build -t y ."));
        String dockerfile = DOCKERFILE.replace(" " + SPEC, "");

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly(DOCKERFILE_WITHOUT_SPEC);
    }

    @Test
    void an_image_build_from_another_dockerfile_is_found_rather_than_passed_unread() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -f other.Dockerfile -t x ."));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `docker build -f other.Dockerfile -t x .`"
                        + " builds from a Dockerfile this test does not read; " + EXTEND);
    }

    @Test
    void an_image_build_from_another_context_is_found_rather_than_checked_against_the_root_dockerfile()
            throws IOException {
        // Given
        var otherContexts = List.of(
                "docker build -t x application",
                "docker buildx build --tag x ./application",
                "docker build -t x -",
                "docker build -t x https://github.com/Vikteur/wedding-portal.git",
                "docker build --frobnicate y -t x .",
                "docker buildx bake",
                "docker bake image",
                "docker compose build");
        var rootContexts = List.of(
                "docker build -t wedding-portal:ci .",
                "docker build --build-arg A=b -t x ./",
                "docker buildx build --platform linux/amd64 --load -t x .");

        // When / Then
        for (String command : otherContexts) {
            assertThat(violations(build(PIN, CHECKOUT, run(command)))).as(command)
                    .containsExactly("ci.yml job build: `" + command + "` builds from a Dockerfile this test does not"
                            + " read; " + EXTEND);
        }
        for (String command : rootContexts) {
            assertThat(violations(build(PIN, CHECKOUT, run(command)))).as(command).isEmpty();
        }
    }

    @Test
    void a_docker_global_option_before_the_subcommand_does_not_hide_an_image_build() throws IOException {
        // Given: image builds that name a docker option the test reads (and its value) in front of the subcommand
        var commands = List.of(
                "docker --context ci build -t x .",
                "docker -H tcp://h:2375 build -t x .",
                "docker --host=tcp://h:2375 build -t x .",
                "docker -D build -t x .",
                "docker --config /tmp/docker buildx build -t x .",
                "docker buildx --builder ci build -t x .",
                "docker image build -t x .");
        String withoutSpec = DOCKERFILE.replace(" " + SPEC, "");

        // When / Then
        for (String command : commands) {
            JsonNode workflow = build(PIN, CHECKOUT, run(command));
            assertThat(jobsStartingTheApplication("ci.yml", workflow)).as(command).containsExactly("ci.yml: build");
            assertThat(violations("ci.yml", workflow, withoutSpec)).as(command)
                    .containsExactly(DOCKERFILE_WITHOUT_SPEC);
            assertThat(violations("ci.yml", workflow, DOCKERFILE)).as(command).isEmpty();
        }
    }

    @Test
    void a_docker_option_the_test_does_not_know_before_the_subcommand_is_refused() throws IOException {
        // Given: in a job that starts nothing else, so the refusal does not depend on a starting step
        var commands = List.of(
                "docker --frobnicate build -t x .",
                "docker -x build -t x .",
                "docker -Htcp://h:2375 build -t x .",
                "docker compose --frobnicate up");

        // When / Then
        for (String command : commands) {
            assertThat(violations(build(run(command)))).as(command)
                    .containsExactly("ci.yml job build: `" + command + "` passes an option this test cannot read before"
                            + " the docker subcommand; " + EXTEND);
        }
    }

    @Test
    void an_image_build_from_another_dockerfile_or_context_is_refused_behind_a_docker_option_too()
            throws IOException {
        // Given
        var commands = List.of(
                "docker --context ci build -t x application",
                "docker buildx --builder ci build --tag x ./application",
                "docker --context ci build -f other.Dockerfile .",
                "docker build -fother.Dockerfile -t x .",
                "docker build -t x -fother.Dockerfile .");

        // When / Then
        for (String command : commands) {
            assertThat(violations(build(PIN, CHECKOUT, run(command)))).as(command)
                    .containsExactly("ci.yml job build: `" + command + "` builds from a Dockerfile this test does not"
                            + " read; " + EXTEND);
        }
    }

    @Test
    void docker_compose_up_and_run_are_refused_like_compose_build_because_the_compose_file_is_not_read()
            throws IOException {
        // Given
        var commands = List.of(
                "docker compose up",
                "docker compose up --build",
                "docker compose run app",
                "docker compose -f ci.yml up --build",
                "docker compose --profile ci up",
                "docker compose build");

        // When / Then
        for (String command : commands) {
            assertThat(violations(build(PIN, CHECKOUT, run(command)))).as(command)
                    .containsExactly("ci.yml job build: `" + command + "` builds from a Dockerfile this test does not"
                            + " read; " + EXTEND);
        }
    }

    @Test
    void an_action_the_test_does_not_know_is_found() throws IOException {
        // Given: the step as YAML, and the uses: value the message quotes
        var steps = List.of(
                List.of("{uses: 'docker/build-push-action@v6', with: {context: ., push: false}}",
                        "docker/build-push-action@v6"),
                List.of("{uses: 'docker/bake-action@v5'}", "docker/bake-action@v5"),
                List.of("{uses: 'gradle/gradle-build-action@v2', with: {arguments: build}}",
                        "gradle/gradle-build-action@v2"),
                List.of("{uses: 'gradle/actions/setup-gradle@v4', with: {arguments: build}}",
                        "gradle/actions/setup-gradle@v4"),
                List.of("{uses: ./.github/actions/build}", "./.github/actions/build"),
                List.of("{uses: 'docker://gradle:9'}", "docker://gradle:9"),
                List.of("{uses: 'some-org/some-action@v1'}", "some-org/some-action@v1"));

        // When / Then
        for (var step : steps) {
            assertThat(violations(build(PIN, CHECKOUT, step.get(0)))).as(step.get(1))
                    .containsExactly("ci.yml job build: `uses: " + step.get(1) + "` is an action this test does not"
                            + " know; add it to SETUP_ACTIONS or " + EXTEND);
        }
    }

    @Test
    void a_reusable_workflow_job_is_found() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    uses: ./.github/workflows/build.yml
                """);

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `uses: ./.github/workflows/build.yml` is a reusable workflow this"
                        + " test does not read; " + EXTEND);
    }

    @Test
    void the_actions_ci_yml_uses_to_set_up_are_not_reported() throws IOException {
        // Given
        String sha = "0123456789abcdef0123456789abcdef01234567";
        JsonNode workflow = build(
                PIN,
                CHECKOUT,
                "{uses: 'actions/checkout@" + sha + "'}",
                "{uses: 'actions/setup-java@" + sha + "', with: {java-version: 25}}",
                "{uses: 'gradle/actions/setup-gradle@" + sha + "'}",
                "{uses: 'actions/setup-node@" + sha + "'}",
                "{uses: 'actions/upload-artifact@" + sha + "', with: {name: x, path: y}}",
                "{uses: 'gradle/gradle-build-action@v2'}",
                run(BUILD));

        // When / Then
        assertThat(violations(workflow)).isEmpty();
    }

    @Test
    void a_pin_step_that_runs_only_on_some_events_is_found() throws IOException {
        // Given
        JsonNode workflow = build(conditionalPin(ON_PUSH), CHECKOUT, run(BUILD));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: " + notShared("the pin step `contract-pin`", ON_PUSH));
    }

    @Test
    void a_contract_checkout_that_runs_only_on_some_events_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, conditionalCheckout(ON_PUSH), run(BUILD));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: "
                        + notShared("the rekord-contract checkout `Check out rekord-contract`", ON_PUSH));
    }

    @Test
    void a_pin_or_checkout_step_whose_failure_the_job_ignores_is_found() throws IOException {
        // Given: the build would run without the pin or the checkout the step failed to make
        String namedCheckout = "{name: 'Check out rekord-contract', " + CHECKOUT.substring(1);
        String expression = "'${{ matrix.experimental }}'";

        // When / Then
        for (String value : List.of("true", expression)) {
            assertThat(violations(build(ignoringFailure(value, PIN), CHECKOUT, run(BUILD)))).as("pin " + value)
                    .containsExactly("ci.yml job build: " + ignoresFailure("the pin step `contract-pin`"));
            assertThat(violations(build(PIN, ignoringFailure(value, namedCheckout), run(BUILD))))
                    .as("checkout " + value)
                    .containsExactly("ci.yml job build: "
                            + ignoresFailure("the rekord-contract checkout `Check out rekord-contract`"));
        }
        assertThat(violations(build(ignoringFailure("false", PIN), ignoringFailure("false", CHECKOUT), run(BUILD))))
                .isEmpty();
    }

    @Test
    void a_conditional_pin_and_checkout_shared_by_the_step_that_starts_the_application_pass() throws IOException {
        // Given
        JsonNode workflow = build(conditionalPin(ON_PUSH), conditionalCheckout(ON_PUSH), when(ON_PUSH, run(BUILD)));

        // When / Then
        assertThat(violations(workflow)).isEmpty();
    }

    @Test
    void a_later_step_that_starts_the_application_without_the_condition_is_found() throws IOException {
        // Given
        JsonNode workflow = build(
                conditionalPin(ON_PUSH), conditionalCheckout(ON_PUSH), when(ON_PUSH, run(BUILD)), run(BUILD));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly(
                        "ci.yml job build: " + notShared("the pin step `contract-pin`", ON_PUSH),
                        "ci.yml job build: "
                                + notShared("the rekord-contract checkout `Check out rekord-contract`", ON_PUSH));
    }

    @Test
    void a_later_step_that_does_not_start_the_application_need_not_share_the_condition() throws IOException {
        // Given: only the steps that start the application are compared, and their conditions as trimmed text
        JsonNode workflow = build(
                conditionalPin(ON_PUSH + " "),
                conditionalCheckout(ON_PUSH + " "),
                when(" " + ON_PUSH, run(BUILD)),
                run("./gradlew -q help --task test"),
                run("echo done"));

        // When / Then
        assertThat(violations(workflow)).isEmpty();
    }

    @Test
    void an_image_build_without_the_contract_checkout_is_found() throws IOException {
        // Given
        JsonNode workflow = build(PIN, run("docker build -t x ."));

        // When / Then
        assertThat(violations(workflow)).containsExactly(NO_CHECKOUT);
    }

    private static JsonNode build(String... steps) throws IOException {
        StringBuilder yaml = new StringBuilder("jobs:\n  build:\n    steps:\n");
        for (String step : steps) {
            yaml.append("      - ").append(step).append('\n');
        }
        return Workflows.parse(yaml.toString());
    }

    private static String run(String script) {
        return "{run: '" + script.replace("'", "''") + "'}";
    }

    private static String when(String condition, String step) {
        return "{if: \"" + condition + "\", " + step.substring(1);
    }

    private static String conditionalPin(String condition) {
        return when(condition, PIN);
    }

    private static String conditionalCheckout(String condition) {
        return "{name: 'Check out rekord-contract', " + when(condition, CHECKOUT).substring(1);
    }

    private static String ignoringFailure(String value, String step) {
        return "{continue-on-error: " + value + ", " + step.substring(1);
    }

    private static String ignoresFailure(String step) {
        return step + " sets continue-on-error, so the build goes on when it fails";
    }

    private static String notShared(String step, String condition) {
        return step + " runs only if `" + condition + "`, a condition the steps that start the application do not"
                + " share";
    }

    private static List<String> violations(JsonNode workflow) {
        return violations("ci.yml", workflow, DOCKERFILE);
    }

    /** Every job, as {@code file: job}, with a step that starts the application. */
    static List<String> jobsStartingTheApplication(String file, JsonNode workflow) {
        List<String> jobs = new ArrayList<>();
        for (var job : workflow.path("jobs").properties()) {
            if (firstStartingStep(Workflows.steps(workflow, job.getKey())) >= 0) {
                jobs.add(file + ": " + job.getKey());
            }
        }
        return jobs;
    }

    /**
     * One line per way a job that starts the application leaves out the pinned contract or its spec: no step before
     * it reading the pin from gradle.properties, no checkout of rekord-contract after that step (before it the ref
     * output is still empty, so the default branch is checked out) to a path of its own at the ref that step outputs,
     * a checkout path outside the workspace, a Gradle run (or the Dockerfile of an image build) whose
     * last {@code -Pcontract.spec} is missing or is not {@code <checkout path>/dist/openapi.yaml}, a pin or checkout
     * step whose {@code if:} the steps that start the application do not share. Fails closed, in every job: a command
     * (or Dockerfile line) that names the wrapper in a shape it cannot classify, an image build from a Dockerfile it
     * does not read, a {@code uses:} action it does not know, and a reusable workflow are reported too.
     */
    static List<String> violations(String file, JsonNode workflow, String dockerfile) {
        List<String> violations = new ArrayList<>();
        for (var job : workflow.path("jobs").properties()) {
            List<JsonNode> steps = Workflows.steps(workflow, job.getKey());
            String where = file + " job " + job.getKey() + ": ";
            if (job.getValue().has("uses")) {
                violations.add(where + "`uses: " + job.getValue().path("uses").asText() + "` is a reusable workflow"
                        + " this test does not read; " + EXTEND);
            }
            for (JsonNode step : steps) {
                if (step.has("uses") && !isKnownSetupAction(step)) {
                    violations.add(where + "`uses: " + step.path("uses").asText() + "` is an action this test does not"
                            + " know; add it to SETUP_ACTIONS or " + EXTEND);
                }
                for (List<String> words : commands(step.path("run").asText(""))) {
                    refuseUnreadable(where, words, violations);
                }
            }
            int first = firstStartingStep(steps);
            if (first < 0) {
                continue;
            }
            List<JsonNode> before = steps.subList(0, first);
            int pinStep = pinStep(before);
            String pin = pinStep < 0 ? null : before.get(pinStep).path("id").asText();
            JsonNode checkout = pin == null ? null : checkoutStep(before.subList(pinStep + 1, before.size()), pin);
            String path = checkout == null ? null : checkout.path("with").path("path").asText().trim();
            if (pin == null) {
                violations.add(where + "no step before the first one that starts the application reads the pin"
                        + " (contract-pin.sh gradle.properties)");
            }
            if (path == null) {
                violations.add(where + "no step after the pin step and before the first one that starts the"
                        + " application checks out rekord-contract to a path of its own at the ref the pin step"
                        + " outputs");
            } else if (!insideTheWorkspace(path)) {
                violations.add(where + "the contract is checked out to " + path + ", outside the workspace");
            }
            if (pin != null) {
                mustRunBeforeTheBuild("the pin step", before.get(pinStep), steps.subList(first, steps.size()), where,
                        violations);
            }
            if (checkout != null) {
                mustRunBeforeTheBuild("the rekord-contract checkout", checkout, steps.subList(first, steps.size()),
                        where, violations);
            }
            boolean dockerfileRead = false;
            for (JsonNode step : steps.subList(first, steps.size())) {
                for (List<String> words : commands(step.path("run").asText(""))) {
                    var gradle = gradleArguments(words);
                    if (gradle.isPresent() && !startedTasks(gradle.get()).isEmpty()) {
                        checkSpec(where, wrapperWord(words, gradle.get()), gradle.get(), path, violations);
                    } else if (buildsImage(words) && namesAnotherDockerfile(words)) {
                        violations.add(where + "`" + String.join(" ", words)
                                + "` builds from a Dockerfile this test does not read; " + EXTEND);
                    } else if (buildsImage(words) && !dockerfileRead) {
                        dockerfileRead = true;
                        for (List<String> line : commands(dockerfile)) {
                            var arguments = gradleArguments(line).filter(a -> !startedTasks(a).isEmpty());
                            arguments.ifPresent(
                                    a -> checkSpec(where + "Dockerfile ", wrapperWord(line, a), a, path, violations));
                            refuseUnreadable(where + "Dockerfile ", line, violations);
                        }
                    }
                }
            }
        }
        return violations;
    }

    private static void checkSpec(String where, String wrapper, List<String> arguments, String path,
            List<String> violations) {
        String command = wrapper + " " + String.join(" ", arguments);
        String spec = null;
        for (String argument : arguments) {
            if (argument.startsWith(SPEC_PROPERTY)) {
                spec = argument.substring(SPEC_PROPERTY.length());
            }
        }
        if (spec == null) {
            violations.add(where + "`" + command + "` does not pass -Pcontract.spec");
        } else if (path != null && !spec.equals(path + "/" + SPEC_FILE)) {
            violations.add(where + "`" + command + "` passes " + spec + ", not " + path + "/" + SPEC_FILE
                    + ", the spec in the contract checkout");
        }
    }

    /** A step that uses one of the {@link #SETUP_ACTIONS}; the Gradle ones only when they get no arguments to run. */
    private static boolean isKnownSetupAction(JsonNode step) {
        String name = step.path("uses").asText();
        name = name.substring(0, name.contains("@") ? name.indexOf('@') : name.length());
        boolean runsGradle = name.startsWith("gradle/") && step.path("with").has("arguments");
        return SETUP_ACTIONS.contains(name) && !runsGradle;
    }

    private static void refuseUnreadable(String where, List<String> words, List<String> violations) {
        if (namesTheWrapperUnreadably(words)) {
            violations.add(where + "`" + String.join(" ", words) + "` names the Gradle wrapper or `gradle` in a shape"
                    + " this test cannot classify; " + EXTEND);
        }
        if (passesAnUnreadableDockerOption(words)) {
            violations.add(where + "`" + String.join(" ", words) + "` passes an option this test cannot read before"
                    + " the docker subcommand; " + EXTEND);
        }
    }

    /**
     * A command with a word that is the Gradle wrapper, neither run as one (see {@link #gradleArguments}) nor a known
     * mention of it (chmod +x gradlew): the test cannot tell whether it starts the application.
     */
    private static boolean namesTheWrapperUnreadably(List<String> words) {
        return words.stream().anyMatch(word -> GRADLE.matcher(word).matches() || GRADLE_IN_WORD.matcher(word).find())
                && gradleArguments(words).isEmpty()
                && argumentsOf(words, MENTIONS::contains).isEmpty();
    }

    private static int firstStartingStep(List<JsonNode> steps) {
        for (int i = 0; i < steps.size(); i++) {
            var commands = commands(steps.get(i).path("run").asText(""));
            if (commands.stream().anyMatch(CiContractSpecTest::startsTheApplication)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean startsTheApplication(List<String> words) {
        return gradleArguments(words).map(arguments -> !startedTasks(arguments).isEmpty()).orElse(false)
                || buildsImage(words);
    }

    /** The index, among the steps given, of the one with an id that reads the pin from gradle.properties, or -1. */
    private static int pinStep(List<JsonNode> steps) {
        for (int i = 0; i < steps.size(); i++) {
            JsonNode step = steps.get(i);
            if (!step.path("id").asText("").isBlank() && PIN_SCRIPT.matcher(step.path("run").asText("")).find()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A pin or checkout step with an {@code if:} still counts as present, but it must be reported unless every step
     * from the first one that starts the application on has the same condition: a build could run without it.
     */
    private static void conditionNotShared(String what, JsonNode step, List<JsonNode> fromTheFirstBuild, String where,
            List<String> violations) {
        String condition = step.path("if").asText("").trim();
        boolean shared = fromTheFirstBuild.stream()
                .filter(later -> commands(later.path("run").asText("")).stream()
                        .anyMatch(CiContractSpecTest::startsTheApplication))
                .allMatch(later -> later.path("if").asText("").trim().equals(condition));
        if (!condition.isEmpty() && !shared) {
            violations.add(where + what + " `" + nameOf(step) + "` runs only if `" + condition + "`, a condition the"
                    + " steps that start the application do not share");
        }
    }

    /** A step with {@code continue-on-error} other than {@code false}: if it fails, the job and the build go on. */
    private static void failureIgnored(String what, JsonNode step, String where, List<String> violations) {
        if (!step.path("continue-on-error").asText("false").trim().equals("false")) {
            violations.add(where + what + " `" + nameOf(step) + "` sets continue-on-error, so the build goes on when"
                    + " it fails");
        }
    }

    private static String nameOf(JsonNode step) {
        return step.path("name").asText(step.path("id").asText(step.path("uses").asText()));
    }

    /** The pin step and the checkout must not be skippable or ignorable while a build runs: both are reported. */
    private static void mustRunBeforeTheBuild(String what, JsonNode step, List<JsonNode> fromTheFirstBuild,
            String where, List<String> violations) {
        conditionNotShared(what, step, fromTheFirstBuild, where, violations);
        failureIgnored(what, step, where, violations);
    }

    /** The step that checks the contract out at the ref the pin step outputs, to a path of its own; null when none. */
    private static JsonNode checkoutStep(List<JsonNode> steps, String pinId) {
        for (JsonNode step : steps) {
            JsonNode with = step.path("with");
            if (step.path("uses").asText("").startsWith("actions/checkout@")
                    && with.path("repository").asText("").endsWith("/rekord-contract")
                    && ("${{ steps." + pinId + ".outputs.ref }}").equals(with.path("ref").asText("").trim())
                    && !with.path("path").asText("").isBlank()) {
                return step;
            }
        }
        return null;
    }

    private static boolean insideTheWorkspace(String path) {
        return !path.startsWith("/")
                && !path.matches("^[A-Za-z]:.*")
                && !path.equals(".")
                && !List.of(path.split("/")).contains("..");
    }

    /**
     * The commands of a script, one list of words each: the text after a {@code #} that starts a word dropped,
     * continued lines joined, a Dockerfile RUN in exec form read as its shell form, split at {@code && || ; |}.
     */
    private static List<List<String>> commands(String script) {
        List<List<String>> commands = new ArrayList<>();
        String text = shellForm(withoutComments(script).replaceAll("\\\\\\R", " "));
        for (String part : SEPARATOR.split(text)) {
            List<String> words = Arrays.stream(part.trim().split("\\s+"))
                    .map(word -> word.replace("\"", "").replace("'", ""))
                    .filter(word -> !word.isEmpty())
                    .toList();
            if (!words.isEmpty()) {
                commands.add(words);
            }
        }
        return commands;
    }

    /**
     * Every Dockerfile {@code RUN [--option ...] ["program", "argument", ...]} as RUN, its options and the JSON
     * words joined by spaces, so {@code ["sh", "-c", "./gradlew build"]} reads as the shell form
     * {@code sh -c ./gradlew build}.
     * A line whose brackets are not a JSON array stays as written.
     */
    private static String shellForm(String script) {
        return EXEC_RUN.matcher(script).replaceAll(run -> {
            List<String> words = new ArrayList<>();
            try {
                JSON.readTree(run.group(2)).forEach(word -> words.add(word.asText()));
            } catch (IOException e) {
                return Matcher.quoteReplacement(run.group());
            }
            return Matcher.quoteReplacement(run.group(1) + " " + withoutComments(String.join(" ", words)));
        });
    }

    /**
     * The script with each shell comment dropped: from a {@code #} that starts a word outside quotes to the end of its
     * line, so {@code echo "Build #1" && ./gradlew build} keeps its build. A quote closes with its own kind, and a
     * backslash escapes the next character outside quotes and inside double ones.
     */
    private static String withoutComments(String script) {
        StringBuilder kept = new StringBuilder();
        char quote = 0;
        boolean comment = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (c == '\n' || c == '\r') {
                comment = false;
            } else if (comment) {
                continue;
            } else if (c == '\\' && quote != '\'' && i + 1 < script.length()) {
                kept.append(c);
                c = script.charAt(++i);
            } else if (quote != 0) {
                quote = c == quote ? 0 : quote;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '#' && (i == 0 || Character.isWhitespace(script.charAt(i - 1)))) {
                comment = true;
                continue;
            }
            kept.append(c);
        }
        return kept.toString();
    }

    /**
     * What follows the Gradle wrapper (or gradle) when the command runs it: it comes first, behind at most a Dockerfile
     * RUN, a shell or an env prefix, their options and variable assignments. Empty for a command that only names the
     * wrapper (COPY gradlew, chmod +x gradlew, test -f gradlew, echo ./gradlew).
     */
    private static Optional<List<String>> gradleArguments(List<String> words) {
        return argumentsOf(words, word -> GRADLE.matcher(word).matches());
    }

    /** The word the Gradle run is started with, as written: the one in front of its arguments. */
    private static String wrapperWord(List<String> words, List<String> arguments) {
        return words.get(words.size() - arguments.size() - 1);
    }

    private static Optional<List<String>> argumentsOf(List<String> words, Predicate<String> program) {
        Set<String> valueOptions = Set.of();
        boolean duration = false;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (program.test(word)) {
                return Optional.of(words.subList(i + 1, words.size()));
            }
            if (WRAPPERS.containsKey(word)) {
                valueOptions = WRAPPERS.get(word);
                duration = word.equals("timeout");
            } else if (valueOptions.contains(word)) {
                i++;
            } else if (duration && DURATION.matcher(word).matches()) {
                duration = false;
            } else if (!isPrefix(word) && !KEYWORDS.contains(word) && !word.startsWith("-") && !word.contains("=")) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static boolean isPrefix(String word) {
        return PREFIXES.contains(word) || word.equalsIgnoreCase("RUN");
    }

    /**
     * The tasks one Gradle run starts: options and their values skipped (a diagnostic's --task value included),
     * project paths dropped, {@code clean} and the diagnostic tasks left out.
     */
    private static List<String> startedTasks(List<String> arguments) {
        List<String> tasks = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (VALUE_OPTIONS.contains(argument)) {
                i++;
            } else if (!argument.startsWith("-")) {
                String task = argument.substring(argument.lastIndexOf(':') + 1);
                if (!DIAGNOSTIC_TASKS.contains(task) && !task.equals("clean")) {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * A docker command cut at its leaf subcommand: {@code docker buildx --debug build -t x .} has the subcommands
     * [buildx, build] and the rest [-t, x, .].
     */
    private record DockerCommand(List<String> subcommands, List<String> rest) {
    }

    /**
     * What follows {@code docker}, cut at its leaf subcommand (compose up, buildx build); empty when an option the
     * test does not know stands in front of the leaf, because its value might be taken for the subcommand.
     */
    private static Optional<DockerCommand> dockerCommand(List<String> arguments) {
        List<String> subcommands = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            String word = arguments.get(i);
            if (DOCKER_VALUE_OPTIONS.contains(word)) {
                i++;
            } else if (DOCKER_FLAGS.contains(word) || (word.startsWith("--") && word.contains("="))) {
                continue;
            } else if (word.startsWith("-")) {
                return Optional.empty();
            } else {
                subcommands.add(word);
                if (!DOCKER_GROUPS.contains(word)) {
                    return Optional.of(new DockerCommand(subcommands, arguments.subList(i + 1, arguments.size())));
                }
            }
        }
        return Optional.of(new DockerCommand(subcommands, List.of()));
    }

    /** The docker command the words run, when it is one and the test reads the options in front of its subcommand. */
    private static Optional<DockerCommand> docker(List<String> words) {
        return argumentsOf(words, "docker"::equals).flatMap(CiContractSpecTest::dockerCommand);
    }

    /** A docker command with an option in front of its subcommand that the test does not know. */
    private static boolean passesAnUnreadableDockerOption(List<String> words) {
        return argumentsOf(words, "docker"::equals).map(arguments -> dockerCommand(arguments).isEmpty()).orElse(false);
    }

    /**
     * An image build: {@code docker [image|buildx|builder] build}, {@code docker [buildx] bake},
     * {@code docker compose build|up|run}.
     */
    private static boolean buildsImage(List<String> words) {
        return docker(words)
                .map(DockerCommand::subcommands)
                .map(subcommands -> subcommands.contains("build") || subcommands.contains("bake")
                        || (subcommands.contains("compose")
                                && (subcommands.contains("up") || subcommands.contains("run"))))
                .orElse(false);
    }

    /**
     * An image build that does not read the Dockerfile at the repository root: another one with -f (or -f<file>),
     * bake, compose (its file names the Dockerfile), or a docker build whose context is not exactly one positional
     * word, {@code .} or {@code ./}.
     */
    private static boolean namesAnotherDockerfile(List<String> words) {
        return words.stream().anyMatch(word -> word.equals("--file") || word.startsWith("--file=")
                        || (word.startsWith("-f") && !word.startsWith("--")))
                || docker(words).map(CiContractSpecTest::buildsFromAnotherContext).orElse(false);
    }

    private static boolean buildsFromAnotherContext(DockerCommand docker) {
        List<String> subcommands = docker.subcommands();
        if (subcommands.contains("bake") || subcommands.contains("compose")) {
            return true;
        }
        if (!subcommands.contains("build")) {
            return false;
        }
        List<String> positional = new ArrayList<>();
        List<String> options = docker.rest();
        for (int i = 0; i < options.size(); i++) {
            String word = options.get(i);
            if (BUILD_VALUE_OPTIONS.contains(word)) {
                i++;
            } else if (word.equals("-") || !word.startsWith("-")) {
                positional.add(word);
            }
        }
        return !(positional.size() == 1 && (positional.get(0).equals(".") || positional.get(0).equals("./")));
    }
}
