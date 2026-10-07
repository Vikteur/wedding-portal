package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class TestJvmTimeZoneTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));

    @Test
    void the_test_jvm_runs_with_user_timezone_utc() {
        // Given the arguments this test JVM was started with
        // Then the zone is set explicitly, so a runner whose OS zone is already UTC cannot hide a broken setting
        assertThat(ManagementFactory.getRuntimeMXBean().getInputArguments()).contains("-Duser.timezone=UTC");
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
    }

    @Test
    void every_test_task_gets_the_utc_jvm_arg() throws IOException {
        // Given the root build script
        String script = Files.readString(REPO_ROOT.resolve("build.gradle.kts"));

        // Then every Test task, including integrationTest, gets the UTC argument
        assertThat(script)
                .containsPattern("tasks\\.withType<Test>\\(\\)\\.configureEach\\s*\\{[^}]*-Duser\\.timezone=UTC");
    }
}
