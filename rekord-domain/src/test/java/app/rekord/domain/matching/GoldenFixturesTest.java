package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The golden fixtures copied from rekord-api, with the ticket's two changes (TASK-24.4, AC #1, #8). */
class GoldenFixturesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern MACHINE_PATH = Pattern.compile("^([A-Za-z]:[/\\\\]|/Users/|/home/).*", Pattern.DOTALL);

    private static JsonNode read(String name) {
        try (InputStream in = GoldenFixturesTest.class.getResourceAsStream(name)) {
            assertThat(in).as("classpath resource %s", name).isNotNull();
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static JsonNode set() {
        return read("/golden/golden-set.json");
    }

    private static JsonNode library() {
        return read("/golden/golden-library.json");
    }

    @Test
    void the_index_holds_as_many_files_as_source_library_size_20869() {
        JsonNode set = set();
        List<LibraryIndex.Track> tracks = new ArrayList<>();
        for (JsonNode t : library().get("tracks")) {
            tracks.add(new LibraryIndex.Track(t.get("id").asText(),
                    t.path("artist").isNull() ? null : t.get("artist").asText(), t.get("title").asText(),
                    t.path("duration_sec").isNull() ? null : t.get("duration_sec").asDouble()));
        }
        assertThat(new LibraryIndex(tracks).size()).isEqualTo(set.at("/source/library_size").asInt()).isEqualTo(20869);
    }

    @Test
    void the_fixtures_hold_212_cases_9_preferences_and_13_membership_rows() {
        JsonNode set = set();
        JsonNode library = library();
        assertThat(set.get("cases")).hasSize(212);
        assertThat(set.at("/source/preferences").asInt()).isEqualTo(9);
        assertThat(set.at("/source/playlist_members").asInt()).isEqualTo(13);
        assertThat(library.get("preferences")).hasSize(9);
        assertThat(library.get("membership")).hasSize(13);
    }

    @Test
    void source_database_is_music_golden_src_db() {
        assertThat(set().at("/source/database").asText()).isEqualTo("/music/golden-src.db");
    }

    @Test
    void no_string_or_key_in_either_fixture_starts_with_a_machine_path() {
        assertThat(machinePaths("set", set())).isEmpty();
        assertThat(machinePaths("library", library())).isEmpty();
    }

    @Test
    void the_machine_path_check_catches_drive_letters_users_and_home() throws IOException {
        JsonNode probe = JSON.readTree("{\"a\":\"C:/x\",\"b\":\"d:\\\\x\",\"c\":[\"/Users/a\"],"
                + "\"d\":{\"/home/a\":\"ok\"},\"e\":\"/music/golden-src.db\"}");
        assertThat(machinePaths("p", probe)).hasSize(4);
    }

    private static List<String> machinePaths(String path, JsonNode node) {
        List<String> found = new ArrayList<>();
        if (node.isTextual() && MACHINE_PATH.matcher(node.asText()).matches()) {
            found.add(path);
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                String child = path + "." + e.getKey();
                if (MACHINE_PATH.matcher(e.getKey()).matches()) {
                    found.add(child + " (key)");
                }
                found.addAll(machinePaths(child, e.getValue()));
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                found.addAll(machinePaths(path + "[" + i + "]", node.get(i)));
            }
        }
        return found;
    }

    @Test
    void the_18_ud19c_cases_are_recorded_ambiguous_with_no_pick() {
        JsonNode cases = set().get("cases");
        assertThat(GoldenGate.UD19C).hasSize(18);
        for (int pos : GoldenGate.UD19C) {
            JsonNode expected = cases.get(pos).get("expected");
            assertThat(expected.get("bucket").asText()).as("case %d bucket", pos).isEqualTo("ambiguous");
            assertThat(expected.get("auto_selected_id").isNull()).as("case %d auto_selected_id", pos).isTrue();
            assertThat(expected.get("from_preference").asBoolean()).as("case %d from_preference", pos).isFalse();
            assertThat(expected.get("candidates")).as("case %d candidates", pos).isNotEmpty();
        }
    }

    @Test
    void the_summary_block_counts_ud19c() {
        JsonNode summary = set().get("summary");
        assertThat(summary.get("cases").asInt()).isEqualTo(212);
        assertThat(counts(summary.get("by_bucket"))).isEqualTo(Map.of("auto", 29, "ambiguous", 180, "unmatched", 3));
        Map<String, Map<String, Integer>> families = new TreeMap<>();
        summary.get("by_family").fields().forEachRemaining(e -> families.put(e.getKey(), counts(e.getValue())));
        Map<String, Map<String, Integer>> expected = new TreeMap<>();
        expected.put("typo", Map.of("ambiguous", 22, "auto", 3));
        expected.put("no_artist", Map.of("ambiguous", 25));
        expected.put("feat_inline", Map.of("ambiguous", 12));
        expected.put("exact", Map.of("ambiguous", 19, "auto", 6));
        expected.put("core_only", Map.of("ambiguous", 25));
        expected.put("added_version", Map.of("ambiguous", 25));
        expected.put("ampersand", Map.of("ambiguous", 17, "auto", 8));
        expected.put("case_punct", Map.of("ambiguous", 12));
        expected.put("duration_off", Map.of("ambiguous", 12));
        expected.put("unowned", Map.of("unmatched", 3, "ambiguous", 2));
        expected.put("preference", Map.of("ambiguous", 9));
        expected.put("playlist_member", Map.of("auto", 12));
        assertThat(families).isEqualTo(expected);
    }

    @Test
    void the_summary_block_agrees_with_the_cases() {
        JsonNode set = set();
        Map<String, Map<String, Integer>> counted = new TreeMap<>();
        Map<String, Integer> buckets = new TreeMap<>();
        for (JsonNode c : set.get("cases")) {
            String bucket = c.at("/expected/bucket").asText();
            counted.computeIfAbsent(c.get("family").asText(), f -> new LinkedHashMap<>()).merge(bucket, 1, Integer::sum);
            buckets.merge(bucket, 1, Integer::sum);
        }
        Map<String, Map<String, Integer>> recorded = new TreeMap<>();
        set.at("/summary/by_family").fields().forEachRemaining(e -> recorded.put(e.getKey(), counts(e.getValue())));
        assertThat(counted).isEqualTo(recorded);
        assertThat(buckets).isEqualTo(counts(set.at("/summary/by_bucket")));
    }

    @Test
    void the_provenance_note_names_the_oracle_commit() throws IOException {
        try (InputStream in = GoldenFixturesTest.class.getResourceAsStream("/golden/README.md")) {
            assertThat(in).as("golden/README.md").isNotNull();
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains("ec65ae35c182e6e25f571c76d45b15a78f183c10")
                    .contains("src/test/resources/golden-set.json")
                    .contains("src/test/resources/golden-library.json");
        }
    }

    @Test
    void every_case_and_candidate_carries_every_recorded_key() {
        // Given the checked-in set, which GoldenGate reads with null-safe helpers that would hide a lost key
        // When each case, query, expected block, version and candidate is checked for the keys the gate compares
        List<String> problems = schemaProblems(set());

        // Then none is missing (a message names a position and a key, never a value)
        assertThat(problems.stream().limit(20).toList()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "/cases/0, family", "/cases/0, query", "/cases/0, expected",
            "/cases/0/query, artist", "/cases/0/query, title", "/cases/0/query, duration_sec",
            "/cases/85/expected, bucket", "/cases/85/expected, auto_selected_id",
            "/cases/85/expected, from_preference", "/cases/85/expected, candidate_count",
            "/cases/85/expected, input_version", "/cases/85/expected, candidates",
            "/cases/85/expected/input_version, descriptors", "/cases/85/expected/input_version, remixer",
            "/cases/85/expected/candidates/0, track_id", "/cases/85/expected/candidates/0, score",
            "/cases/85/expected/candidates/0, artist", "/cases/85/expected/candidates/0, title",
            "/cases/85/expected/candidates/0, parts", "/cases/85/expected/candidates/0, duration_delta_sec",
            "/cases/85/expected/candidates/0, version", "/cases/85/expected/candidates/0, playlists",
            "/cases/85/expected/candidates/0/version, descriptors",
            "/cases/85/expected/candidates/0/version, remixer"})
    void a_copy_that_loses_one_recorded_key_is_reported_by_position_and_key(String parent, String key) {
        // Given a copy of the set that loses one key
        JsonNode copy = set();
        ((ObjectNode) copy.at(parent)).remove(key);

        // When it is checked
        // Then exactly one problem names that key at that position
        assertThat(schemaProblems(copy)).containsExactly(parent + " lacks " + key);
    }

    @Test
    void every_library_track_carries_every_key_the_gate_reads() {
        // Given the checked-in library, which GoldenGate reads with null-safe helpers for artist and duration
        // When each track is checked for id, artist, title and duration_sec
        List<String> problems = libraryProblems(library());

        // Then none is missing (a message names a position and a key, never a value)
        assertThat(problems.stream().limit(20).toList()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"/tracks/0, id", "/tracks/0, artist", "/tracks/0, title", "/tracks/0, duration_sec",
            "/tracks/20868, duration_sec"})
    void a_library_copy_that_loses_one_track_key_is_reported_by_position_and_key(String parent, String key) {
        JsonNode copy = library();
        ((ObjectNode) copy.at(parent)).remove(key);

        assertThat(libraryProblems(copy)).containsExactly(parent + " lacks " + key);
    }

    private static final List<String> TRACK_KEYS = List.of("id", "artist", "title", "duration_sec");

    /** Like {@link #schemaProblems}: a position and a key, never a value (the titles stay out of any output). */
    private static List<String> libraryProblems(JsonNode library) {
        List<String> problems = new ArrayList<>();
        JsonNode tracks = library.get("tracks");
        for (int i = 0; i < tracks.size(); i++) {
            lacking(problems, "/tracks/" + i, tracks.get(i), TRACK_KEYS);
        }
        return problems;
    }

    /** The keys GoldenGate reads from a case, by level; it reads most of them null-safe, so a lost key hides. */
    private static final List<String> CASE_KEYS = List.of("family", "query", "expected");
    private static final List<String> QUERY_KEYS = List.of("artist", "title", "duration_sec");
    private static final List<String> EXPECTED_KEYS = List.of("bucket", "auto_selected_id", "from_preference",
            "candidate_count", "input_version", "candidates");
    private static final List<String> VERSION_KEYS = List.of("descriptors", "remixer");
    private static final List<String> CANDIDATE_KEYS = List.of("track_id", "score", "artist", "title", "parts",
            "duration_delta_sec", "version", "playlists");

    /** One line per missing key, as "JSON-pointer lacks key": a position and a key, never a value (titles stay out). */
    private static List<String> schemaProblems(JsonNode set) {
        List<String> problems = new ArrayList<>();
        JsonNode cases = set.get("cases");
        for (int i = 0; i < cases.size(); i++) {
            String at = "/cases/" + i;
            JsonNode c = cases.get(i);
            lacking(problems, at, c, CASE_KEYS);
            if (c.has("query")) {
                lacking(problems, at + "/query", c.get("query"), QUERY_KEYS);
            }
            if (!c.has("expected")) {
                continue;
            }
            JsonNode expected = c.get("expected");
            String inExpected = at + "/expected";
            lacking(problems, inExpected, expected, EXPECTED_KEYS);
            if (expected.has("input_version")) {
                lacking(problems, inExpected + "/input_version", expected.get("input_version"), VERSION_KEYS);
            }
            JsonNode candidates = expected.path("candidates");
            for (int k = 0; k < candidates.size(); k++) {
                String inCandidate = inExpected + "/candidates/" + k;
                lacking(problems, inCandidate, candidates.get(k), CANDIDATE_KEYS);
                if (candidates.get(k).has("version")) {
                    lacking(problems, inCandidate + "/version", candidates.get(k).get("version"), VERSION_KEYS);
                }
            }
        }
        return problems;
    }

    private static void lacking(List<String> problems, String at, JsonNode node, List<String> keys) {
        for (String key : keys) {
            if (!node.has(key)) {
                problems.add(at + " lacks " + key);
            }
        }
    }

    private static Map<String, Integer> counts(JsonNode node) {
        Map<String, Integer> out = new TreeMap<>();
        node.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asInt()));
        return out;
    }
}
