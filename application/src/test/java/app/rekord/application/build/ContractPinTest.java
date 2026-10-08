package app.rekord.application.build;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContractPinTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Pattern TAG = Pattern.compile("^v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final String PIN_LINE = "^\\s*rekordContractTag\\s*[=:].*$";
    /**
     * TASK-2.4: the first tag whose smoke/pom.xml has the shape and option set the parity test reads from the
     * checkout (hub commit bee8ce6; the smoke/pom.xml of v0.1.0 predates it).
     */
    private static final String FIRST_TAG_WITH_THE_HUB_PROBE = "v0.2.0";
    private static final String PIN_REF = "${{ steps.contract-pin.outputs.ref }}";
    private static final String PIN = ".github/scripts/contract-pin.sh";
    private static final String MERGE_CHECK = ".github/scripts/contract-pin-merge-check.sh";
    private static final String REF_CHECK = ".github/scripts/contract-ref-check.sh";
    private static final Map<String, String> PUSH_TO_MAIN = Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF_NAME", "main");

    @TempDir
    Path tmp;

    @Test
    void gradle_properties_names_exactly_one_rekord_contract_pin_and_it_is_a_tag_or_a_branch_name()
            throws IOException {
        // Given
        var pins = Files.readAllLines(REPO_ROOT.resolve("gradle.properties")).stream()
                .filter(line -> line.matches(PIN_LINE))
                .toList();

        // When
        String value = pins.isEmpty()
                ? ""
                : pins.get(0).replaceFirst("^\\s*rekordContractTag\\s*[=:]\\s*", "").trim();

        // Then
        assertThat(pins).as("rekordContractTag lines").hasSize(1);
        assertThat(value).as("the pinned tag or branch").matches("^[A-Za-z0-9._/-]+$");
    }

    @Test
    void the_pin_is_not_below_v0_2_0_the_first_tag_with_the_hub_probe_the_parity_test_reads() throws IOException {
        // Given: HubProbeParityTest reads smoke/pom.xml from the pinned checkout, in the shape v0.2.0 first has
        String pin = pinnedValue();
        Assumptions.assumeTrue(
                TAG.matcher(pin).matches(), "a branch pin names no version; the merge check refuses it anyway");

        // When
        int order = compareVersions(pin, FIRST_TAG_WITH_THE_HUB_PROBE);

        // Then
        assertThat(order).as("%s against %s", pin, FIRST_TAG_WITH_THE_HUB_PROBE).isNotNegative();
    }

    @Test
    void tags_compare_by_number_and_not_by_text() {
        assertThat(compareVersions("v0.1.0", "v0.2.0")).isNegative();
        assertThat(compareVersions("v0.2.0", "v0.2.0")).isZero();
        assertThat(compareVersions("v0.10.0", "v0.2.0")).isPositive();
        assertThat(compareVersions("v1.0.0", "v0.99.99")).isPositive();
        assertThat(compareVersions("v0.2.1", "v0.2.0")).isPositive();
    }

    @Test
    void the_spec_the_build_reads_is_the_version_the_pin_names() throws IOException {
        // Given: contract.spec is <checkout>/dist/openapi.yaml; the hub keeps info.version equal to its tag
        String pin = pinnedValue();
        Assumptions.assumeTrue(TAG.matcher(pin).matches(), "a branch pin names no version");
        String spec = System.getProperty("contract.spec");
        assertThat(spec).as("contract.spec system property").isNotBlank();

        // When
        String version = Workflows.parse(Files.readString(Path.of(spec))).path("info").path("version").asText();

        // Then
        assertThat("v" + version)
                .as("info.version of %s against rekordContractTag in gradle.properties", spec)
                .isEqualTo(pin);
    }

    @Test
    void a_branch_name_or_a_tag_without_the_v_is_not_a_valid_tag() {
        assertThat(List.of("main", "master", "0.1.0", "v1", "v1.2", "v01.2.3", "v1.2.3-rc1", "feature/x"))
                .noneMatch(value -> TAG.matcher(value).matches());
        assertThat(List.of("v0.1.0", "v1.20.3", "v10.0.0")).allMatch(value -> TAG.matcher(value).matches());
    }

    @Test
    void ci_checks_out_the_contract_at_the_pinned_ref_after_reading_the_pin() throws IOException {
        // Given
        var steps = Workflows.steps(Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml")), "build");

        // When
        int pin = indexOfStep(steps, step -> "contract-pin".equals(step.path("id").asText()));
        int checkout = indexOfStep(steps, step -> "Check out rekord-contract".equals(step.path("name").asText()));

        // Then
        assertThat(pin).as("the pin step").isNotNegative().isLessThan(checkout);
        assertThat(steps.get(pin).path("run").asText().trim().split("\\s+"))
                .containsExactly("bash", ".github/scripts/contract-pin.sh", "gradle.properties");
        JsonNode with = steps.get(checkout).path("with");
        assertThat(with.path("repository").asText()).isEqualTo("Vikteur/rekord-contract");
        assertThat(with.path("ref").asText()).isEqualTo(PIN_REF);
    }

    @Test
    void ci_verifies_the_checked_out_contract_is_exactly_the_pin_after_the_checkout() throws IOException {
        // Given
        var steps = Workflows.steps(Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml")), "build");

        // When
        int checkout = indexOfStep(steps, step -> "Check out rekord-contract".equals(step.path("name").asText()));
        int verify = indexOfStep(steps, step -> step.path("run").asText().contains("contract-ref-check.sh"));
        int gradle = indexOfStep(steps, step -> "gradle".equals(step.path("id").asText()));

        // Then
        assertThat(verify).as("the exact-ref verification step").isGreaterThan(checkout).isLessThan(gradle);
        assertThat(steps.get(verify).path("run").asText())
                .contains("contract-ref-check.sh contract \"${{ steps.contract-pin.outputs.kind }}\""
                        + " \"${{ steps.contract-pin.outputs.pin }}\"");
        assertThat(steps.get(verify).has("if")).isFalse();
        assertThat(steps.get(verify).has("continue-on-error")).isFalse();
    }

    @Test
    void the_last_build_step_requires_the_pin_to_be_a_tag_without_if_or_continue_on_error() throws IOException {
        // Given
        var steps = Workflows.steps(Workflows.read(REPO_ROOT.resolve(".github/workflows/ci.yml")), "build");

        // When
        JsonNode last = steps.get(steps.size() - 1);

        // Then
        assertThat(last.path("name").asText()).isEqualTo("Contract pin is a tag (required to merge)");
        assertThat(last.path("run").asText().trim())
                .isEqualTo("bash " + MERGE_CHECK + " \"${{ steps.contract-pin.outputs.kind }}\"");
        assertThat(last.has("if")).isFalse();
        assertThat(last.has("continue-on-error")).isFalse();
    }

    @Test
    void the_pin_script_reads_exactly_one_line_and_writes_kind_ref_pin_and_tag_to_the_output() throws IOException {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(PIN));

        // When / Then
        assertThat(script).contains("set -euo pipefail", "rekordContractTag", "GITHUB_OUTPUT");
        assertThat(script).contains("echo \"kind=$kind\"", "echo \"ref=$ref\"", "echo \"pin=$pin\"", "tag=$pin");
        assertThat(script).contains("^v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
        assertThat(script).containsPattern("(?s)-ne 1 \\]\\].*?exit 1");
    }

    @Test
    void a_tag_pin_on_a_push_to_main_is_accepted_and_resolves_to_the_tag_ref() throws Exception {
        // Given
        Path props = props("rekordContractTag=v0.1.0\n");

        // When
        var result = pin(props, PUSH_TO_MAIN);

        // Then
        assertThat(result.exit()).isZero();
        assertThat(result.output()).contains("kind=tag", "ref=refs/tags/v0.1.0", "pin=v0.1.0", "tag=v0.1.0");
    }

    @Test
    void a_branch_pin_equal_to_the_pull_request_head_branch_is_accepted_and_resolves_to_the_branch_ref()
            throws Exception {
        // Given
        Path props = props("rekordContractTag=feature/x\n");

        // When
        var result = pin(props, Map.of(
                "GITHUB_EVENT_NAME", "pull_request", "GITHUB_HEAD_REF", "feature/x", "GITHUB_REF_NAME", "7/merge"));

        // Then
        assertThat(result.exit()).isZero();
        assertThat(result.output()).contains("kind=branch", "ref=refs/heads/feature/x", "pin=feature/x");
        assertThat(result.output()).doesNotContain("tag=");
    }

    @Test
    void a_branch_pin_on_a_push_to_main_is_refused() throws Exception {
        // Given
        Path props = props("rekordContractTag=main\n");

        // When
        var result = pin(props, PUSH_TO_MAIN);

        // Then
        assertThat(result.exit()).isNotZero();
        assertThat(result.stderr()).contains("main must pin a rekord-contract tag");
    }

    @Test
    void a_branch_pin_that_is_not_this_branch_is_refused() throws Exception {
        // Given
        Path props = props("rekordContractTag=feature/other\n");

        // When
        var result = pin(props, Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF_NAME", "feature/x"));

        // Then
        assertThat(result.exit()).isNotZero();
        assertThat(result.stderr()).contains("a branch pin must name this branch (feature/x), found feature/other");
    }

    @Test
    void invalid_branch_names_are_refused_even_when_they_match_the_build_branch() throws Exception {
        for (String bad : List.of("a..b", "-x", "/x", "a b", "a;rm", "x$(id)")) {
            // Given
            Path props = props("rekordContractTag=" + bad + "\n");

            // When
            var result = pin(props, Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF_NAME", bad));

            // Then
            assertThat(result.exit()).as(bad).isNotZero();
        }
    }

    @Test
    void two_pin_lines_are_refused() throws Exception {
        // Given
        Path props = props("rekordContractTag=v0.1.0\nrekordContractTag=v0.2.0\n");

        // When
        var result = pin(props, PUSH_TO_MAIN);

        // Then
        assertThat(result.exit()).isNotZero();
        assertThat(result.stderr()).contains("exactly one rekordContractTag");
    }

    @Test
    void the_merge_check_fails_for_a_branch_pin_and_passes_for_a_tag_pin() throws Exception {
        // Given / When
        var branch = bash(Map.of(), MERGE_CHECK, "branch");
        var tag = bash(Map.of(), MERGE_CHECK, "tag");

        // Then
        assertThat(branch.exit()).isNotZero();
        assertThat(branch.stderr()).contains("must name a rekord-contract tag");
        assertThat(tag.exit()).isZero();
    }

    @Test
    void the_ref_check_script_verifies_tag_and_branch_commits_and_rejects_an_unknown_kind() throws Exception {
        // Given
        String script = Files.readString(REPO_ROOT.resolve(REF_CHECK));

        // When
        var unknown = bash(Map.of(), REF_CHECK, tmp.toString(), "weird", "x");

        // Then
        assertThat(script).contains("set -euo pipefail");
        assertThat(script).contains("fetch --depth=1 origin \"refs/tags/$pin:refs/tags/$pin\"");
        assertThat(script).contains("rev-parse HEAD", "rev-parse \"refs/tags/$pin^{commit}\"");
        assertThat(script).contains("ls-remote origin \"refs/heads/$pin\"");
        assertThat(script).containsPattern("\"\\$head\" != \"\\$expected\" \\]\\]");
        assertThat(unknown.exit()).isNotZero();
        assertThat(unknown.stderr()).contains("unknown pin kind");
    }

    @Test
    void properties_and_shell_files_are_checked_out_with_lf_endings() throws IOException {
        // Given
        var rules = Files.readAllLines(REPO_ROOT.resolve(".gitattributes")).stream()
                .map(line -> line.trim().replaceAll("\\s+", " "))
                .toList();

        // Then
        assertThat(rules).contains("*.properties text eol=lf", "*.sh text eol=lf");
    }

    private record Result(int exit, String stderr, String output) {}

    private static String pinnedValue() throws IOException {
        var pins = Files.readAllLines(REPO_ROOT.resolve("gradle.properties")).stream()
                .filter(line -> line.matches(PIN_LINE))
                .toList();
        assertThat(pins).as("rekordContractTag lines").hasSize(1);
        return pins.get(0).replaceFirst("^\\s*rekordContractTag\\s*[=:]\\s*", "").trim();
    }

    /** Negative, zero or positive as {@code left} is lower than, equal to or higher than {@code right}. */
    private static int compareVersions(String left, String right) {
        int[] a = numbers(left);
        int[] b = numbers(right);
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    private static int[] numbers(String tag) {
        Matcher matcher = TAG.matcher(tag);
        assertThat(matcher.matches()).as("%s is a vMAJOR.MINOR.PATCH tag", tag).isTrue();
        return new int[] {
            Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))
        };
    }

    private Path props(String content) throws IOException {
        return Files.writeString(tmp.resolve("gradle.properties"), content);
    }

    private Result pin(Path props, Map<String, String> env) throws Exception {
        Path out = tmp.resolve("github-output");
        Files.writeString(out, "");
        Map<String, String> all = new HashMap<>(env);
        all.put("GITHUB_OUTPUT", out.toString());
        var result = bash(all, PIN, props.toString());
        return new Result(result.exit(), result.stderr(), Files.readString(out));
    }

    private Result bash(Map<String, String> env, String... args) throws Exception {
        String[] scriptArgs = List.of(args).subList(1, args.length).toArray(String[]::new);
        var result = ScriptRunner.in(REPO_ROOT).with(env).run(args[0], scriptArgs);
        return new Result(result.exit(), result.stderr(), "");
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
