package app.rekord.domain.matching;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoreTest {

    // AC #1 (BR-MX-12): the version facet for the eleven title pairs, computed with rekord-api's Score.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Song|Song|1.0",
            "Song (A Remix)|Song (A Remix)|1.0",
            "Song|Song (Extended Mix)|0.6",
            "Song|Song (A Remix)|0.25",
            "Song (A Remix)|Song (B Remix)|0.2",
            "Song|Song (Remastered 2011)|0.9",
            "Song (Extended Mix)|Song (A Remix)|0.2",
            "Song (Remix)|Song (A Remix)|0.85",
            "Titel - Live - Radio Edit|Titel (Radio Edit)|0.4",
            "Song|Song (Live)|0.25",
            "Song (Remastered)|Song (Extended Mix)|0.54"})
    void the_version_facet_matches_rekord_api_for_every_title_pair(String a, String b, double expected) {
        // 0.54 is 0.6 x 0.9, so every value is compared within a rounding error.
        assertThat(Score.versionScore(Versions.extract(a), Versions.extract(b))).isCloseTo(expected, within(1e-9));
    }

    // AC #2 (BR-MX-13): 1.0 within 3 seconds, falling to 0.0 at 45, null when a duration is unknown.
    @Test
    void the_duration_facet_is_1_within_3_seconds_then_falls_to_0_at_45_and_null_when_unknown() {
        assertThat(Score.durationScore(200.0, 203.0)).isEqualTo(1.0);
        assertThat(Score.durationScore(200.0, 224.0)).isEqualTo(0.5);
        assertThat(Score.durationScore(200.0, 245.0)).isEqualTo(0.0);
        assertThat(Score.durationScore(200.0, null)).isNull();
    }
}
