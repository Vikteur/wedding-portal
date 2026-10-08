package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** TASK-24.4 AC #7: the gate runs in the normal test task, with no database and no network. */
class GoldenGateRunsInTheFastSetTest {

    private static final List<Class<?>> GATE =
            List.of(MatcherGoldenSetTest.class, GoldenGateRunsInTheFastSetTest.class);

    @Test
    void neither_gate_class_name_ends_in_IT_and_neither_is_disabled_or_tagged() {
        for (Class<?> gate : GATE) {
            assertThat(gate.getSimpleName()).doesNotEndWith("IT");
            assertThat(gate.isAnnotationPresent(Disabled.class)).as("%s @Disabled", gate).isFalse();
            assertThat(gate.isAnnotationPresent(Tag.class)).as("%s @Tag", gate).isFalse();
        }
    }

    @Test
    void the_fixtures_are_read_from_the_classpath_with_no_file_system_path_or_url() throws IOException {
        String source = Files.readString(Path.of("src/test/java/app/rekord/domain/matching/GoldenGate.java"));

        assertThat(source).contains("getResourceAsStream");
        assertThat(source).doesNotContain("Paths.get", "Path.of", "new File", "FileInputStream", "new URL",
                "URI.create", "Files.read", "http://", "https://");
    }

    @Test
    void the_test_runtime_has_no_jdbc_driver_and_no_testcontainers() {
        assertThat(DriverManager.drivers().toList()).isEmpty();
        assertThatThrownBy(() -> Class.forName("org.testcontainers.containers.GenericContainer"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
