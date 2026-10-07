package app.rekord.domain.matching;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Folds text to lowercase words: {@code "Étienne & Co"} to {@code "etienne and co"}.
 *
 * <p>Its output feeds {@link Signature}, whose id addresses every remembered version choice, so the output
 * must stay byte-exact: one character of drift orphans those choices without any error.
 *
 * <p>One deliberate deviation from rekord-api (UD-19.m6): the last step keeps Unicode letters and decimal digits
 * ({@code [^\p{L}\p{Nd}]+}) instead of {@code [^a-z0-9]+}, so Cyrillic, Greek and CJK text keeps its own
 * signature instead of collapsing to the empty string.
 */
public final class Normalize {

    /** Letters NFKD will not decompose to ASCII. Both cases, because this runs before lowercasing. */
    private static final Map<Character, String> SPECIAL = Map.ofEntries(
            Map.entry('ø', "o"), Map.entry('Ø', "o"),
            Map.entry('æ', "ae"), Map.entry('Æ', "ae"),
            Map.entry('œ', "oe"), Map.entry('Œ', "oe"),
            Map.entry('ß', "ss"),
            Map.entry('đ', "d"), Map.entry('Đ', "d"),
            Map.entry('ð', "d"), Map.entry('Ð', "d"),
            Map.entry('ł', "l"), Map.entry('Ł', "l"),
            Map.entry('þ', "th"), Map.entry('Þ', "th"));

    private static final Pattern COMBINING = Pattern.compile("\\p{M}+");
    private static final Pattern APOSTROPHES = Pattern.compile("[’'`´]");
    /** Only between ASCII word characters: "Simon + Garfunkel", not a leading "+". */
    private static final Pattern PLUS_BETWEEN_WORDS = Pattern.compile("(?<=\\w)\\+(?=\\w)");
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    private Normalize() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFKD);
        s = COMBINING.matcher(s).replaceAll("");

        StringBuilder translated = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String replacement = SPECIAL.get(c);
            translated.append(replacement == null ? c : replacement);
        }
        s = translated.toString().toLowerCase(Locale.ROOT);

        s = s.replace("&", " and ");
        s = PLUS_BETWEEN_WORDS.matcher(s).replaceAll(" and ");
        s = s.replace("$", "s");
        s = APOSTROPHES.matcher(s).replaceAll("");
        return NON_LETTER_OR_DIGIT.matcher(s).replaceAll(" ").strip();
    }

    public static List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
