package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArchUnitPinTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    @Test
    void the_catalog_pins_archunit_junit5_with_a_fixed_version() throws IOException {
        String catalog = Files.readString(REPO_ROOT.resolve("gradle/libs.versions.toml"));

        assertThat(catalog).containsPattern("(?m)^archunit\\s*=\\s*\"\\d+\\.\\d+\\.\\d+\"");
        assertThat(catalog).containsPattern("(?m)^archunit-junit5\\s*=\\s*\\{[^}]*"
                + "module\\s*=\\s*\"com\\.tngtech\\.archunit:archunit-junit5\"[^}]*version\\.ref\\s*=\\s*\"archunit\"");
    }

    @Test
    void application_declares_archunit_junit5_as_a_test_dependency() throws IOException {
        assertThat(Files.readString(REPO_ROOT.resolve("application/build.gradle.kts")))
                .contains("testImplementation(libs.archunit.junit5)");
    }
}
