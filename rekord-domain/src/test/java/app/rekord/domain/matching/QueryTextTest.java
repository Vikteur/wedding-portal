package app.rekord.domain.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTextTest {

    @Test
    void a_query_offers_core_artist_and_remixer_tokens_but_its_all_text_leaves_the_remixer_out() {
        QueryText q = QueryText.of("Artist One", "Anthem (feat. Guest Two) (Someone Remix)");

        assertThat(q.tokens()).containsExactlyInAnyOrder("anthem", "artist", "guest", "one", "someone", "two");
        assertThat(q.allNorm()).isEqualTo("artist one guest two anthem");
    }

    @Test
    void a_query_without_artist_has_a_null_artist_norm() {
        QueryText q = QueryText.of("", "Dancin Quen");

        assertThat(q.artistNorm()).isNull();
        assertThat(q.allNorm()).isEqualTo("dancin quen");
    }
}
