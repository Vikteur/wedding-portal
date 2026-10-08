package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.domain.matching.TrackMatcher.MatchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The matcher's acceptance gate (TASK-24.4, UX-07): rekord-api's golden set, with the ticket's two changes, run
 * against the golden library with its preferences and membership rows. {@link GoldenGate} compares every
 * recorded field.
 */
class MatcherGoldenSetTest {

    private static final List<Integer> UD19C = List.of(82, 85, 86, 88, 90, 100, 101, 107, 108, 109, 110, 111,
            115, 122, 152, 155, 156, 161);

    private static List<Integer> positionsOf(String family) {
        JsonNode cases = GoldenGate.set().get("cases");
        return IntStream.range(0, cases.size())
                .filter(i -> cases.get(i).get("family").asText().equals(family)).boxed().toList();
    }

    @Test
    void every_bucket_auto_pick_from_preference_and_candidate_count_equals_the_recorded_one() {
        JsonNode cases = GoldenGate.set().get("cases");
        List<MatchResult> answers = GoldenGate.answers();
        assertThat(answers).hasSize(212);
        for (int i = 0; i < cases.size(); i++) {
            JsonNode expected = cases.get(i).get("expected");
            MatchResult actual = answers.get(i);
            String where = "case " + i + " (" + cases.get(i).get("family").asText() + ")";
            assertThat(GoldenGate.contractBucket(actual.bucket())).as("%s bucket", where)
                    .isEqualTo(expected.get("bucket").asText());
            assertThat(actual.autoSelectedId()).as("%s auto_selected_id", where)
                    .isEqualTo(expected.get("auto_selected_id").isNull() ? null
                            : expected.get("auto_selected_id").asText());
            assertThat(actual.fromPreference()).as("%s from_preference", where)
                    .isEqualTo(expected.get("from_preference").asBoolean());
            assertThat(actual.candidates()).as("%s candidate_count", where)
                    .hasSize(expected.get("candidate_count").asInt());
        }
    }

    @Test
    void the_buckets_total_29_auto_180_ambiguous_3_unmatched() {
        Map<String, Integer> counts = new TreeMap<>();
        for (MatchResult answer : GoldenGate.answers()) {
            counts.merge(GoldenGate.contractBucket(answer.bucket()), 1, Integer::sum);
        }
        assertThat(counts).containsExactly(
                Map.entry("ambiguous", 180), Map.entry("auto", 29), Map.entry("unmatched", 3));
    }

    @Test
    void the_18_ud19c_cases_answer_ambiguous_with_no_pick_and_their_recorded_candidates() {
        JsonNode cases = GoldenGate.set().get("cases");
        List<MatchResult> answers = GoldenGate.answers();
        assertThat(UD19C).hasSize(18);
        for (int i : UD19C) {
            MatchResult actual = answers.get(i);
            assertThat(GoldenGate.contractBucket(actual.bucket())).as("case %d bucket", i).isEqualTo("ambiguous");
            assertThat(actual.autoSelectedId()).as("case %d auto_selected_id", i).isNull();
            assertThat(actual.fromPreference()).as("case %d from_preference", i).isFalse();
            assertThat(actual.candidates().stream().map(c -> c.track().id()).toList())
                    .as("case %d candidates", i)
                    .isEqualTo(StreamSupport.stream(cases.get(i).at("/expected/candidates").spliterator(), false)
                            .map(c -> c.get("track_id").asText()).toList());
        }
    }

    @Test
    void every_candidate_list_equals_the_recorded_one() {
        // Ids in order, score, facets and null facets, delta, versions, remixer, playlists and input_version.
        assertThat(GoldenGate.differences(GoldenGate.set())).isEmpty();
    }

