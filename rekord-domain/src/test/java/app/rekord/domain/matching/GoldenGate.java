package app.rekord.domain.matching;

import app.rekord.domain.matching.LibraryIndex.Track;
import app.rekord.domain.matching.TrackMatcher.MatchResult;
import app.rekord.domain.matching.TrackMatcher.ScoredCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The golden-set gate's engine (TASK-24.4): loads rekord-api's golden fixtures from the classpath once, runs the
 * matcher over the 212 cases with the recorded preferences and membership rows, and lists the differences from
 * the recorded answers, one line per difference, naming the case (see {@link #differences} for its two limits).
 */
final class GoldenGate {

    /** The 18 cases whose same-song candidates the owner left with the DJ (UD-19.c), by position in the set. */
    static final List<Integer> UD19C = List.of(82, 85, 86, 88, 90, 100, 101, 107, 108, 109, 110, 111,
            115, 122, 152, 155, 156, 161);

    private static final double TOLERANCE = 1e-6;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode SET = read("/golden/golden-set.json");
    private static final JsonNode LIBRARY = read("/golden/golden-library.json");
    private static List<MatchResult> answers;

    private GoldenGate() {
    }

    private static JsonNode read(String name) {
        try (InputStream in = GoldenGate.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("classpath resource missing: " + name);
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fresh copy of the checked-in golden set, safe to change. */
    static JsonNode set() {
        return SET.deepCopy();
    }

    static String contractBucket(Bucket bucket) {
        return switch (bucket) {
            case AUTO -> "auto";
            case AMBIGUOUS -> "ambiguous";
            case UNMATCHED -> "unmatched";
        };
    }

    /** The matcher's answer for every case, in case order (computed once per JVM). */
    static synchronized List<MatchResult> answers() {
        if (answers == null) {
            List<Track> tracks = new ArrayList<>();
            for (JsonNode t : LIBRARY.get("tracks")) {
                tracks.add(new Track(t.get("id").asText(), text(t.get("artist")), t.get("title").asText(),
                        number(t.get("duration_sec"))));
            }
            LibraryIndex index = new LibraryIndex(tracks);
            Map<String, String> preferences = new HashMap<>();
            LIBRARY.get("preferences").fields()
                    .forEachRemaining(e -> preferences.put(e.getKey(), e.getValue().asText()));
            Map<String, List<String>> membership = new HashMap<>();
            LIBRARY.get("membership").fields().forEachRemaining(e -> {
                List<String> names = new ArrayList<>();
                e.getValue().forEach(n -> names.add(n.asText()));
                membership.put(e.getKey(), names);
            });
            List<MatchResult> results = new ArrayList<>();
            int position = 0;
            for (JsonNode c : SET.get("cases")) {
                results.add(matchCase(position++, c, index, membership, preferences));
            }
            answers = List.copyOf(results);
        }
        return answers;
    }

    /** One case through the matcher; a crash is rethrown naming the position, family and query. */
    static MatchResult matchCase(int position, JsonNode c, LibraryIndex index,
                                 Map<String, List<String>> membership, Map<String, String> preferences) {
        JsonNode q = c.get("query");
        try {
            return TrackMatcher.matchOne(new MatchQuery(position, text(q.get("artist")),
                    q.get("title").asText(), number(q.get("duration_sec"))), index, membership, preferences);
        } catch (RuntimeException e) {
            throw new AssertionError("case " + position + " (" + c.path("family").asText() + ") "
                    + text(q.path("artist")) + " / " + text(q.path("title")) + ": the matcher threw", e);
        }
    }

    /**
     * The differences between the recorded {@code expected} blocks of {@code set} and the matcher's answers for the
     * checked-in set's queries ({@link #answers()}, run once): the matcher never runs on the queries of {@code set}.
     * A case whose query is not the checked-in one yields one line saying so, and its expected block is not
     * compared. Two limits: a different case count yields only the count line, and a case whose candidate ids
     * differ yields that one line, without the per-candidate fields.
     */
    static List<String> differences(JsonNode set) {
        List<String> lines = new ArrayList<>();
        List<MatchResult> actual = answers();
        JsonNode cases = set.get("cases");
        if (cases.size() != actual.size()) {
            lines.add("cases: expected " + cases.size() + " actual " + actual.size());
            return lines;
        }
        JsonNode checkedIn = SET.get("cases");
        for (int i = 0; i < cases.size(); i++) {
            JsonNode c = cases.get(i);
            JsonNode query = c.get("query");
            if (!query.equals(checkedIn.get(i).get("query"))) {
                lines.add("case " + i + " (" + c.path("family").asText() + "): query differs from the checked-in set,"
                        + " differences() compares only the expected blocks");
                continue;
            }
            String head = "case " + i + " (" + c.get("family").asText() + ") " + text(query.get("artist")) + " / "
                    + query.get("title").asText() + ": ";
            compareCase(head, c.get("expected"), actual.get(i), lines);
        }
        return lines;
    }

    static void assertNoDifferences(JsonNode set) {
        List<String> lines = differences(set);
        if (!lines.isEmpty()) {
            StringBuilder message = new StringBuilder(lines.size() + " golden difference(s):");
            lines.stream().sorted(java.util.Comparator.comparing(GoldenGate::family)).forEach(
                    l -> message.append(System.lineSeparator()).append(l));
            throw new AssertionError(message.toString());
        }
    }

    /** The family named in a case line, or empty for a line that names no case (it sorts first). */
    static String family(String line) {
        int open = line.indexOf('(');
        return open < 0 ? "" : line.substring(open + 1, line.indexOf(')', open));
    }

    private static void compareCase(String head, JsonNode expected, MatchResult actual, List<String> out) {
        check(out, head, "bucket", expected.get("bucket").asText(), contractBucket(actual.bucket()));
        check(out, head, "auto_selected_id", text(expected.get("auto_selected_id")), actual.autoSelectedId());
        check(out, head, "from_preference", expected.get("from_preference").asBoolean(), actual.fromPreference());
        check(out, head, "candidate_count", expected.get("candidate_count").asInt(), actual.candidates().size());
        check(out, head, "input_version.descriptors", strings(expected.at("/input_version/descriptors")),
                actual.inputVersion().descriptors());
        check(out, head, "input_version.remixer", text(expected.at("/input_version/remixer")),
                actual.inputVersion().remixer());

        JsonNode candidates = expected.get("candidates");
        List<String> expectedIds = new ArrayList<>();
        candidates.forEach(c -> expectedIds.add(c.get("track_id").asText()));
        List<String> actualIds = actual.candidates().stream().map(c -> c.track().id()).toList();
        if (!expectedIds.equals(actualIds)) {
            out.add(head + "candidate ids expected " + expectedIds + " actual " + actualIds);
            return;
        }
        for (int k = 0; k < candidates.size(); k++) {
            compareCandidate(head + "candidate[" + k + "] " + expectedIds.get(k) + " ", candidates.get(k),
                    actual.candidates().get(k), out);
        }
    }

    private static void compareCandidate(String head, JsonNode e, ScoredCandidate a, List<String> out) {
        close(out, head, "score", e.get("score").asDouble(), a.score());
        check(out, head, "artist", text(e.get("artist")), a.track().artist());
        check(out, head, "title", e.get("title").asText(), a.track().title());

        Set<String> expectedFacets = new TreeSet<>();
        e.get("parts").fieldNames().forEachRemaining(expectedFacets::add);
        check(out, head, "facet names", expectedFacets, new TreeSet<>(a.parts().keySet()));
        for (String facet : expectedFacets) {
            JsonNode recorded = e.get("parts").get(facet);
            Double got = a.parts().get(facet);
            if (recorded.isNull() || got == null) {
                check(out, head, "facet " + facet, recorded.isNull() ? null : recorded.asDouble(), got);
            } else {
                close(out, head, "facet " + facet, recorded.asDouble(), got);
            }
        }

        JsonNode delta = e.get("duration_delta_sec");
        check(out, head, "duration_delta_sec", delta.isNull() ? null : delta.asDouble(), a.durationDeltaSec());
        check(out, head, "version.descriptors", strings(e.at("/version/descriptors")), a.version().descriptors());
        check(out, head, "version.remixer", text(e.at("/version/remixer")), a.version().remixer());
        check(out, head, "playlists", strings(e.get("playlists")), a.playlists());
    }

    private static void check(List<String> out, String head, String field, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            out.add(head + field + " expected " + expected + " actual " + actual);
        }
    }

    private static void close(List<String> out, String head, String field, double expected, double actual) {
        // Written as "not within" so a NaN on either side is a difference.
        if (!(Math.abs(expected - actual) <= TOLERANCE)) {
            out.add(head + field + " expected " + expected + " actual " + actual);
        }
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asText()));
        return values;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Double number(JsonNode node) {
        return node == null || node.isNull() ? null : node.asDouble();
    }
}
