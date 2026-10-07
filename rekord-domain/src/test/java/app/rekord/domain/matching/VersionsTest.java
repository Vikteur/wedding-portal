package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VersionsTest {

    private static Arguments row(String title, String core, List<String> descriptors, String remixer) {
        return Arguments.of(title, core, descriptors, remixer);
    }

    private static Arguments feat(String title, String core, List<String> descriptors, List<String> featured) {
        return Arguments.of(title, core, descriptors, featured);
    }

    static Stream<Arguments> versionRows() {
        return Stream.of(
                row("Am I Wrong", "Am I Wrong", List.of(), null),
                row("Am I Wrong (Original Mix)", "Am I Wrong", List.of(), null),
                row("Substitution (Purple Disco Machine Remix)", "Substitution", List.of("remix"),
                        "purple disco machine"),
                row("Am I Wrong [Superdiscount Extended Edit]", "Am I Wrong", List.of("extended"), "superdiscount"),
                row("Animals - Radio Edit", "Animals", List.of("radio"), null),
                row("Animals (Radio Edit)", "Animals", List.of("radio"), null),
                row("Levels (Remastered 2011)", "Levels", List.of("remaster"), null),
                row("Divide - Part 2", "Divide - Part 2", List.of(), null));
    }

    @ParameterizedTest
    @MethodSource("versionRows")
    void strips_version_brackets_and_suffixes_into_descriptors(
            String title, String core, List<String> descriptors, String remixer) {
        Versions.TitleParts parts = Versions.extract(title);

        assertThat(parts.coreTitle()).isEqualTo(core);
        assertThat(parts.descriptors()).isEqualTo(descriptors);
        assertThat(parts.remixer()).isEqualTo(remixer);
    }

    static Stream<Arguments> synonymRows() {
        return Stream.of(
                row("(I Can't Get No) Satisfaction", "(I Can't Get No) Satisfaction", List.of(), null),
                row("One (Club Mix)", "One", List.of("club"), null),
                row("Song (VIP)", "Song", List.of("vip"), null),
                row("Song (Acoustic)", "Song", List.of("acoustic"), null),
                row("Song (Sped Up)", "Song", List.of("spedup"), null),
                row("Faded (Restrung)", "Faded (Restrung)", List.of(), null),
                row("Greyhound - Extended Mix", "Greyhound", List.of("extended"), null),
                row("Titel - Live - Radio Edit", "Titel", List.of("live", "radio"), null));
    }

    @ParameterizedTest
    @MethodSource("synonymRows")
    void keeps_non_version_brackets_and_canonicalises_synonyms(
            String title, String core, List<String> descriptors, String remixer) {
        Versions.TitleParts parts = Versions.extract(title);

        assertThat(parts.coreTitle()).isEqualTo(core);
        assertThat(parts.descriptors()).isEqualTo(descriptors);
        assertThat(parts.remixer()).isEqualTo(remixer);
    }

    static Stream<Arguments> limitRows() {
        return Stream.of(
                row("Song (Rmx)", "Song", List.of("remix"), null),
                row("Song (A Cappella)", "Song", List.of("acapella"), null),
                row("Song (Slowed and Reverb)", "Song", List.of("slowed"), null),
                row("Song {Dub}", "Song", List.of("dub"), null),
                row("Song (A B C D Remix)", "Song", List.of("remix"), "a b c d"),
                row("Song (A B C D E Remix)", "Song (A B C D E Remix)", List.of(), null),
                row("Extended Mix", "Extended Mix", List.of(), null));
    }

    @ParameterizedTest
    @MethodSource("limitRows")
    void applies_the_leftover_limit_and_the_empty_core_rule(
            String title, String core, List<String> descriptors, String remixer) {
        Versions.TitleParts parts = Versions.extract(title);

        assertThat(parts.coreTitle()).isEqualTo(core);
        assertThat(parts.descriptors()).isEqualTo(descriptors);
        assertThat(parts.remixer()).isEqualTo(remixer);
    }

    static Stream<Arguments> featuredRows() {
        return Stream.of(
                feat("Peaches (feat. Daniel Caesar & Giveon)", "Peaches", List.of(),
                        List.of("daniel caesar and giveon")),
                feat("One More Time (feat. Someone) [Club Mix]", "One More Time", List.of("club"),
                        List.of("someone")),
                feat("Song (with Dua Lipa)", "Song", List.of(), List.of("dua lipa")),
                feat("I'm the One feat. DJ Khaled", "I'm the One", List.of(), List.of("dj khaled")),
                feat("Solo ft Demi Lovato", "Solo", List.of(), List.of("demi lovato")));
    }

    @ParameterizedTest
    @MethodSource("featuredRows")
    void reads_featured_artists_from_segments_and_inline_tails(
            String title, String core, List<String> descriptors, List<String> featured) {
        Versions.TitleParts parts = Versions.extract(title);

        assertThat(parts.coreTitle()).isEqualTo(core);
        assertThat(parts.descriptors()).isEqualTo(descriptors);
        assertThat(parts.featured()).isEqualTo(featured);
    }
}
