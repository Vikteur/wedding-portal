package app.rekord.application.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Keeps the comment at the NOT NULL filter of {@link SchemaSnapshot} honest about the versions it was checked on. */
class SchemaSnapshotNotNullCommentTest {

    private static final Path SOURCE = Path.of(System.getProperty("wedding.repoRoot")).resolve(Path.of(
            "application", "src", "test", "java", "app", "rekord", "application", "persistence", "migration",
            "SchemaSnapshot.java"));

    @Test
    void the_comment_at_the_not_null_filter_names_every_postgresql_version_the_snapshot_test_runs_on()
            throws IOException {
        // Given the comment lines directly above the NOT NULL filter of SchemaSnapshot
        List<String> lines = Files.readAllLines(SOURCE);
        int filter = 0;
        while (!lines.get(filter).contains("String NOT_NULL")) {
            filter++;
        }
        Deque<String> comment = new ArrayDeque<>();
        for (int i = filter - 1; i >= 0 && lines.get(i).strip().startsWith("//"); i--) {
            comment.addFirst(lines.get(i).strip());
        }
        String text = String.join(" ", comment).toLowerCase(Locale.ROOT);

        // Then it says it was checked, and names the major version of every image SchemaSnapshotIT runs the NOT NULL test on
        assertThat(text).as("comment above the NOT NULL filter").contains("checked on");
        for (String image : SchemaSnapshotIT.NOT_NULL_IMAGES) {
            String version = image.replaceFirst("^postgres:(\\d+).*$", "$1");
            assertThat(text).as("comment above the NOT NULL filter names %s", image).contains("postgresql " + version);
        }
    }
}
