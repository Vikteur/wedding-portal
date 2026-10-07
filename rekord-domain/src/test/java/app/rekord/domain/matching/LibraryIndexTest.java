package app.rekord.domain.matching;

import app.rekord.domain.matching.LibraryIndex.IndexedTrack;
import app.rekord.domain.matching.LibraryIndex.Track;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