    @Test
    void the_thresholds_block_equals_the_matchers_constants() {
        JsonNode thresholds = GoldenGate.set().get("thresholds");
        Map<String, Double> literals = new TreeMap<>(Map.ofEntries(
                Map.entry("REPORT_THRESHOLD", 0.45), Map.entry("STRONG_THRESHOLD", 0.60),
                Map.entry("AUTO_SCORE", 0.82), Map.entry("AUTO_MARGIN", 0.10),
                Map.entry("AUTO_MIN_VERSION", 0.90), Map.entry("AUTO_MIN_DURATION", 0.55),
                Map.entry("MAX_CANDIDATES", 8.0), Map.entry("WEIGHT_TITLE", 0.40),
                Map.entry("WEIGHT_ARTIST", 0.30), Map.entry("WEIGHT_COMBINED", 0.70),
                Map.entry("WEIGHT_VERSION", 0.15), Map.entry("WEIGHT_DURATION", 0.15),
                Map.entry("PLAYLIST_BONUS", 0.02), Map.entry("PLAYLIST_BONUS_CAP", 3.0)));
        Map<String, Double> constants = new TreeMap<>(Map.ofEntries(
                Map.entry("REPORT_THRESHOLD", Score.REPORT_THRESHOLD),
                Map.entry("STRONG_THRESHOLD", Score.STRONG_THRESHOLD),
                Map.entry("AUTO_SCORE", Score.AUTO_SCORE), Map.entry("AUTO_MARGIN", Score.AUTO_MARGIN),
                Map.entry("AUTO_MIN_VERSION", Score.AUTO_MIN_VERSION),
                Map.entry("AUTO_MIN_DURATION", Score.AUTO_MIN_DURATION),
                Map.entry("MAX_CANDIDATES", (double) Score.MAX_CANDIDATES),
                Map.entry("WEIGHT_TITLE", Score.WEIGHT_TITLE), Map.entry("WEIGHT_ARTIST", Score.WEIGHT_ARTIST),
                Map.entry("WEIGHT_COMBINED", Score.WEIGHT_COMBINED),
                Map.entry("WEIGHT_VERSION", Score.WEIGHT_VERSION),
                Map.entry("WEIGHT_DURATION", Score.WEIGHT_DURATION),
                Map.entry("PLAYLIST_BONUS", Score.PLAYLIST_BONUS),
                Map.entry("PLAYLIST_BONUS_CAP", (double) Score.PLAYLIST_BONUS_CAP)));
        assertThat(thresholds).hasSize(14);
        Map<String, Double> recorded = new TreeMap<>();
        thresholds.fields().forEachRemaining(e -> recorded.put(e.getKey(), e.getValue().asDouble()));
        assertThat(recorded).isEqualTo(literals).isEqualTo(constants);
    }

    @Test
    void the_9_preference_cases_pick_the_remembered_file_and_stay_ambiguous() {
        JsonNode cases = GoldenGate.set().get("cases");
        List<Integer> positions = positionsOf("preference");
        assertThat(positions).hasSize(9);
        for (int i : positions) {
            MatchResult actual = GoldenGate.answers().get(i);
            JsonNode expected = cases.get(i).get("expected");
            assertThat(actual.fromPreference()).as("case %d from_preference", i).isTrue();
            assertThat(actual.autoSelectedId()).as("case %d auto_selected_id", i)
                    .isEqualTo(expected.get("auto_selected_id").asText());
            assertThat(GoldenGate.contractBucket(actual.bucket())).as("case %d bucket", i)
                    .isEqualTo(expected.get("bucket").asText());
        }
    }

    @Test
    void the_12_playlist_member_cases_keep_their_recorded_bucket() {
        JsonNode cases = GoldenGate.set().get("cases");
        List<Integer> positions = positionsOf("playlist_member");
        assertThat(positions).hasSize(12);
        for (int i : positions) {
            assertThat(GoldenGate.contractBucket(GoldenGate.answers().get(i).bucket()))
                    .as("case %d bucket", i).isEqualTo(cases.get(i).at("/expected/bucket").asText());
        }
    }

