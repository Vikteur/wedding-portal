package app.rekord.domain.matching;

import app.rekord.domain.matching.LibraryIndex.Track;
import app.rekord.domain.matching.TrackMatcher.MatchResult;
import app.rekord.domain.matching.TrackMatcher.ScoredCandidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
