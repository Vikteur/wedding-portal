package app.rekord.domain.matching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Holds {@link Fuzz} to rapidfuzz's exact output on the 1,016 recorded pairs (BR-MX-10). */
class FuzzTest {

    private static final double TOLERANCE = 1e-9;

    private record Case(String a, String b, double ratio, double sort, double set) {
    }

    private static List<Case> fixture() throws Exception {
        try (InputStream in = FuzzTest.class.getResourceAsStream("/fuzz-fixture.json")) {
            assertThat(in).as("fuzz-fixture.json on the test classpath").isNotNull();
            JsonNode root = new ObjectMapper().readTree(in);
            List<Case> cases = new ArrayList<>();
            for (JsonNode node : root.get("cases")) {
                cases.add(new Case(
                        node.get("a").asText(),
                        node.get("b").asText(),
                        node.get("ratio").asDouble(),
                        node.get("token_sort_ratio").asDouble(),
                        node.get("token_set_ratio").asDouble()));
            }
            return cases;
        }
    }

    @Test
    void fixture_holds_the_1016_recorded_pairs() throws Exception {
        assertThat(fixture()).hasSize(1016);
    }

    @Test
    void ratio_matches_rapidfuzz_on_every_recorded_pair() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Case c : fixture()) {
            double actual = Fuzz.ratio(c.a(), c.b());
            if (Math.abs(actual - c.ratio()) > TOLERANCE) {
                wrong.add("ratio(%s, %s): expected %s, got %s".formatted(c.a(), c.b(), c.ratio(), actual));
            }
        }
        assertThat(wrong).as("pairs where the port disagrees with rapidfuzz").isEmpty();
    }

    @Test
    void token_sort_ratio_matches_rapidfuzz_on_every_recorded_pair() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Case c : fixture()) {
            double actual = Fuzz.tokenSortRatio(c.a(), c.b());
            if (Math.abs(actual - c.sort()) > TOLERANCE) {
                wrong.add("tokenSortRatio(%s, %s): expected %s, got %s".formatted(c.a(), c.b(), c.sort(), actual));
            }
        }
        assertThat(wrong).isEmpty();
    }

    @Test
    void token_set_ratio_matches_rapidfuzz_on_every_recorded_pair() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Case c : fixture()) {
            double actual = Fuzz.tokenSetRatio(c.a(), c.b());
            if (Math.abs(actual - c.set()) > TOLERANCE) {
                wrong.add("tokenSetRatio(%s, %s): expected %s, got %s".formatted(c.a(), c.b(), c.set(), actual));
            }
        }
        assertThat(wrong).isEmpty();
    }

    @Test
    void edge_cases() {
        assertThat(Fuzz.ratio("", "")).isEqualTo(100.0);
        assertThat(Fuzz.ratio("abc", "")).isEqualTo(0.0);
        assertThat(Fuzz.tokenSetRatio("", "abc")).isEqualTo(0.0);
        assertThat(Fuzz.tokenSetRatio("anthem", "other anthem of ours")).isEqualTo(100.0);
        assertThat(Fuzz.tokenSortRatio("anthem", "other anthem of ours")).isCloseTo(46.15384615384615, within(1e-9));
    }
}