    @Test
    void a_copy_with_one_candidate_score_moved_by_0_001_fails_and_names_case_family_track_and_both_scores() {
        JsonNode set = GoldenGate.set();
        assertThat(GoldenGate.differences(set)).isEmpty();

        JsonNode copy = set.deepCopy();
        ObjectNode moved = null;
        for (JsonNode candidate : copy.at("/cases/85/expected/candidates")) {
            if (candidate.get("track_id").asText().equals("76a106fe8fe7")) {
                moved = (ObjectNode) candidate;
            }
        }
        assertThat(moved).isNotNull();
        assertThat(moved.get("score").asDouble()).isEqualTo(0.9163);
        moved.put("score", 0.9173);

        assertThatThrownBy(() -> GoldenGate.assertNoDifferences(copy))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("case 85")
                .hasMessageContaining("typo")
                .hasMessageContaining("76a106fe8fe7")
                .hasMessageContaining("expected 0.9173")
                .hasMessageContaining("actual 0.9163");
    }

    @Test
    void a_copy_with_a_score_or_facet_that_is_not_a_number_fails() {
        JsonNode copy = GoldenGate.set();
        ((ObjectNode) copy.at("/cases/85/expected/candidates/0")).put("score", Double.NaN);
        ((ObjectNode) copy.at("/cases/85/expected/candidates/0/parts")).put("title", Double.NaN);

        assertThat(GoldenGate.differences(copy))
                .anyMatch(line -> line.startsWith("case 85 ") && line.contains("score expected NaN"))
                .anyMatch(line -> line.startsWith("case 85 ") && line.contains("facet title expected NaN"));
    }

    /** A value of the same kind as {@code current} that is certainly not equal to it. */
    private static JsonNode different(JsonNode current) {
        if (current.isBoolean()) {
            return BooleanNode.valueOf(!current.asBoolean());
        }
        if (current.isIntegralNumber()) {
            return IntNode.valueOf(current.asInt() + 1);
        }
        if (current.isNumber()) {
            return DoubleNode.valueOf(current.asDouble() + 1.0);
        }
        if (current.isArray()) {
            ArrayNode longer = ((ArrayNode) current).deepCopy();
            longer.add("changed");
            return longer;
        }
        return TextNode.valueOf(current.isTextual() ? current.asText() + " changed" : "changed");
    }

    @ParameterizedTest(name = "{1} of {0}")
    @CsvSource(delimiter = '|', value = {
            "/cases/85/expected                       | bucket              | bucket expected",
            "/cases/85/expected                       | auto_selected_id    | auto_selected_id expected",
            "/cases/85/expected                       | from_preference     | from_preference expected",
            "/cases/85/expected                       | candidate_count     | candidate_count expected",
            "/cases/85/expected/input_version         | descriptors         | input_version.descriptors expected",
            "/cases/85/expected/input_version         | remixer             | input_version.remixer expected",
            "/cases/85/expected/candidates/0          | track_id            | candidate ids expected",
            "/cases/85/expected/candidates/0          | score               | score expected",
            "/cases/85/expected/candidates/0          | artist              | artist expected",
            "/cases/85/expected/candidates/0          | title               | title expected",
            "/cases/85/expected/candidates/0          | duration_delta_sec  | duration_delta_sec expected",
            "/cases/85/expected/candidates/0          | playlists           | playlists expected",
            "/cases/85/expected/candidates/0/version  | descriptors         | version.descriptors expected",
            "/cases/85/expected/candidates/0/version  | remixer             | version.remixer expected"})
    void a_copy_with_one_recorded_field_changed_names_that_field(String parent, String key, String reported) {
        // Given a copy of the golden set with one recorded value of case 85 changed
        JsonNode copy = GoldenGate.set();
        ObjectNode holder = (ObjectNode) copy.at(parent);
        assertThat(holder.has(key)).as("%s holds %s", parent, key).isTrue();
        holder.set(key, different(holder.get(key)));

        // When the copy is compared
        List<String> lines = GoldenGate.differences(copy);

        // Then case 85, and only case 85, reports a difference that names the field
        assertThat(lines).as("changing %s of %s", key, parent)
                .isNotEmpty()
                .allMatch(line -> line.startsWith("case 85 ("))
                .anyMatch(line -> line.contains(" " + reported));
    }

