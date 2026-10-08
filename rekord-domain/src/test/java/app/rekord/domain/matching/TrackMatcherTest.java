package app.rekord.domain.matching;

import app.rekord.domain.matching.LibraryIndex.Track;
import app.rekord.domain.matching.TrackMatcher.MatchResult;
import app.rekord.domain.matching.TrackMatcher.ScoredCandidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TrackMatcherTest {

    private static List<String> ids(MatchResult result) {
        return result.candidates().stream().map(c -> c.track().id()).toList();
    }

    private static LibraryIndex bandLibraryWithSolo() {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i <= 50; i++) {
            tracks.add(new Track("b" + i, "Band", "Love", null));
        }
        tracks.add(new Track("z", "Solo", "Love", null));
        return new LibraryIndex(tracks);
    }

    @Test
    void a_rare_token_match_lists_only_that_file() {
        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "Solo", "Love", null), bandLibraryWithSolo());

        assertThat(ids(result)).containsExactly("z");
    }

    @Test
    void a_common_query_lists_the_last_8_band_files_last_first() {
        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "Band", "Love", null), bandLibraryWithSolo());

        assertThat(ids(result)).containsExactly("b50", "b49", "b48", "b47", "b46", "b45", "b44", "b43");
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsOnly(1.0);
    }

    @Test
    void the_fallback_candidates_are_scored_on_the_combined_facet() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("a", null, "Dancing Queen", null),
                new Track("b", null, "Dancing Queen", null)));

        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "", "Dancin Quen", null), index);

        assertThat(ids(result)).containsExactly("a", "b");
        for (ScoredCandidate c : result.candidates()) {
            assertThat(c.score()).isEqualTo(0.9314);
            assertThat(c.parts().get("combined")).isCloseTo(0.9166666666666665, within(1e-9));
            assertThat(c.parts().get("version")).isEqualTo(1.0);
            assertThat(c.parts()).containsKey("duration");
            assertThat(c.parts().get("duration")).isNull();
        }
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
    }

    @Test
    void an_empty_query_is_unmatched() {
        LibraryIndex index = new LibraryIndex(List.of(new Track("1", "Daft Punk", "One More Time", null)));

        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "", "", null), index);

        assertThat(result.candidates()).isEmpty();
        assertThat(result.bucket()).isEqualTo(Bucket.UNMATCHED);
    }

    @Test
    void candidates_below_045_are_dropped() {
        // Both files share the rare token "love" with the query, so the index retrieves them. Numbers
        // computed with rekord-api's Matcher: 0.4641 stays, a longer title scores 0.419, under the 0.45 floor.
        MatchQuery query = new MatchQuery(0, "Alpha", "Love", null);

        MatchResult kept = TrackMatcher.matchOne(query,
                new LibraryIndex(List.of(new Track("x", "Zeta", "Love Zzzz Wwww", null))));
        MatchResult dropped = TrackMatcher.matchOne(query,
                new LibraryIndex(List.of(new Track("x", "Zeta", "Love Zzzz Wwww Yyyy", null))));

        assertThat(ids(kept)).containsExactly("x");
        assertThat(kept.candidates().get(0).score()).isEqualTo(0.4641);
        assertThat(dropped.candidates()).isEmpty();
        assertThat(dropped.bucket()).isEqualTo(Bucket.UNMATCHED);
    }

    // The cases below use the query "Daft Punk" / "One More Time" and scores computed with rekord-api's
    // matcher classes (recorded in TASK-24.3). They pin the facets, the score order and rekord-api's
    // three-guard bucket as ported here; TASK-24.3 tightens auto under UD-19.c.

    private static Track daftPunk(String id, String title, Double durationSec) {
        return new Track(id, "Daft Punk", title, durationSec);
    }

    private static MatchResult match(Double queryDuration, Track... tracks) {
        return TrackMatcher.matchOne(new MatchQuery(0, "Daft Punk", "One More Time", queryDuration),
                new LibraryIndex(List.of(tracks)));
    }

    @Test
    void a_close_single_file_is_auto_with_every_facet_at_1() {
        MatchResult result = match(320.0, daftPunk("f", "One More Time", 322.0));

        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("f");
        ScoredCandidate only = result.candidates().get(0);
        assertThat(only.score()).isEqualTo(1.0);
        // AC #3: the delta is file minus query, 322 - 320.
        assertThat(only.durationDeltaSec()).isEqualTo(2.0);
        assertThat(only.parts()).containsExactly(
                java.util.Map.entry("title", 1.0), java.util.Map.entry("artist", 1.0),
                java.util.Map.entry("version", 1.0), java.util.Map.entry("duration", 1.0));
    }

    private static MatchResult match(Double queryDuration, Map<String, List<String>> playlists, Track... tracks) {
        return TrackMatcher.matchOne(new MatchQuery(0, "Daft Punk", "One More Time", queryDuration),
                new LibraryIndex(List.of(tracks)), playlists);
    }

    // AC #3 (BR-MX-14, PIN-14-0369): half-even to one decimal, each file matched alone.
    @Test
    void the_duration_delta_is_rounded_half_even_to_one_decimal() {
        MatchResult low = match(320.0, daftPunk("f", "One More Time", 320.25));
        MatchResult high = match(320.0, daftPunk("f", "One More Time", 320.75));

        assertThat(low.candidates().get(0).durationDeltaSec()).isEqualTo(0.2);
        assertThat(high.candidates().get(0).durationDeltaSec()).isEqualTo(0.8);
    }

    // AC #3: no delta when either duration is unknown.
    @Test
    void the_duration_delta_is_null_when_either_duration_is_unknown() {
        MatchResult noQuery = match(null, daftPunk("f", "One More Time", 322.0));
        MatchResult noFile = match(320.0, daftPunk("f", "One More Time", null));

        assertThat(noQuery.candidates().get(0).durationDeltaSec()).isNull();
        assertThat(noFile.candidates().get(0).durationDeltaSec()).isNull();
    }

    // AC #4 and AC #7
    @Test
    void without_a_duration_only_an_exact_version_is_auto() {
        MatchResult exact = match(null, daftPunk("f", "One More Time", 322.0));
        MatchResult extended = match(null, daftPunk("f", "One More Time (Extended Mix)", 400.0));

        assertThat(exact.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(extended.candidates().get(0).score()).isEqualTo(0.9294);
        assertThat(extended.candidates().get(0).parts().get("version")).isEqualTo(0.6);
        assertThat(extended.candidates().get(0).parts().get("duration")).isNull();
        assertThat(extended.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(extended.autoSelectedId()).isNull();
    }

    // AC #4
    @Test
    void another_version_is_never_auto() {
        MatchResult result = match(320.0, daftPunk("f", "One More Time (Kygo Remix)", 320.0));

        assertThat(result.candidates().get(0).score()).isEqualTo(0.8875);
        assertThat(result.candidates().get(0).parts().get("version")).isEqualTo(0.25);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
    }

    // AC #7
    @Test
    void a_far_duration_is_never_auto() {
        MatchResult result = match(320.0, daftPunk("f", "One More Time", 380.0));

        assertThat(result.candidates().get(0).score()).isEqualTo(0.85);
        assertThat(result.candidates().get(0).parts().get("duration")).isEqualTo(0.0);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
    }

    // AC #7
    @Test
    void candidates_are_ordered_by_score_and_a_clear_leader_is_auto() {
        // Retrieval lists b first (same hit count, later file); the score puts a first.
        MatchResult result = match(320.0,
                daftPunk("a", "One More Time", 320.0),
                daftPunk("b", "One More Time (Kygo Remix)", 320.0));

        assertThat(ids(result)).containsExactly("a", "b");
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsExactly(1.0, 0.8875);
        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("a");
    }

    // AC #8: without membership, equal files stay ambiguous in retrieval order.
    @Test
    void two_equal_leaders_are_ambiguous_and_keep_retrieval_order() {
        MatchResult result = match(320.0, daftPunk("x", "One More Time", 320.0), daftPunk("y", "One More Time", 320.0));

        assertThat(ids(result)).containsExactly("y", "x");
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #8 (BR-MX-15, BR-MX-16, BR-LIB-42): the only playlist member leads and passes the margin guard.
    @Test
    void a_playlist_member_leads_two_equal_files_and_is_auto() {
        MatchResult result = match(320.0, Map.of("x", List.of("Peak hour")),
                daftPunk("x", "One More Time", 320.0), daftPunk("y", "One More Time", 320.0));

        assertThat(ids(result)).containsExactly("x", "y");
        assertThat(result.candidates().get(0).playlists()).containsExactly("Peak hour");
        assertThat(result.candidates().get(1).playlists()).isEmpty();
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsExactly(1.0, 1.0);
        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("x");
    }

    // AC #9 (BR-MX-15, BR-LIB-42): the nudge orders, the bucket reads the raw scores.
    @Test
    void the_nudge_orders_but_the_bucket_reads_the_raw_scores() {
        MatchResult result = match(320.0, Map.of(
                        "a", List.of("P1"),
                        "b", List.of("P1", "P2", "P3", "P4")),
                daftPunk("a", "One More Time", 320.0), daftPunk("b", "One More Time", 326.0));

        assertThat(ids(result)).containsExactly("b", "a");
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsExactly(0.9893, 1.0);
        assertThat(result.candidates().get(0).playlists()).containsExactly("P1", "P2", "P3", "P4");
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #10 (BR-MX-15, BR-LIB-42): at most 3 playlists count, and a playlist leader with a playlist-less
    // runner-up is not what happens here, because the leader is in none.
    @Test
    void the_nudge_counts_at_most_3_playlists() {
        MatchResult result = match(320.0, Map.of("b", List.of("P1", "P2", "P3", "P4")),
                daftPunk("a", "One More Time", 320.0), daftPunk("b", "One More Time", 342.6));

        assertThat(ids(result)).containsExactly("a", "b");
        assertThat(result.candidates().get(1).score()).isEqualTo(0.93);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #11
    @Test
    void nine_identical_files_list_8_and_are_ambiguous() {
        Track[] tracks = new Track[9];
        for (int i = 0; i < 9; i++) {
            tracks[i] = daftPunk("f" + i, "One More Time", 320.0);
        }

        MatchResult result = match(320.0, tracks);

        assertThat(result.candidates()).hasSize(8);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
    }

    // AC #6
    @Test
    void a_listed_candidate_under_060_leaves_the_song_unmatched() {
        MatchResult listed = match(320.0, new Track("o", "Other Band", "One More Night", 200.0));
        MatchResult dropped = match(320.0, daftPunk("l", "Da Funk (Live)", 500.0));

        assertThat(ids(listed)).containsExactly("o");
        assertThat(listed.candidates().get(0).score()).isEqualTo(0.4502);
        assertThat(listed.bucket()).isEqualTo(Bucket.UNMATCHED);
        assertThat(dropped.candidates()).isEmpty();
        assertThat(dropped.bucket()).isEqualTo(Bucket.UNMATCHED);
        assertThat(dropped.input().artist()).isEqualTo("Daft Punk");
        assertThat(dropped.input().title()).isEqualTo("One More Time");
    }

    @Test
    void a_query_without_artist_drops_the_artist_facet_from_the_mean() {
        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, null, "One More Time", 320.0),
                new LibraryIndex(List.of(daftPunk("f", "One More Time", 320.0))));

        ScoredCandidate only = result.candidates().get(0);
        assertThat(only.parts()).containsEntry("artist", null);
        assertThat(only.score()).isEqualTo(1.0);
    }
}
