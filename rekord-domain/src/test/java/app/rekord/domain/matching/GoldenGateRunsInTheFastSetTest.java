package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-24.4 AC #7: the gate runs in the normal test task, with no database, and with the fixtures read from the
 * classpath (no file path or URL in {@code GoldenGate}, which a scan of its source checks).
 */
class GoldenGateRunsInTheFastSetTest {

    private static final Path GATE_SOURCE = Path.of("src/test/java/app/rekord/domain/matching/GoldenGate.java");

    private static final List<Class<?>> GATE = List.of(MatcherGoldenSetTest.class, GoldenFixturesTest.class,
            GoldenGateRunsInTheFastSetTest.class);

    @Test
    void no_gate_class_name_ends_in_IT_and_no_gate_class_or_test_is_disabled_or_tagged() {
        for (Class<?> gate : GATE) {
            assertThat(gate.getSimpleName()).doesNotEndWith("IT");
            assertThat(gate.isAnnotationPresent(Disabled.class)).as("%s @Disabled", gate).isFalse();
            assertThat(tagged(gate)).as("%s @Tag", gate).isFalse();
            for (Method test : gate.getDeclaredMethods()) {
                assertThat(test.isAnnotationPresent(Disabled.class)).as("%s @Disabled", test).isFalse();
                assertThat(tagged(test)).as("%s @Tag", test).isFalse();
            }
        }
    }

    private static boolean tagged(AnnotatedElement element) {
        return element.isAnnotationPresent(Tag.class) || element.isAnnotationPresent(Tags.class);
    }

    @Test
    void the_gate_source_is_found_from_the_module_directory(@TempDir Path workingDir) throws IOException {
        // Given the gate source under the module's own src/test (Gradle's working directory for the test task)
        Files.createDirectories(workingDir.resolve("src/test/java/app/rekord/domain/matching"));
        Files.writeString(workingDir.resolve(GATE_SOURCE), "module copy");

        // When it is looked up from that directory
        // Then the module copy is read
        assertThat(gateSource(workingDir)).isEqualTo("module copy");
    }

    @Test
    void the_gate_source_is_found_from_the_repository_root(@TempDir Path workingDir) throws IOException {
        // Given the gate source only under rekord-domain/ (an IDE run from the repository root)
        Path underModule = workingDir.resolve("rekord-domain").resolve(GATE_SOURCE);
        Files.createDirectories(underModule.getParent());
        Files.writeString(underModule, "root copy");

        // When it is looked up from the repository root
        // Then the rekord-domain copy is read
        assertThat(gateSource(workingDir)).isEqualTo("root copy");
    }

    @Test
    void a_directory_with_no_gate_source_fails_instead_of_passing_on_an_empty_read(@TempDir Path workingDir) {
        assertThatThrownBy(() -> gateSource(workingDir)).isInstanceOf(IOException.class);
    }

    /** GoldenGate.java as the test task or an IDE sees it, from the module directory or the repository root. */
    private static String gateSource(Path workingDir) throws IOException {
        Path inModule = workingDir.resolve(GATE_SOURCE);
        return Files.readString(Files.exists(inModule) ? inModule : workingDir.resolve("rekord-domain").resolve(GATE_SOURCE));
    }

    @Test
    void the_fixtures_are_read_from_the_classpath_with_no_file_system_path_or_url() throws IOException {
        String source = gateSource(Path.of(System.getProperty("user.dir")));

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
