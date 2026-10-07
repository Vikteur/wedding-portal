package app.rekord.application.build;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reads GitHub workflow files as YAML so the tests check structure, not text. */
final class Workflows {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern PINNED = Pattern.compile("^[\\w.-]+/[\\w./-]+@[0-9a-f]{40}$");

    private static final Pattern ALLOWED_AFTER_FAILURE =
            Pattern.compile("^failure\\(\\) && steps\\.gradle\\.outcome == 'failure'$");

    private Workflows() {}

    static JsonNode read(Path file) throws IOException {
        return parse(Files.readString(file));
    }

    static JsonNode parse(String yaml) throws IOException {
        return YAML.readTree(yaml);
    }

    static List<Path> files(Path repoRoot) throws IOException {
        Path workflows = repoRoot.resolve(".github/workflows");
        if (!Files.isDirectory(workflows)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(workflows)) {
            return files.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    static List<JsonNode> steps(JsonNode workflow, String job) {
        List<JsonNode> steps = new ArrayList<>();
        workflow.path("jobs").path(job).path("steps").forEach(steps::add);
        return steps;
    }

    /** Every {@code uses:} value that is neither local, a docker image, nor pinned by a 40-character commit sha. */
    static List<String> unpinnedActions(JsonNode workflow) {
        return uses(workflow).stream()
                .filter(use -> !use.startsWith("./") && !use.startsWith("docker://"))
                .filter(use -> !PINNED.matcher(use).matches())
                .toList();
    }

    /**
     * The steps that could still run when the named step fails: the named step itself when it has
     * {@code continue-on-error}, and every later step whose {@code if:} is anything other than the allowed
     * {@code failure() && steps.gradle.outcome == 'failure'} (with step id {@code gradle} after the named step).
     */
    static List<JsonNode> stepsRunningAfterFailureOf(JsonNode workflow, String job, String stepName) {
        List<JsonNode> steps = steps(workflow, job);
        int named = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (stepName.equals(steps.get(i).path("name").asText())) {
                named = i;
                break;
            }
        }
        List<JsonNode> running = new ArrayList<>();
        if (named < 0) {
            return running;
        }
        if (steps.get(named).has("continue-on-error")) {
            running.add(steps.get(named));
        }
        int gradle = -1;
        for (int i = named + 1; i < steps.size(); i++) {
            if ("gradle".equals(steps.get(i).path("id").asText())) {
                gradle = i;
            }
        }
        for (int i = named + 1; i < steps.size(); i++) {
            JsonNode step = steps.get(i);
            if (!step.has("if")) {
                continue;
            }
            boolean allowed = gradle > named
                    && i > gradle
                    && ALLOWED_AFTER_FAILURE.matcher(step.get("if").asText().trim()).matches();
            if (!allowed) {
                running.add(step);
            }
        }
        return running;
    }

    /** Every {@code uses:} value of the job-level and step-level references. */
    static List<String> uses(JsonNode workflow) {
        List<String> uses = new ArrayList<>();
        workflow.path("jobs").forEach(job -> {
            if (job.hasNonNull("uses")) {
                uses.add(job.get("uses").asText());
            }
            job.path("steps").forEach(step -> {
                if (step.hasNonNull("uses")) {
                    uses.add(step.get("uses").asText());
                }
            });
        });
        return uses;
    }
}
