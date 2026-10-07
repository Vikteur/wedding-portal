package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class CiWorkflowTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

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
        assertThat(gradle.path("run").asText().split("\s+"))
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
    void ci_builds_the_image_and_runs_the_image_check() throws IOException {
        // Given
        JsonNode ci = Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml"));
        String text = Workflows.steps(ci, "image").stream()
                .map(step -> step.path("run").asText())
                .reduce("", (a, b) -> a + "\n" + b);

        // When / Then
        assertThat(text).contains("docker build", "image-check.sh");
        assertThat(text).doesNotContain("docker push").doesNotContain("docker login");
        assertThat(texts(ci.path("jobs").path("image").path("needs"))).containsExactly("build");
    }

    @Test
    void ci_result_is_read_by_the_commit_sha() throws IOException {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(".archon/scripts/ci-by-sha.sh"));

        // When / Then
        assertThat(script).contains("git rev-parse HEAD", "gh run list", "--commit \"$sha\"");
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
