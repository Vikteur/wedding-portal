package app.rekord.domain.matching;

import app.rekord.domain.matching.LibraryIndex.IndexedTrack;
import app.rekord.domain.matching.LibraryIndex.Track;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryIndexTest {

    @Test
    void a_tagged_file_indexes_artist_plus_featured_core_and_remixer() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("1", "Artist One", "Anthem (feat. Guest Two) (Someone Remix)", null)));

        IndexedTrack item = index.items().get(0);
        assertThat(item.artistNorm()).isEqualTo("artist one guest two");
        assertThat(item.coreNorm()).isEqualTo("anthem");
        assertThat(item.allNorm()).isEqualTo("artist one guest two anthem someone");
        assertThat(item.tokens()).hasSize(6);
    }

    @Test
    void a_file_without_artist_keeps_the_dash_in_its_core() {
        LibraryIndex index = new LibraryIndex(List.of(new Track("2", null, "Artist One - Anthem", null)));

        IndexedTrack item = index.items().get(0);
        assertThat(item.artistNorm()).isNull();
        assertThat(item.coreNorm()).isEqualTo("artist one anthem");
        assertThat(item.allNorm()).isEqualTo("artist one anthem");
    }

    @Test
    void the_index_numbers_files_in_the_order_given() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("c", "A", "One", null),
                new Track("a", "B", "Two", null),
                new Track("b", "C", "Three", null)));

        assertThat(index.items()).extracting(i -> i.track().id()).containsExactly("c", "a", "b");
        assertThat(index.size()).isEqualTo(3);
        assertThat(index.byId("a").track().title()).isEqualTo("Two");
    }

    private static List<String> ids(List<IndexedTrack> items) {
        return items.stream().map(i -> i.track().id()).toList();
    }

    private static List<Track> bandLibraryWithSolo() {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i <= 50; i++) {
            tracks.add(new Track("b" + i, "Band", "Love", null));
        }
        tracks.add(new Track("z", "Solo", "Love", null));
        return tracks;
    }

    private static List<IndexedTrack> candidatesFor(LibraryIndex index, String artist, String title) {
        QueryText q = QueryText.of(artist, title);
        return index.candidates(q.tokens(), q.allNorm());
    }

    @Test
    void a_rare_token_alone_admits_a_file() {
        LibraryIndex index = new LibraryIndex(bandLibraryWithSolo());

        assertThat(ids(candidatesFor(index, "Solo", "Love"))).containsExactly("z");
    }

    @Test
    void two_shared_tokens_admit_a_file_but_one_common_token_does_not() {
        LibraryIndex index = new LibraryIndex(bandLibraryWithSolo());

        List<String> expected = new ArrayList<>();
        for (int i = 50; i >= 0; i--) {
            expected.add("b" + i);
        }
        assertThat(ids(candidatesFor(index, "Band", "Love"))).containsExactlyElementsOf(expected);
    }

    @Test
    void candidates_are_capped_at_300_last_in_library_order_first() {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            tracks.add(new Track("f" + i, "Band", "Love", null));
        }
        LibraryIndex index = new LibraryIndex(tracks);

        List<String> expected = new ArrayList<>();
        for (int i = 399; i >= 100; i--) {
            expected.add("f" + i);
        }
        assertThat(ids(index.candidates(Set.of("band", "love"), "band love"))).containsExactlyElementsOf(expected);
    }

    @Test
    void more_hits_rank_before_fewer() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("three", "Band", "Love Song", null),
                new Track("two-a", "Band", "Love", null),
                new Track("two-b", "Band", "Song", null)));

        assertThat(ids(candidatesFor(index, "Band", "Love Song"))).containsExactly("three", "two-b", "two-a");
    }

    @Test
    void with_no_shared_token_the_fallback_lists_equal_scores_in_library_order() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("a", null, "Dancing Queen", null),
                new Track("b", null, "Dancing Queen", null)));

        assertThat(ids(candidatesFor(index, "", "Dancin Quen"))).containsExactly("a", "b");
    }

    @Test
    void the_fallback_returns_nothing_for_an_empty_query() {
        LibraryIndex index = new LibraryIndex(List.of(new Track("1", "Daft Punk", "One More Time", null)));

        assertThat(candidatesFor(index, "", "")).isEmpty();
    }

    @Test
    void the_fallback_keeps_scores_of_50_and_more_best_first() {
        LibraryIndex index = new LibraryIndex(List.of(
                new Track("low", null, "Zebra Xylophone", null),
                new Track("worse", null, "Dancing Queen Extra Words Here", null),
                new Track("best", null, "Dancing Queen", null)));

        assertThat(Fuzz.tokenSetRatio("dancin quen", "zebra xylophone")).isLessThan(50);
        assertThat(Fuzz.tokenSetRatio("dancin quen", "dancing queen extra words here")).isGreaterThanOrEqualTo(50);
        assertThat(ids(index.candidates(Set.of("xqz"), "dancin quen"))).containsExactly("best", "worse");
    }

    @Test
    void the_fallback_keeps_at_most_50() {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            tracks.add(new Track("f" + i, null, "Dancing Queen", null));
        }
        LibraryIndex index = new LibraryIndex(tracks);

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            expected.add("f" + i);
        }
        assertThat(ids(index.candidates(Set.of("xqz"), "dancin quen"))).containsExactlyElementsOf(expected);
    }
}
