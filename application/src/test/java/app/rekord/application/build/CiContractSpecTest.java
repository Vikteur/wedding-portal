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
    private static final Pattern PIN_SCRIPT = Pattern.compile("contract-pin\\.sh\\s+gradle\\.properties\\b");
    private static final Pattern COMMENT = Pattern.compile("(^|\\s)#.*$", Pattern.MULTILINE);
    private static final Pattern EXEC_RUN =
            Pattern.compile("^(\\s*RUN(?:\\s+--\\S+)*)\\s*(\\[.*\\])\\s*$", Pattern.MULTILINE);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SEPARATOR = Pattern.compile("&&|\\|\\||[;|]|\\R");
    private static final Pattern GRADLE = Pattern.compile("(.*[/\\\\])?gradlew(\\.bat)?|gradle");
    /** What may stand in front of the wrapper without making it a mere argument. */
    private static final Set<String> PREFIXES = Set.of("RUN", "exec", "sudo", "time", "nohup", "env", "bash", "sh");
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
    void a_gradle_run_behind_a_wrapper_a_shell_keyword_or_the_windows_wrapper_is_checked() throws IOException {
        // Given: each shape with the Gradle part of it as written
        var shapes = List.of(
                List.of("timeout 30m ./gradlew build", "./gradlew build"),
                List.of("timeout -s KILL 30m ./gradlew build", "./gradlew build"),
                List.of("xvfb-run ./gradlew test", "./gradlew test"),
                List.of("xvfb-run -a ./gradlew test", "./gradlew test"),
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
                List.of("gradlew.bat build", "gradlew.bat build"));

        // When / Then
        for (var shape : shapes) {
            JsonNode workflow = build(PIN, CHECKOUT, run(shape.get(0)));
            assertThat(jobsStartingTheApplication("ci.yml", workflow)).as(shape.get(0)).containsExactly("ci.yml: build");
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
                List.of("xvfb-run -s \"-screen 0 1x1x24\" ./gradlew test", "xvfb-run -s -screen 0 1x1x24 ./gradlew test"));

        // When / Then: in a job that starts nothing else, so the refusal does not depend on a starting step
        for (var script : scripts) {
            assertThat(violations(build(run(script.get(0))))).as(script.get(0))
                    .containsExactly("ci.yml job build: `" + script.get(1) + "` names the Gradle wrapper in a shape"
                            + " this test cannot classify; extend CiContractSpecTest to read it");
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
        // Given
        JsonNode workflow = build(
                PIN,
                CHECKOUT.replace("path: contract", "path: ../contract"),
                run("./gradlew build -Pcontract.spec=../contract/dist/openapi.yaml"));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: the contract is checked out to ../contract, outside the workspace");
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
                "chmod +x gradlew",
                "test -f gradlew",
                "echo ./gradlew test",
                "# ./gradlew build",
                "sudo chmod +x gradlew",
                "ls -l gradlew",
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
                .containsExactly("ci.yml job build: Dockerfile"
                        + " `./gradlew --no-daemon :application:quarkusBuild -x test` does not pass -Pcontract.spec");
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
                .containsExactly("ci.yml job build: Dockerfile"
                        + " `./gradlew --no-daemon :application:quarkusBuild -x test` does not pass -Pcontract.spec");
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
    void a_dockerfile_line_that_names_the_wrapper_in_a_shape_the_test_cannot_read_is_refused() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -t x ."));
        String dockerfile = DOCKERFILE + "RUN stdbuf -oL ./gradlew build " + SPEC + "\n";

        // When / Then
        assertThat(violations("ci.yml", workflow, dockerfile))
                .containsExactly("ci.yml job build: Dockerfile `RUN stdbuf -oL ./gradlew build " + SPEC + "` names the"
                        + " Gradle wrapper in a shape this test cannot classify; extend CiContractSpecTest to read it");
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
    void an_image_build_from_another_dockerfile_is_found_rather_than_passed_unread() throws IOException {
        // Given
        JsonNode workflow = build(PIN, CHECKOUT, run("docker build -f other.Dockerfile -t x ."));

        // When / Then
        assertThat(violations(workflow))
                .containsExactly("ci.yml job build: `docker build -f other.Dockerfile -t x .`"
                        + " builds from a Dockerfile this test does not read");
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
                    .containsExactly("ci.yml job build: `" + command + "` builds from a Dockerfile this test does not read");
        }
        for (String command : rootContexts) {
            assertThat(violations(build(PIN, CHECKOUT, run(command)))).as(command).isEmpty();
        }
    }

    @Test
    void a_step_that_builds_through_an_action_is_found() throws IOException {
        // Given: the step as YAML, and the uses: value the message quotes
        var steps = List.of(
                List.of("{uses: 'docker/build-push-action@v6', with: {context: ., push: false}}",
                        "docker/build-push-action@v6"),
                List.of("{uses: 'docker/bake-action@v5'}", "docker/bake-action@v5"),
                List.of("{uses: 'gradle/gradle-build-action@v2', with: {arguments: build}}",
                        "gradle/gradle-build-action@v2"),
                List.of("{uses: ./.github/actions/build}", "./.github/actions/build"),
                List.of("{uses: 'docker://gradle:9'}", "docker://gradle:9"),
                List.of("{uses: 'some-org/some-action@v1'}", "some-org/some-action@v1"));

        // When / Then
        for (var step : steps) {
            assertThat(violations(build(PIN, CHECKOUT, step.get(0)))).as(step.get(1))
                    .containsExactly("ci.yml job build: `uses: " + step.get(1) + "` builds through an action this test"
                            + " does not read; extend CiContractSpecTest to read it");
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
                .containsExactly("ci.yml job build: `uses: ./.github/workflows/build.yml` builds through a reusable"
                        + " workflow this test does not read");
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

    private static final String ON_PUSH = "github.event_name == 'push'";

    private static String when(String condition, String step) {
        return "{if: " + "\"" + condition + "\", " + step.substring(1);
    }

    private static String conditionalPin(String condition) {
        return when(condition, PIN);
    }

    private static String conditionalCheckout(String condition) {
        return "{name: 'Check out rekord-contract', " + when(condition, CHECKOUT).substring(1);
    }

    private static String notShared(String step, String condition) {
        return step + " runs only if `" + condition + "`, a condition the steps that start the application do not share";
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
        return "{run: '" + script + "'}";
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
     * last {@code -Pcontract.spec} is missing or is not {@code <checkout path>/dist/openapi.yaml}.
     */
    static List<String> violations(String file, JsonNode workflow, String dockerfile) {
        List<String> violations = new ArrayList<>();
        for (var job : workflow.path("jobs").properties()) {
            List<JsonNode> steps = Workflows.steps(workflow, job.getKey());
            String where = file + " job " + job.getKey() + ": ";
            if (job.getValue().has("uses")) {
                violations.add(where + "`uses: " + job.getValue().path("uses").asText() + "` builds through a reusable"
                        + " workflow this test does not read");
            }
            for (JsonNode step : steps) {
                if (step.has("uses") && !isKnownSetupAction(step)) {
                    violations.add(where + "`uses: " + step.path("uses").asText() + "` builds through an action this"
                            + " test does not read; extend CiContractSpecTest to read it");
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
                conditionNotShared("the pin step", before.get(pinStep), steps.subList(first, steps.size()), where,
                        violations);
            }
            if (checkout != null) {
                conditionNotShared("the rekord-contract checkout", checkout, steps.subList(first, steps.size()), where,
                        violations);
            }
            for (JsonNode step : steps.subList(first, steps.size())) {
                for (List<String> words : commands(step.path("run").asText(""))) {
                    var gradle = gradleArguments(words);
                    if (gradle.isPresent() && !startedTasks(gradle.get()).isEmpty()) {
                        checkSpec(where, wrapperWord(words, gradle.get()), gradle.get(), path, violations);
                    } else if (buildsImage(words) && namesAnotherDockerfile(words)) {
                        violations.add(where + "`" + String.join(" ", words)
                                + "` builds from a Dockerfile this test does not read");
                    } else if (buildsImage(words)) {
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
            violations.add(where + "`" + String.join(" ", words) + "` names the Gradle wrapper in a shape this test"
                    + " cannot classify; extend CiContractSpecTest to read it");
        }
    }

    /**
     * A command with a word that is the Gradle wrapper, neither run as one (see {@link #gradleArguments}) nor a known
     * mention of it (chmod +x gradlew): the test cannot tell whether it starts the application.
     */
    private static boolean namesTheWrapperUnreadably(List<String> words) {
        return words.stream().anyMatch(word -> GRADLE.matcher(word).matches())
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
            String name = step.path("name").asText(step.path("id").asText(step.path("uses").asText()));
            violations.add(where + what + " `" + name + "` runs only if `" + condition + "`, a condition the steps"
                    + " that start the application do not share");
        }
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
     * The commands of a script, one list of words each: the text after a {@code #} that starts a word dropped, continued
     * lines joined, a Dockerfile RUN in exec form read as its shell form, split at {@code && || ; |}.
     */
    private static List<List<String>> commands(String script) {
        List<List<String>> commands = new ArrayList<>();
        String text = shellForm(COMMENT.matcher(script).replaceAll("$1").replaceAll("\\\\\\R", " "));
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
     * Every Dockerfile {@code RUN [--option ...] ["program", "argument", ...]} as RUN, its options and the JSON words
     * joined by spaces, so {@code ["sh", "-c", "./gradlew build"]} reads as the shell form {@code sh -c ./gradlew build}.
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
            return Matcher.quoteReplacement(run.group(1) + " " + String.join(" ", words));
        });
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
            } else if (!PREFIXES.contains(word) && !KEYWORDS.contains(word) && !word.startsWith("-")
                    && !word.contains("=")) {
                return Optional.empty();
            }
        }
        return Optional.empty();
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
     * An image build that does not read the Dockerfile at the repository root: another one with -f, bake, or a docker
     * build whose context is not exactly one positional word, {@code .} or {@code ./}.
     */
    private static boolean namesAnotherDockerfile(List<String> words) {
        return words.contains("-f")
                || words.contains("--file")
                || words.stream().anyMatch(word -> word.startsWith("--file="))
                || argumentsOf(words, "docker"::equals).map(CiContractSpecTest::buildsFromAnotherContext).orElse(false);
    }

    private static boolean buildsFromAnotherContext(List<String> arguments) {
        List<String> subcommands = arguments.stream().takeWhile(word -> !word.startsWith("-")).limit(3).toList();
        if (subcommands.contains("bake")) {
            return true;
        }
        if (!subcommands.contains("build")) {
            return false;
        }
        List<String> positional = new ArrayList<>();
        List<String> options = arguments.subList(arguments.indexOf("build") + 1, arguments.size());
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

    /** An image build: {@code docker [image|buildx|builder] build}, {@code docker compose up|run}. */
    private static boolean buildsImage(List<String> words) {
        return argumentsOf(words, "docker"::equals)
                .map(arguments -> arguments.stream().takeWhile(word -> !word.startsWith("-")).limit(3).toList())
                .map(subcommands -> subcommands.contains("build") || subcommands.contains("bake")
                        || (subcommands.contains("compose")
                                && (subcommands.contains("up") || subcommands.contains("run"))))
                .orElse(false);
    }
}
