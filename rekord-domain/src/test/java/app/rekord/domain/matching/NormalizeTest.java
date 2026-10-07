package app.rekord.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class NormalizeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Étienne de Crécy|etienne de crecy",
        "Møme & RØMANS|mome and romans",
        "KE$HA|kesha",
        "Don't Stop Believin'|dont stop believin",
        "Kungs & Cookin’ On 3 Burners|kungs and cookin on 3 burners",
        "A+B|a and b"
    })
    void normalises_latin_spellings_to_one_ascii_form(String input, String expected) {
        // Given a title or artist spelled in Latin script, When normalised, Then one ASCII form results
        assertThat(Normalize.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Señorita (ft. Camila)|senorita ft camila",
        "'  spaced   out  '|spaced out",
        "Cœur de pirate|coeur de pirate",
        "Blitzkrieg ß|blitzkrieg ss",
        "Łódź Þór Đorđe|lodz thor dorde",
        "''|''"
    })
    void folds_brackets_spacing_ligatures_and_special_letters(String input, String expected) {
        assertThat(Normalize.normalize(input)).isEqualTo(expected);
    }

    @Test
    void normalises_null_to_the_empty_string() {
        assertThat(Normalize.normalize(null)).isEmpty();
    }

    @Test
    void composed_and_decomposed_accents_normalise_alike() {
        assertThat(Normalize.normalize("Cr\u00E9cy")).isEqualTo("crecy");
        assertThat(Normalize.normalize("Cre\u0301cy")).isEqualTo("crecy");
    }

    @Test
    void tokenises_on_the_normalised_spaces() {
        assertThat(Normalize.tokenize("The XX - Intro!")).isEqualTo(List.of("the", "xx", "intro"));
        assertThat(Normalize.tokenize("")).isEmpty();
        assertThat(Normalize.tokenize("...")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Кино|кино",
        "Мумий Тролль|мумии тролль",
        "Άλφα Βήτα|αλφα βητα",
        "東京事変|東京事変",
        "ABBA Ёлка|abba елка",
        "ＡＢＢＡ|abba",
        "Кино+Ария|кино ария"
    })
    void keeps_letters_and_digits_of_every_script(String input, String expected) {
        assertThat(Normalize.normalize(input)).isEqualTo(expected);
    }
}
