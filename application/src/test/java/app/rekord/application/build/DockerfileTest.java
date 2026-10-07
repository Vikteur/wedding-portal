package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DockerfileTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final String FAST_JAR = "application/build/quarkus-app/";

    @Test
    void build_stage_runs_the_gradle_wrapper_on_jdk_25_with_tests_skipped() throws IOException {
        // Given
        List<Stage> stages = stages();

        // When
        Stage build = stages.get(0);

        // Then
        assertThat(build.from()).isEqualTo("eclipse-temurin:25-jdk AS build");
        List<String> gradleRuns = build.argsOf("RUN").stream()
                .filter(run -> run.contains("./gradlew"))
                .toList();
        assertThat(gradleRuns).as("RUN instructions invoking ./gradlew").isNotEmpty();
        assertThat(gradleRuns)
                .anySatisfy(run -> assertThat(run)
                        .contains(":application:quarkusBuild")
                        .contains("-x test")
                        .contains("-x integrationTest"));
        assertThat(stages.stream().flatMap(stage -> stage.argsOf("RUN").stream()).toList())
                .noneMatch(run -> run.matches("(?s).*\\bmvnw?\\b.*"));
    }

    @Test
    void runtime_stage_is_the_temurin_25_jre_holding_the_fast_jar_under_app() throws IOException {
        // Given
        List<Stage> stages = stages();

        // When
        Stage runtime = stages.get(stages.size() - 1);

        // Then
        assertThat(runtime.from()).startsWith("eclipse-temurin:25-jre");
        assertThat(runtime.argsOf("WORKDIR")).contains("/app");
        List<String> copies = runtime.argsOf("COPY");
        for (String source : List.of("lib/", "*.jar", "app/", "quarkus/")) {
            assertThat(copies)
                    .as("COPY --from=build of %s", source)
                    .anySatisfy(copy -> assertThat(copy.replaceAll("\\s+", " "))
                            .startsWith("--from=build ")
                            .contains(" /build/" + FAST_JAR + source + " ")
                            .containsPattern(" /app/\\S*$"));
        }
    }

    @Test
    void runtime_runs_as_uid_10001_with_the_java_opts_port_and_entrypoint() throws IOException {
        // Given
        Stage runtime = lastStage();

        // When
        List<Instruction> instructions = runtime.instructions();

        // Then
        String runs = String.join("\n", runtime.argsOf("RUN"));
        assertThat(runs).containsPattern("groupadd\\b[^\\n]*--gid 10001");
        assertThat(runs).containsPattern("useradd\\b[^\\n]*--uid 10001");

        List<String> users = runtime.argsOf("USER");
        assertThat(users).isNotEmpty();
        assertThat(users.get(users.size() - 1)).isEqualTo("10001:10001");
        int lastRun = lastIndexOf(instructions, "RUN");
        int lastUser = lastIndexOf(instructions, "USER");
        assertThat(lastUser).as("USER comes after every RUN").isGreaterThan(lastRun);

        assertThat(String.join("\n", runtime.argsOf("ENV")))
                .contains("JAVA_OPTS=\"-XX:MaxRAMPercentage=70 -Duser.timezone=UTC\"");
        assertThat(runtime.argsOf("EXPOSE")).contains("8080");
        assertThat(runtime.argsOf("ENTRYPOINT"))
                .containsExactly("[\"sh\", \"-c\", \"exec java $JAVA_OPTS -jar /app/quarkus-run.jar\"]");
    }

    @Test
    void health_check_curls_api_health_every_30s_with_5s_timeout_40s_start_and_3_retries() throws IOException {
        // Given
        Stage runtime = lastStage();

        // When
        List<String> healthChecks = runtime.argsOf("HEALTHCHECK");

        // Then
        assertThat(healthChecks).hasSize(1);
        assertThat(healthChecks.get(0).replaceAll("\\s+", " "))
                .contains("--interval=30s")
                .contains("--timeout=5s")
                .contains("--start-period=40s")
                .contains("--retries=3")
                .contains("CMD curl -fsS --max-time 3 http://127.0.0.1:8080/api/health");
        assertThat(String.join("\n", runtime.argsOf("RUN"))).containsPattern("apt-get install\\b[^\\n]*\\bcurl\\b");
    }

    @Test
    void dockerignore_denies_by_default_and_lets_in_only_the_build_inputs() throws IOException {
        // Given
        Path file = REPO_ROOT.resolve(".dockerignore");
        assertThat(file).as(".dockerignore at the repo root").isRegularFile();

        // When
        List<String> rules = Files.readAllLines(file).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();

        // Then
        assertThat(rules).first().isEqualTo("*");
        assertThat(rules)
                .contains(
                        "!gradlew",
                        "!gradle/",
                        "!settings.gradle.kts",
                        "!build.gradle.kts",
                        "!*/build.gradle.kts",
                        "!*/src/main/");
        int lastAllow = rules.lastIndexOf("!*/src/main/");
        for (String denied : List.of("build/", "*/build/", ".gradle/")) {
            assertThat(rules.lastIndexOf(denied))
                    .as("%s is re-denied after the allow rules", denied)
                    .isGreaterThan(lastAllow);
        }
    }

    @Test
    void image_check_script_asserts_every_acceptance_criterion() throws IOException {
        // Given
        Path script = REPO_ROOT.resolve(".github/scripts/image-check.sh");
        assertThat(script).as("image check script").isRegularFile();

        // When
        String content = Files.readString(script);

        // Then
        assertThat(content)
                .contains("10001:10001")
                .contains("MaxRAMPercentage=70")
                .contains("8080/tcp")
                .contains("30000000000")
                .contains("40000000000")
                .contains("healthy")
                .contains("/proc/1")
                .contains("/api/health")
                .contains("health-200.json")
                .contains("postgres:17-alpine")
                .contains("DB_URL=jdbc:postgresql://")
                .contains("DB_PASSWORD")
                .contains("pg_isready -h 127.0.0.1")
                .contains("flyway_schema_history")
                .doesNotContainPattern("POSTGRES_PASSWORD=[A-Za-z0-9]");
    }

    private static Stage lastStage() throws IOException {
        List<Stage> stages = stages();
        return stages.get(stages.size() - 1);
    }

    private static int lastIndexOf(List<Instruction> instructions, String keyword) {
        int index = -1;
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i).keyword().equals(keyword)) {
                index = i;
            }
        }
        return index;
    }

    /** Splits the Dockerfile at each FROM; comments are skipped and {@code \}-continued lines are joined. */
    private static List<Stage> stages() throws IOException {
        Path dockerfile = REPO_ROOT.resolve("Dockerfile");
        assertThat(dockerfile).as("Dockerfile at the repo root").isRegularFile();

        List<Stage> stages = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        for (String raw : Files.readAllLines(dockerfile)) {
            String line = raw.strip();
            // Docker drops comment and empty lines, also inside a continued instruction.
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.endsWith("\\")) {
                pending.append(line, 0, line.length() - 1).append(' ');
                continue;
            }
            pending.append(line);
            Instruction instruction = Instruction.parse(pending.toString().strip());
            pending.setLength(0);
            if (instruction.keyword().equals("FROM")) {
                stages.add(new Stage(instruction.args(), new ArrayList<>()));
            } else {
                assertThat(stages).as("%s before the first FROM", instruction.keyword()).isNotEmpty();
                stages.get(stages.size() - 1).instructions().add(instruction);
            }
        }
        assertThat(stages).as("stages in the Dockerfile").hasSizeGreaterThanOrEqualTo(2);
        return stages;
    }

    private record Instruction(String keyword, String args) {
        static Instruction parse(String line) {
            String[] parts = line.split("\\s+", 2);
            return new Instruction(parts[0].toUpperCase(Locale.ROOT), parts.length > 1 ? parts[1] : "");
        }
    }

    private record Stage(String from, List<Instruction> instructions) {
        List<String> argsOf(String keyword) {
            return instructions.stream()
                    .filter(instruction -> instruction.keyword().equals(keyword))
                    .map(Instruction::args)
                    .toList();
        }
    }
}
