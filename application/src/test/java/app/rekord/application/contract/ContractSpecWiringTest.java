package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ContractSpecWiringTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final String SPEC_ARG = "-Pcontract.spec=contract/dist/openapi.yaml";
    private static final Pattern HELP = Pattern.compile("\bhelp\b");

    private static JsonNode workflow() throws IOException {
        return new ObjectMapper(new YAMLFactory()).readTree(Files.readString(REPO_ROOT.resolve(".github/workflows/ci.yml")));
    }

    private static List<JsonNode> steps(JsonNode job) {
        List<JsonNode> steps = new ArrayList<>();
        job.path("steps").forEach(steps::add);
        return steps;
    }

    private static List<String> lines(JsonNode step) {
        return step.path("run").asText("").lines().map(String::trim).toList();
    }

    private static boolean compilingGradle(String line) {
        return line.contains("gradlew") && !HELP.matcher(line).find();
    }

    private static boolean dockerBuild(String line) {
        return line.contains("docker build");
    }

    private static boolean needsContract(JsonNode step) {
        return lines(step).stream().anyMatch(l -> compilingGradle(l) || dockerBuild(l));
    }

    /** A run step's text with folded (`>`) and multi-line scalars joined, to look for a script by name. */
    private static boolean runs(JsonNode step, String script) {
        return step.path("run").asText("").contains(script);
    }

    @Test
    void every_job_that_compiles_or_builds_the_image_holds_the_pinned_contract_first() throws IOException {
        JsonNode jobs = workflow().path("jobs");
        List<String> checked = new ArrayList<>();
        for (var entry : jobs.properties()) {
            List<JsonNode> steps = steps(entry.getValue());
            for (int i = 0; i < steps.size(); i++) {
                if (!needsContract(steps.get(i))) {
                    continue;
                }
                checked.add(entry.getKey());
                List<JsonNode> before = steps.subList(0, i);
                String job = entry.getKey();
                assertThat(before).as(job + ": contract-pin step")
                        .anyMatch(s -> "contract-pin".equals(s.path("id").asText()) && runs(s, "contract-pin.sh"));
                assertThat(before).as(job + ": checkout of rekord-contract")
                        .anyMatch(s -> s.path("uses").asText().startsWith("actions/checkout@")
                                && "Vikteur/rekord-contract".equals(s.path("with").path("repository").asText())
                                && "contract".equals(s.path("with").path("path").asText())
                                && "${{ steps.contract-pin.outputs.ref }}"
                                        .equals(s.path("with").path("ref").asText())
                                && "${{ secrets.CONTRACT_TOKEN }}"
                                        .equals(s.path("with").path("token").asText()));
                assertThat(before).as(job + ": read-only check").anyMatch(s -> runs(s, "contract-read-only-check.sh"));
                assertThat(before).as(job + ": ref check").anyMatch(s -> runs(s, "contract-ref-check.sh"));
                break;
            }
        }
        assertThat(checked).as("jobs that need the contract").contains("build", "image");
    }

    @Test
    void every_compiling_gradle_invocation_in_ci_passes_the_contract_spec() throws IOException {
        int compiling = 0;
        for (var job : workflow().path("jobs").properties()) {
            for (JsonNode step : steps(job.getValue())) {
                for (String line : lines(step)) {
                    if (compilingGradle(line)) {
                        compiling++;
                        assertThat(line).as(job.getKey() + ": " + line).contains(SPEC_ARG);
                    }
                }
            }
        }
        assertThat(compiling).isPositive();
    }

    @Test
    void the_dockerfile_build_line_passes_the_spec_from_the_checkout() throws IOException {
        String dockerfile = Files.readString(REPO_ROOT.resolve("Dockerfile"));

        assertThat(dockerfile).containsPattern("(?m)gradlew[^\n]*quarkusBuild[^\n]*" + Pattern.quote(SPEC_ARG));
    }

    @Test
    void dockerignore_lets_in_the_bundle_and_nothing_else_under_contract() throws IOException {
        List<String> contractLines = Files.readAllLines(REPO_ROOT.resolve(".dockerignore")).stream()
                .map(String::trim)
                .filter(l -> l.startsWith("!contract") || l.startsWith("contract"))
                .toList();

        assertThat(contractLines).containsExactly("!contract/dist/openapi.yaml");
    }

    @Test
    void the_token_never_reaches_the_image_build() throws IOException {
        JsonNode image = workflow().path("jobs").path("image");
        List<JsonNode> steps = steps(image);
        assertThat(steps).anyMatch(s -> lines(s).stream().anyMatch(ContractSpecWiringTest::dockerBuild));
        for (JsonNode step : steps) {
            if (lines(step).stream().anyMatch(ContractSpecWiringTest::dockerBuild)) {
                assertThat(step.toString()).as("docker build step").doesNotContain("secrets.");
            }
        }
        assertThat(Files.readAllLines(REPO_ROOT.resolve("Dockerfile")))
                .filteredOn(l -> l.stripLeading().matches("(?i)(ARG|ENV)\b.*"))
                .noneMatch(l -> l.toLowerCase().contains("token"));
    }
}
