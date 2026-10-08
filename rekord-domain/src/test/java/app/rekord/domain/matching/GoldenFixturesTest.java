package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** The golden fixtures copied from rekord-api, with the ticket's two changes (TASK-24.4, AC #1, #8). */
class GoldenFixturesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern MACHINE_PATH = Pattern.compile("^([A-Za-z]:[/\\\\]|/Users/|/home/).*", Pattern.DOTALL);
    private static final List<Integer> UD19C = List.of(82, 85, 86, 88, 90, 100, 101, 107, 108, 109, 110, 111,
            115, 122, 152, 155, 156, 161);

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
        for (int pos : UD19C) {
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

    private static Map<String, Integer> counts(JsonNode node) {
        Map<String, Integer> out = new TreeMap<>();
        node.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asInt()));
        return out;
    }
}
