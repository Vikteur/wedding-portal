package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
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
}
