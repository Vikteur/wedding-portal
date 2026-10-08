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
    // matcher classes (recorded in TASK-24.3). They pin the facets, the score order and the bucket: rekord-api's
    // three guards plus UD-19.c's same-song rule.

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

    // PIN-14-0369: the delta is signed, file minus query; -2.25 rounds half-even to -2.2.
    @Test
    void a_shorter_file_has_a_negative_delta() {
        MatchResult result = match(320.0, daftPunk("f", "One More Time", 317.75));

        assertThat(result.candidates().get(0).durationDeltaSec()).isEqualTo(-2.2);
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
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsExactly(1.0, 1.0);
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
        assertThat(result.candidates().get(1).playlists()).containsExactly("P1");
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // BR-MX-15: the margin guard reads raw scores. Raw margin 1.0 - 0.9214 = 0.0786 < 0.10; the ranked keys
    // (1.06 vs 0.9414) would clear it. Both files are in a playlist, so the lone-member clause does not apply.
    @Test
    void the_margin_guard_reads_raw_scores_not_the_nudged_keys() {
        MatchResult result = match(320.0, Map.of(
                        "a", List.of("P1", "P2", "P3"),
                        "b", List.of("P1")),
                daftPunk("a", "One More Time", 320.0), daftPunk("b", "One More Time", 345.0));

        assertThat(ids(result)).containsExactly("a", "b");
        assertThat(result.candidates()).extracting(ScoredCandidate::score).containsExactly(1.0, 0.9214);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #10 (BR-MX-15, BR-LIB-42): at most 3 playlists count. The lone-member clause does not apply: the
    // leader is in no playlist.
    @Test
    void the_nudge_counts_at_most_3_playlists() {
        MatchResult result = match(320.0, Map.of("b", List.of("P1", "P2", "P3", "P4")),
                daftPunk("a", "One More Time", 320.0), daftPunk("b", "One More Time", 342.6));

        assertThat(ids(result)).containsExactly("a", "b");
        assertThat(result.candidates().get(1).score()).isEqualTo(0.93);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // Design: a null membership map means no file is in a playlist, as in rekord-api.
    @Test
    void a_null_playlist_map_means_no_playlists() {
        MatchResult result = match(320.0, (Map<String, List<String>>) null,
                daftPunk("x", "One More Time", 320.0), daftPunk("y", "One More Time", 320.0));

        assertThat(ids(result)).containsExactly("y", "x");
        assertThat(result.candidates()).extracting(ScoredCandidate::playlists).containsOnly(List.of());
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
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

    // BR-MX-15, BR-MX-17: the nudge orders before the cap, so a playlist member retrieval ranks 9th still leads.
    @Test
    void a_playlist_member_survives_the_cap_of_8_and_leads() {
        Track[] tracks = new Track[9];
        for (int i = 0; i < 9; i++) {
            tracks[i] = daftPunk("t" + i, "One More Time", 320.0);
        }

        MatchResult result = match(320.0, Map.of("t0", List.of("Peak hour")), tracks);

        assertThat(ids(result)).containsExactly("t0", "t8", "t7", "t6", "t5", "t4", "t3", "t2");
        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("t0");
    }

    // AC #6
    @Test
    void a_listed_candidate_under_060_leaves_the_song_unmatched() {
        MatchResult listed = match(320.0, new Track("o", "Other Band", "One More Night", 200.0));
        MatchResult dropped = match(320.0, daftPunk("l", "Da Funk (Live)", 500.0));

        assertThat(ids(listed)).containsExactly("o");
        assertThat(listed.candidates().get(0).score()).isEqualTo(0.4502);
        assertThat(listed.bucket()).isEqualTo(Bucket.UNMATCHED);
        // The file is retrieved, so it is the 0.45 floor that drops it, not the token gate.
        QueryText query = QueryText.of("Daft Punk", "One More Time");
        assertThat(new LibraryIndex(List.of(daftPunk("l", "Da Funk (Live)", 500.0)))
                .candidates(query.tokens(), query.allNorm()))
                .extracting(c -> c.track().id()).containsExactly("l");
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
        // AC #5b (UD-19.c): a query without an artist is never auto.
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #5a (UD-19.c): a file without an artist tag is never the requested song.
    @Test
    void a_filename_only_file_is_never_auto() {
        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "Daft Punk", "One More Time", null),
                new LibraryIndex(List.of(new Track("f", null, "Daft Punk One More Time", null))));

        ScoredCandidate only = result.candidates().get(0);
        assertThat(only.parts().get("combined")).isEqualTo(1.0);
        assertThat(only.parts().get("version")).isEqualTo(1.0);
        assertThat(only.parts().get("duration")).isNull();
        assertThat(only.score()).isEqualTo(1.0);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // AC #12 (UD-19.c): rekord-api answers auto; the core titles differ, so this is ambiguous.
    @Test
    void a_different_core_title_is_never_auto() {
        MatchResult result = TrackMatcher.matchOne(
                new MatchQuery(0, "The Chainsmokers ft. Daya", "on't Let Me Down (Intro)", 228.0),
                new LibraryIndex(List.of(new Track("f", "The Chainsmokers ft. Daya",
                        "Don't Let Me Down (Intro)", 228.0))));

        ScoredCandidate only = result.candidates().get(0);
        assertThat(only.score()).isEqualTo(0.9163);
        assertThat(only.parts().get("artist")).isEqualTo(1.0);
        assertThat(only.parts().get("version")).isEqualTo(1.0);
        assertThat(only.parts().get("duration")).isEqualTo(1.0);
        assertThat(result.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(result.autoSelectedId()).isNull();
    }

    // UD-19.c must not be too strict: a featured artist in the file's title is left out of the song.
    @Test
    void a_featured_artist_in_the_file_title_still_allows_auto() {
        MatchResult result = match(320.0, daftPunk("f", "One More Time (feat. Romanthony)", 320.0));

        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("f");
    }

    // UD-19.c: the song's artist is the whole normalised artist field, so an extra artist on either side is a
    // different song even at score 1.0; a featured artist in the query's title is left out, as in the file's.
    @Test
    void an_extra_artist_on_either_side_is_never_auto_even_at_1() {
        MatchResult extraInQuery = TrackMatcher.matchOne(
                new MatchQuery(0, "Daft Punk, Romanthony", "One More Time", 320.0),
                new LibraryIndex(List.of(daftPunk("f", "One More Time", 320.0))));
        MatchResult extraInFile = match(320.0, new Track("f", "Daft Punk & Romanthony", "One More Time", 320.0));

        assertThat(extraInQuery.candidates().get(0).score()).isEqualTo(1.0);
        assertThat(extraInQuery.bucket()).isEqualTo(Bucket.AMBIGUOUS);
        assertThat(extraInFile.candidates().get(0).score()).isEqualTo(1.0);
        assertThat(extraInFile.bucket()).isEqualTo(Bucket.AMBIGUOUS);

        // "feat." written into the artist field stays part of the artist; only a title's featured artist is dropped
        MatchResult featInFileArtist = match(320.0,
                new Track("f", "Daft Punk feat. Romanthony", "One More Time", 320.0));
        assertThat(featInFileArtist.candidates().get(0).score()).isEqualTo(1.0);
        assertThat(featInFileArtist.bucket()).isEqualTo(Bucket.AMBIGUOUS);
    }

    @Test
    void a_featured_artist_in_the_query_title_still_allows_auto() {
        MatchResult result = TrackMatcher.matchOne(
                new MatchQuery(0, "Daft Punk", "One More Time (feat. Romanthony)", 320.0),
                new LibraryIndex(List.of(daftPunk("f", "One More Time", 320.0))));

        assertThat(result.bucket()).isEqualTo(Bucket.AUTO);
        assertThat(result.autoSelectedId()).isEqualTo("f");
    }
}
