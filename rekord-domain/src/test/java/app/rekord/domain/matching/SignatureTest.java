package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class SignatureTest {

    static Stream<Arguments> spellingRows() {
        return Stream.of(
                Arguments.of("Artist One", "Anthem", "artist one|anthem||", "5640fae95ee899f8"),
                Arguments.of("artist one", "ANTHEM!", "artist one|anthem||", "5640fae95ee899f8"),
                Arguments.of("Justin Bieber", "Peaches (feat. Daniel Caesar)", "justin bieber|peaches||",
                        "79a721bf6e0b7f26"),
                Arguments.of("Justin Bieber", "Peaches", "justin bieber|peaches||", "79a721bf6e0b7f26"),
                Arguments.of(null, "Anthem", "|anthem||", "7298f481ab00c4b9"),
                Arguments.of("Artist Two", "Anthem", "artist two|anthem||", "077c6b511f37749e"));
    }

    @ParameterizedTest
    @MethodSource("spellingRows")
    void two_spellings_share_a_signature_and_featured_artists_are_left_out(
            String artist, String title, String signature, String id) {
        assertThat(Signature.signatureOf(artist, title)).isEqualTo(signature);
        assertThat(Signature.signatureId(artist, title)).isEqualTo(id);
    }

    @ParameterizedTest
    @MethodSource("versionRows")
    void versions_and_remixers_give_their_own_ids(String artist, String title, String id) {
        assertThat(Signature.signatureId(artist, title)).isEqualTo(id);
    }

    static Stream<Arguments> versionRows() {
        return Stream.of(
                Arguments.of("deadmau5", "Strobe", "46b0b3916e07bdf4"),
                Arguments.of("deadmau5", "Strobe (Radio Edit)", "4a6e7911f10c5a0b"),
                Arguments.of("deadmau5", "Strobe (Someone Remix)", "9126c0757348a076"),
                Arguments.of("deadmau5", "Strobe (Original Mix)", "46b0b3916e07bdf4"),
                Arguments.of("Daft Punk", "One More Time", "4cad791cad0c0451"));
    }

    @Test
    void a_remix_signature_names_the_descriptor_and_the_remixer() {
        assertThat(Signature.signatureOf("deadmau5", "Strobe (Someone Remix)"))
                .isEqualTo("deadmau5|strobe|remix|someone");
    }

    @Test
    void a_cyrillic_song_gets_its_own_signature_id() {
        assertThat(Signature.signatureOf("Кино", "Группа крови")).isEqualTo("кино|группа крови||");
        assertThat(Signature.signatureId("Кино", "Группа крови"))
                .isEqualTo("b13c96a2b1b93681")
                .isNotEqualTo("98c4b7d37a4c63c3");
    }
}