    @Test
    void a_copy_with_one_facet_value_changed_names_that_facet() {
        // Given a copy with the value of the first recorded facet of case 85's leader changed
        JsonNode copy = GoldenGate.set();
        ObjectNode parts = (ObjectNode) copy.at("/cases/85/expected/candidates/0/parts");
        String facet = parts.fieldNames().next();
        parts.set(facet, different(parts.get(facet).isNull() ? DoubleNode.valueOf(0.5) : parts.get(facet)));

        // When the copy is compared, then the difference names the facet
        assertThat(GoldenGate.differences(copy))
                .isNotEmpty()
                .allMatch(line -> line.startsWith("case 85 ("))
                .anyMatch(line -> line.contains(" facet " + facet + " expected"));
    }

    @Test
    void a_copy_with_a_facet_added_or_removed_names_the_facet_names() {
        // Given a copy with a facet more recorded on case 85's leader, and another with the first facet removed
        JsonNode added = GoldenGate.set();
        ((ObjectNode) added.at("/cases/85/expected/candidates/0/parts")).put("extra", 0.5);
        JsonNode removed = GoldenGate.set();
        ObjectNode parts = (ObjectNode) removed.at("/cases/85/expected/candidates/0/parts");
        parts.remove(parts.fieldNames().next());

        // When each copy is compared, then the difference names the facet names
        assertThat(GoldenGate.differences(added)).anyMatch(line -> line.contains(" facet names expected"));
        assertThat(GoldenGate.differences(removed)).singleElement().asString()
                .startsWith("case 85 (").contains(" facet names expected");
    }

    @Test
    void a_copy_with_a_query_changed_says_the_query_differs_instead_of_comparing_stale_answers() {
        // Given a copy whose case 85 asks for another song than the checked-in set ran
        JsonNode copy = GoldenGate.set();
        ((ObjectNode) copy.at("/cases/85/query")).put("title", "Another Song");

        // When the copy is compared
        List<String> lines = GoldenGate.differences(copy);

        // Then the one difference says the query differs, and the expected block is not compared to old answers
        assertThat(lines).singleElement().asString()
                .startsWith("case 85 (")
                .contains("query differs from the checked-in set")
                .contains("compares only the expected blocks");
    }

    @Test
    void a_copy_with_a_case_removed_fails_on_the_case_count() {
        // Given a copy with its first case removed
        JsonNode copy = GoldenGate.set();
        ((ArrayNode) copy.get("cases")).remove(0);

        // When the copy is compared, then only the count line is reported
        assertThat(GoldenGate.differences(copy)).containsExactly("cases: expected 211 actual 212");
    }

    @Test
    void a_case_the_matcher_cannot_run_is_named_by_position_family_and_query() {
        // Given a case whose query has no title, so the run throws
        ObjectNode broken = JsonNodeFactory.instance.objectNode();
        broken.put("family", "typo");
        broken.putObject("query").put("artist", "Some Artist");

        // When it is run as case 7
        // Then the failure is an AssertionError that names the case and keeps the cause
        assertThatThrownBy(() -> GoldenGate.matchCase(7, broken, new LibraryIndex(List.of()), Map.of(), Map.of()))
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("case 7 (typo) ")
                .hasMessageContaining("Some Artist")
                .hasMessageEndingWith(": the matcher threw")
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void a_difference_line_without_a_case_family_sorts_first_instead_of_crashing() {
        // Given the count line, which names no case family
        // When its family is read, then it is empty (and sorts before any family)
        assertThat(GoldenGate.family("cases: expected 211 actual 212")).isEmpty();
        assertThat(GoldenGate.family("case 85 (typo) a / b: bucket expected auto actual ambiguous"))
                .isEqualTo("typo");
    }
}
