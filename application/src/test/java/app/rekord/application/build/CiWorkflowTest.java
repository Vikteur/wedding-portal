package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CiWorkflowTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Pattern REGISTRY_COMMAND = Pattern.compile("docker (image )?push|docker login|--push\\b");
    private static final Pattern REGISTRY_ACTION = Pattern.compile("^docker/(login-action|build-push-action)@");

    @Test
    void every_action_in_every_workflow_is_pinned_by_a_40_character_commit_sha() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);
        int seen = 0;

        // When / Then
        assertThat(workflows).isNotEmpty();
        for (Path file : workflows) {
            JsonNode workflow = Workflows.read(file);
            seen += Workflows.uses(workflow).size();
            assertThat(Workflows.unpinnedActions(workflow)).as("unpinned actions in %s", file).isEmpty();
        }
        assertThat(seen).as("uses: references seen").isPositive();
    }

    @Test
    void a_tag_or_branch_or_short_sha_reference_counts_as_unpinned() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - uses: actions/checkout@v4
                      - uses: gradle/actions/setup-gradle@main
                      - uses: actions/setup-java@cf277c6
                      - uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4.6.2
                      - uses: ./local-action
                """);

        // When
        var unpinned = Workflows.unpinnedActions(workflow);

        // Then
        assertThat(unpinned)
                .containsExactly(
                        "actions/checkout@v4", "gradle/actions/setup-gradle@main", "actions/setup-java@cf277c6");
    }

    @Test
    void ci_runs_on_push_to_main_and_feature_branches_and_on_pull_requests_into_main() throws IOException {
        // Given
        JsonNode on = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml")).path("on");

        // When
        var pushBranches = texts(on.path("push").path("branches"));
        var pullRequestBranches = texts(on.path("pull_request").path("branches"));

        // Then
        assertThat(pushBranches).containsExactlyInAnyOrder("main", "feature/**");
        assertThat(pullRequestBranches).containsExactly("main");
        assertThat(on.has("workflow_dispatch")).isTrue();
    }

    @Test
    void exactly_one_workflow_runs_on_push_and_pull_request() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);

        // When
        var triggering = new ArrayList<Path>();
        for (Path file : workflows) {
            JsonNode on = Workflows.read(file).path("on");
            if (on.has("push") || on.has("pull_request")) {
                triggering.add(file);
            }
        }

        // Then
        assertThat(triggering).hasSize(1);
    }

    @Test
    void no_workflow_uses_pull_request_target() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);

        // When / Then
        for (Path file : workflows) {
            assertThat(Workflows.read(file).path("on").has("pull_request_target"))
                    .as("pull_request_target in %s", file)
                    .isFalse();
        }
    }

    @Test
    void build_job_runs_test_integration_test_and_build_on_temurin_25() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        JsonNode java = steps.stream()
                .filter(step -> step.path("uses").asText().startsWith("actions/setup-java@"))
                .findFirst()
                .orElseThrow();
        JsonNode gradle = steps.stream()
                .filter(step -> "gradle".equals(step.path("id").asText()))
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(java.path("with").path("distribution").asText()).isEqualTo("temurin");
        assertThat(java.path("with").path("java-version").asText()).isEqualTo("25");
        assertThat(gradle.path("run").asText().trim().split("\\s+"))
                .contains("./gradlew", "test", "integrationTest", "build");
    }

    @Test
    void ci_checks_the_required_tasks_and_the_fast_jar() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int gradle = indexOfStep(steps, step -> "gradle".equals(step.path("id").asText()));
        int fastJar = indexOfStep(steps, step -> step.path("run").asText().contains("quarkus-run.jar"));
        String text = steps.stream().map(step -> step.path("run").asText()).reduce("", (a, b) -> a + "\n" + b);

        // Then
        assertThat(text)
                .contains(
                        "help --task test",
                        "help --task integrationTest",
                        "help --task build",
                        ":application:help --task quarkusBuild",
                        "application/build/quarkus-app/quarkus-run.jar",
                        "application/build/quarkus-app/lib",
                        "application/build/quarkus-app/app",
                        "application/build/quarkus-app/quarkus");
        assertThat(gradle).as("the build runs before the fast-jar check").isNotNegative().isLessThan(fastJar);
    }

    @Test
    void image_job_builds_checks_and_then_pushes_the_sha_and_latest_tags() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "image");

        // When
        int build = indexOfStep(steps, step -> step.path("run").asText().contains("docker build"));
        int check = indexOfStep(steps, step -> step.path("run").asText().contains("image-check.sh"));
        int tags = indexOfStep(steps, step -> "image-tags".equals(step.path("id").asText()));
        int signIn = indexOfStep(steps, step -> step.path("run").asText().contains("docker login ghcr.io"));
        int push = indexOfStep(steps, step -> step.path("run").asText().contains("docker push"));

        // Then
        assertThat(build).as("docker build").isNotNegative().isLessThan(check);
        assertThat(check).as("image check").isLessThan(tags);
        assertThat(tags).as("image tags").isLessThan(signIn);
        assertThat(signIn).as("sign-in").isLessThan(push);
        assertThat(steps.get(tags).path("name").asText()).isEqualTo("Image tags");
        assertThat(steps.get(tags).path("run").asText().trim())
                .isEqualTo("bash .github/scripts/image-tags.sh \"${{ github.repository }}\" \"${{ github.sha }}\"");
        var commands = steps.get(push).path("run").asText().lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
        assertThat(commands)
                .containsExactly(
                        "docker tag wedding-portal:ci \"${{ steps.image-tags.outputs.sha }}\"",
                        "docker tag wedding-portal:ci \"${{ steps.image-tags.outputs.latest }}\"",
                        "docker push \"${{ steps.image-tags.outputs.sha }}\"",
                        "docker push \"${{ steps.image-tags.outputs.latest }}\"");
        assertThat(texts(ci.path("jobs").path("image").path("needs"))).containsExactly("build");
    }

    @Test
    void nothing_pushes_outside_the_image_job_on_main() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);

        // When / Then
        for (Path file : workflows) {
            assertThat(registryStepsOutsideTheImageJob(file.getFileName().toString(), Workflows.read(file)))
                    .as("push or sign-in steps outside the image job in %s", file)
                    .isEmpty();
        }
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        assertThat(ci.path("jobs").path("image").path("if").asText())
                .isEqualTo("github.event_name == 'push' && github.ref == 'refs/heads/main'");
        assertThat(pushGateViolations(ci)).isEmpty();
        assertThat(Workflows.steps(ci, "image").stream().filter(step -> step.path("run").asText().contains("docker push")))
                .hasSize(1);
    }

    @Test
    void a_push_by_action_or_by_another_docker_command_outside_the_image_job_is_found() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  release:
                    steps:
                      - name: Plain push
                        run: docker push x
                      - name: Image push
                        run: docker image push x
                      - name: Buildx push
                        run: docker buildx build --push -t x .
                      - name: Login action
                        uses: docker/login-action@0123456789abcdef0123456789abcdef01234567
                      - name: Build-push action
                        uses: docker/build-push-action@0123456789abcdef0123456789abcdef01234567
                      - name: Build only
                        run: docker build -t x .
                  image:
                    steps:
                      - name: Push image
                        run: docker push x
                """);

        // When
        var found = registryStepsOutsideTheImageJob("ci.yml", workflow);

        // Then
        assertThat(found)
                .containsExactly(
                        "release: Plain push",
                        "release: Image push",
                        "release: Buildx push",
                        "release: Login action",
                        "release: Build-push action");
        assertThat(registryStepsOutsideTheImageJob("other.yml", workflow)).contains("image: Push image");
    }

    /** Every step, as {@code job: step}, that signs in to a registry or pushes an image, outside {@code ci.yml}'s image job. */
    private static List<String> registryStepsOutsideTheImageJob(String fileName, JsonNode workflow) {
        List<String> found = new ArrayList<>();
        workflow.path("jobs").fields().forEachRemaining(job -> {
            boolean isImageJobOfCi = "ci.yml".equals(fileName) && "image".equals(job.getKey());
            job.getValue().path("steps").forEach(step -> {
                boolean registry = REGISTRY_COMMAND.matcher(step.path("run").asText()).find()
                        || REGISTRY_ACTION.matcher(step.path("uses").asText()).find();
                if (!isImageJobOfCi && registry) {
                    found.add(job.getKey() + ": " + step.path("name").asText());
                }
            });
        });
        return found;
    }

    @Test
    void a_job_if_with_always_or_a_push_step_with_continue_on_error_would_let_a_red_build_push() throws IOException {
        // Given
        JsonNode alwaysJob = Workflows.parse("""
                jobs:
                  image:
                    if: always() && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Image tags
                        id: image-tags
                        run: echo tags
                      - name: Sign in
                        run: docker login ghcr.io
                      - name: Push image
                        run: docker push x
                """);
        JsonNode lenientPush = Workflows.parse("""
                jobs:
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Image tags
                        id: image-tags
                        run: echo tags
                      - name: Sign in
                        run: docker login ghcr.io
                      - name: Push image
                        continue-on-error: true
                        run: docker push x
                """);
        JsonNode strict = Workflows.parse("""
                jobs:
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Image tags
                        id: image-tags
                        run: echo tags
                      - name: Sign in
                        run: docker login ghcr.io
                      - name: Push image
                        run: docker push x
                      - name: Sign out
                        if: always()
                        run: docker logout ghcr.io
                """);

        // When / Then
        assertThat(pushGateViolations(alwaysJob)).isNotEmpty();
        assertThat(pushGateViolations(lenientPush)).containsExactly("Push image");
        assertThat(pushGateViolations(strict)).isEmpty();
    }

    @Test
    void a_lenient_image_check_or_test_step_would_let_an_unchecked_or_red_build_push() throws IOException {
        // Given
        JsonNode lenientCheck = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - name: Build and test
                        id: gradle
                        run: ./gradlew test
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Build image
                        run: docker build -t wedding-portal:ci .
                      - name: Check image
                        continue-on-error: true
                        run: bash .github/scripts/image-check.sh wedding-portal:ci
                      - name: Push image
                        run: docker push x
                """);
        JsonNode skippedCheck = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - name: Build and test
                        id: gradle
                        run: ./gradlew test
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Build image
                        run: docker build -t wedding-portal:ci .
                      - name: Check image
                        if: github.event_name == 'pull_request'
                        run: bash .github/scripts/image-check.sh wedding-portal:ci
                      - name: Push image
                        run: docker push x
                """);
        JsonNode lenientTests = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - name: Build and test
                        id: gradle
                        continue-on-error: true
                        run: ./gradlew test
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Build image
                        run: docker build -t wedding-portal:ci .
                      - name: Check image
                        run: bash .github/scripts/image-check.sh wedding-portal:ci
                      - name: Push image
                        run: docker push x
                """);
        JsonNode lenientBuildJob = Workflows.parse("""
                jobs:
                  build:
                    continue-on-error: true
                    steps:
                      - name: Build and test
                        id: gradle
                        run: ./gradlew test
                  image:
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                    needs: build
                    steps:
                      - name: Push image
                        run: docker push x
                """);

        // When / Then
        assertThat(pushGateViolations(lenientCheck)).containsExactly("Check image");
        assertThat(pushGateViolations(skippedCheck)).containsExactly("Check image");
        assertThat(pushGateViolations(lenientTests)).containsExactly("build: Build and test");
        assertThat(pushGateViolations(lenientBuildJob)).containsExactly("build job continue-on-error");
    }

    /**
     * What would let the image job push after a red build or a failed image check: a loose job if, continue-on-error
     * on either job or on a build step, or an if or continue-on-error on any image step up to the last push.
     */
    private static List<String> pushGateViolations(JsonNode workflow) {
        List<String> violations = new ArrayList<>();
        JsonNode build = workflow.path("jobs").path("build");
        if (build.has("continue-on-error")) {
            violations.add("build job continue-on-error");
        }
        build.path("steps").forEach(step -> {
            if (step.has("continue-on-error")) {
                violations.add("build: " + step.path("name").asText());
            }
        });
        JsonNode image = workflow.path("jobs").path("image");
        String condition = image.path("if").asText().trim();
        if (condition.contains("always()") || condition.contains("failure()") || condition.contains("cancelled()")) {
            violations.add("job if: " + condition);
        }
        if (image.has("continue-on-error")) {
            violations.add("job continue-on-error");
        }
        var steps = Workflows.steps(workflow, "image");
        int lastPush = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).path("run").asText().contains("docker push")) {
                lastPush = i;
            }
        }
        for (JsonNode step : steps.subList(0, lastPush + 1)) {
            if (step.has("if") || step.has("continue-on-error")) {
                violations.add(step.path("name").asText());
            }
        }
        return violations;
    }

    @Test
    void build_job_checks_the_groma_architecture_map_with_a_pinned_cli() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int node = indexOfStep(steps, step -> step.path("uses").asText().startsWith("actions/setup-node@"));
        int install = indexOfStep(
                steps, step -> step.path("run").asText().trim().matches("npm install -g groma\\.md@\\d+\\.\\d+\\.\\d+"));
        int check = indexOfStep(
                steps, step -> step.path("run").asText().trim().equals("bash .github/scripts/groma-check.sh"));

        // Then
        assertThat(node).as("the Node setup step").isNotNegative().isLessThan(install);
        assertThat(install).as("the pinned groma install step").isLessThan(check);
        assertThat(check).as("the groma check step").isNotNegative();
        assertThat(steps.get(check).has("if")).isFalse();
        assertThat(steps.get(check).has("continue-on-error")).isFalse();
    }

    @Test
    void build_job_runs_the_pre_push_hook_test_after_the_groma_check() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int check = indexOfStep(
                steps, step -> step.path("run").asText().trim().equals("bash .github/scripts/groma-check.sh"));
        int hookTest = indexOfStep(
                steps, step -> step.path("run").asText().trim().equals("bash scripts/test/pre-push.test.sh"));

        // Then
        assertThat(hookTest).as("the pre-push hook test step").isNotNegative().isGreaterThan(check);
        assertThat(steps.get(hookTest).has("if")).isFalse();
        assertThat(steps.get(hookTest).has("continue-on-error")).isFalse();
    }

    @Test
    void build_job_checks_out_rekord_contract_with_the_contract_token_secret() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int checkout = indexOfStep(steps, step -> "Check out rekord-contract".equals(step.path("name").asText()));
        int gradle = indexOfStep(steps, step -> "gradle".equals(step.path("id").asText()));

        // Then
        assertThat(checkout).as("the contract checkout step").isNotNegative();
        JsonNode step = steps.get(checkout);
        assertThat(step.path("uses").asText()).startsWith("actions/checkout@");
        assertThat(step.path("with").path("repository").asText()).isEqualTo("Vikteur/rekord-contract");
        assertThat(step.path("with").path("path").asText()).isEqualTo("contract");
        assertThat(step.path("with").path("token").asText()).isEqualTo("${{ secrets.CONTRACT_TOKEN }}");
        assertThat(step.has("continue-on-error")).isFalse();
        assertThat(checkout).as("the checkout runs before the build").isLessThan(gradle);
    }

    @Test
    void the_workflows_own_token_is_used_only_to_sign_in_to_the_registry() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);
        Pattern fallback = Pattern.compile("\\$\\{\\{[^}]*\\|\\|[^}]*}}");
        int occurrences = 0;

        // When
        assertThat(workflows).isNotEmpty();
        for (Path file : workflows) {
            String text = Files.readString(file);
            assertThat(text).as("%s", file).doesNotContain("GITHUB_TOKEN");
            assertThat(fallback.matcher(text).find()).as("a || fallback in %s", file).isFalse();
            occurrences += text.split("github\\.token", -1).length - 1;
        }
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "image");
        int signIn = indexOfStep(steps, step -> step.path("run").asText().contains("docker login ghcr.io"));

        // Then
        assertThat(occurrences).as("github.token references across all workflows").isEqualTo(1);
        assertThat(signIn).as("the sign-in step in the image job").isNotNegative();
        JsonNode step = steps.get(signIn);
        assertThat(step.path("env").size()).isEqualTo(1);
        String variable = step.path("env").fieldNames().next();
        assertThat(step.path("env").path(variable).asText()).isEqualTo("${{ github.token }}");
        String run = step.path("run").asText();
        assertThat(run).contains("--password-stdin", "$" + variable);
        assertThat(run).doesNotContain("github.token").doesNotContain("secrets.");
        assertThat(run).doesNotContain(" -p ").doesNotContain("--password ");
    }

    @Test
    void only_the_image_job_may_write_packages() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));

        // When / Then
        assertThat(fields(ci.path("permissions"))).containsExactly("contents=read");
        assertThat(fields(ci.path("jobs").path("image").path("permissions")))
                .containsExactlyInAnyOrder("contents=read", "packages=write");
        ci.path("jobs").fields().forEachRemaining(job -> {
            if (!"image".equals(job.getKey())) {
                assertThat(job.getValue().has("permissions"))
                        .as("permissions of job %s", job.getKey())
                        .isFalse();
            }
        });
    }

    @Test
    void the_registry_sign_in_comes_after_the_image_check_and_is_signed_out_always() throws IOException {
        // Given
        var steps = Workflows.steps(Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml")), "image");

        // When
        int check = indexOfStep(steps, step -> step.path("run").asText().contains("image-check.sh"));
        int signIn = indexOfStep(steps, step -> step.path("run").asText().contains("docker login ghcr.io"));
        JsonNode last = steps.get(steps.size() - 1);

        // Then
        assertThat(check).as("the image check").isNotNegative();
        assertThat(signIn).as("the sign-in").isGreaterThan(check);
        assertThat(last.path("run").asText().trim()).isEqualTo("docker logout ghcr.io");
        assertThat(last.path("if").asText().trim()).isEqualTo("always()");
    }

    @Test
    void the_contract_token_is_named_only_by_its_secret_name() throws IOException {
        // Given
        var workflows = Workflows.files(REPO_ROOT);
        int occurrences = 0;

        // When
        for (Path file : workflows) {
            JsonNode workflow = Workflows.read(file);
            occurrences += Files.readString(file).split("secrets\\.", -1).length - 1;
            assertThat(workflow.path("env").toString()).as("workflow env of %s", file).doesNotContain("secrets");
            workflow.path("jobs").forEach(job -> {
                assertThat(job.path("env").toString()).doesNotContain("secrets");
                job.path("steps").forEach(step -> {
                    String run = step.path("run").asText();
                    assertThat(run).doesNotContain("CONTRACT_TOKEN").doesNotContain("secrets");
                    assertThat(step.path("env").toString()).doesNotContain("secrets");
                });
            });
        }

        // Then: one per job that checks out the contract (build and image), nowhere else
        assertThat(occurrences).as("secrets. references across all workflows").isEqualTo(2);
    }

    @Test
    void contract_bundle_is_checked_before_the_build() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int bundle = indexOfStep(steps, step -> "Contract bundle present".equals(step.path("name").asText()));
        int gradle = indexOfStep(steps, step -> "gradle".equals(step.path("id").asText()));

        // Then
        assertThat(bundle).as("the bundle check step").isNotNegative();
        assertThat(steps.get(bundle).path("run").asText()).contains("test -f contract/dist/openapi.yaml");
        assertThat(bundle).isLessThan(gradle);
    }

    @Test
    void build_job_proves_the_contract_token_is_read_only_right_after_the_checkout() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        int checkout = indexOfStep(steps, step -> "Check out rekord-contract".equals(step.path("name").asText()));

        // Then
        assertThat(checkout).as("the contract checkout step").isNotNegative();
        JsonNode probe = steps.get(checkout + 1);
        assertThat(probe.path("name").asText()).isEqualTo("Contract token is read-only");
        assertThat(probe.path("run").asText().trim().split("\\s+"))
                .containsExactly("bash", ".github/scripts/contract-read-only-check.sh", "contract");
        assertThat(probe.has("if")).isFalse();
        assertThat(probe.has("continue-on-error")).isFalse();
    }

    @Test
    void read_only_check_fails_when_the_push_is_accepted_and_accepts_only_a_refusal() throws IOException {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(".github/scripts/contract-read-only-check.sh"));

        // When / Then
        assertThat(script).contains("set -euo pipefail");
        assertThat(script).contains("push --dry-run origin");
        assertThat(script)
                .containsPattern("(?s)if \\[\\[ \\$status -eq 0 \\]\\]; then.*?FAIL: the contract token can push.*?exit 1");
        assertThat(script).contains("grep -Eq '403|denied|Permission'");
        assertThat(script)
                .doesNotContain("CONTRACT_TOKEN")
                .doesNotContain("extraheader")
                .doesNotContain("git config");
    }

    @Test
    void ci_result_is_read_by_the_commit_sha() throws IOException {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(".archon/scripts/ci-by-sha.sh"));

        // When / Then
        assertThat(script).contains("git rev-parse HEAD", "gh run list", "--commit \"$sha\"");
    }

    @Test
    void no_step_runs_after_a_failed_contract_checkout() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        var steps = Workflows.steps(ci, "build");

        // When
        var running = Workflows.stepsRunningAfterFailureOf(ci, "build", "Check out rekord-contract");
        int checkout = indexOfStep(steps, step -> "Check out rekord-contract".equals(step.path("name").asText()));

        // Then
        assertThat(running).isEmpty();
        assertThat(checkout).as("the contract checkout step").isNotNegative();
        assertThat(steps.get(checkout).has("continue-on-error")).isFalse();
        assertThat(ci.path("jobs").path("build").has("continue-on-error")).isFalse();
    }

    @Test
    void every_other_job_needs_the_build_job() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));

        // When / Then
        ci.path("jobs").fields().forEachRemaining(job -> {
            if (!"build".equals(job.getKey())) {
                assertThat(texts(job.getValue().path("needs")))
                        .as("needs of job %s", job.getKey())
                        .contains("build");
            }
        });
    }

    @Test
    void a_job_with_always_failure_or_cancelled_in_its_if_or_a_continue_on_error_need_runs_after_a_failed_need()
            throws IOException {
        // Given jobs that need a job, with and without a status function in their if
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    runs-on: ubuntu-latest
                  flaky:
                    runs-on: ubuntu-latest
                    continue-on-error: true
                  always:
                    needs: build
                    if: always()
                  failure:
                    needs: build
                    if: ${{ failure() }}
                  not_cancelled:
                    needs: build
                    if: ${{ !cancelled() }}
                  after_flaky:
                    needs: flaky
                  on_main:
                    needs: build
                    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
                  on_success:
                    needs: build
                    if: success() && github.event_name == 'push'
                """);

        // When / Then
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "always")).isTrue();
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "failure")).isTrue();
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "not_cancelled")).isTrue();
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "after_flaky")).isTrue();
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "on_main")).isFalse();
        assertThat(Workflows.runsAfterAFailedNeed(workflow, "on_success")).isFalse();
    }

    @Test
    void the_image_job_needs_the_build_job_and_does_not_run_when_it_fails() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));

        // When / Then
        assertThat(texts(ci.path("jobs").path("image").path("needs"))).contains("build");
        assertThat(Workflows.runsAfterAFailedNeed(ci, "image")).isFalse();
    }

    @Test
    void the_build_job_checks_out_the_pushed_commit() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));

        // When
        List<JsonNode> steps = Workflows.steps(ci, "build");

        // Then the first step checks out this repository without a ref, so it builds github.sha
        assertThat(steps).isNotEmpty();
        assertThat(steps.get(0).path("uses").asText()).startsWith("actions/checkout@");
        assertThat(steps.get(0).path("with").has("ref")).isFalse();
        assertThat(steps.get(0).path("with").has("repository")).isFalse();

        // And the gradle step runs test and integrationTest
        JsonNode gradle = steps.stream()
                .filter(step -> "gradle".equals(step.path("id").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the build job has no step with id 'gradle'"));
        assertThat(gradle.path("run").asText()).contains(" test ").contains(" integrationTest ");
    }

    @Test
    void a_later_step_with_always_or_bare_failure_would_run_after_the_checkout_fails() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - name: Check out rekord-contract
                        uses: actions/checkout@v4
                      - name: Plain
                        run: echo plain
                      - name: Always
                        if: always()
                        run: echo always
                      - name: Failure
                        if: failure()
                        run: echo failure
                      - name: Not cancelled
                        if: ${{ !cancelled() }}
                        run: echo not-cancelled
                      - name: Build
                        id: gradle
                        run: ./gradlew build
                      - name: Upload
                        if: failure() && steps.gradle.outcome == 'failure'
                        run: echo upload
                """);

        // When
        var running = Workflows.stepsRunningAfterFailureOf(workflow, "build", "Check out rekord-contract");

        // Then
        assertThat(running.stream().map(step -> step.path("name").asText()))
                .containsExactly("Always", "Failure", "Not cancelled");
    }

    @Test
    void continue_on_error_on_the_checkout_would_hide_its_failure() throws IOException {
        // Given
        JsonNode workflow = Workflows.parse("""
                jobs:
                  build:
                    steps:
                      - name: Check out rekord-contract
                        continue-on-error: true
                        uses: actions/checkout@v4
                      - name: Build
                        run: ./gradlew build
                """);

        // When
        var running = Workflows.stepsRunningAfterFailureOf(workflow, "build", "Check out rekord-contract");

        // Then
        assertThat(running.stream().map(step -> step.path("name").asText()))
                .containsExactly("Check out rekord-contract");
    }

    private static List<String> fields(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fields().forEachRemaining(field -> fields.add(field.getKey() + "=" + field.getValue().asText()));
        return fields;
    }

    private static List<String> texts(JsonNode node) {
        List<String> texts = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> texts.add(item.asText()));
        } else if (node.isTextual()) {
            texts.add(node.asText());
        }
        return texts;
    }

    private static int indexOfStep(List<JsonNode> steps, Predicate<JsonNode> match) {
        for (int i = 0; i < steps.size(); i++) {
            if (match.test(steps.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
