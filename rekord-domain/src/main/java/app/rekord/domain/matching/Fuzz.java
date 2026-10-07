package app.rekord.domain.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The three similarity measures the matcher is calibrated against.
 *
 * <p>Written out rather than taken from a library, deliberately. The Python
 * uses rapidfuzz, whose {@code ratio} is normalised <em>Indel</em> similarity —
 * {@code 2 × LCS / (len(a) + len(b))}. The obvious Java equivalent,
 * {@code me.xdrop:fuzzywuzzy}, is a port of the older difflib-based
 * implementation, whose greedy block matching under-counts against a true LCS
 * and therefore returns different numbers.
 *
 * <p>Different numbers would matter here more than usual: every threshold in
 * {@code Score} — 0.82 to auto-pick, a 0.10 margin, 0.90 on version — was tuned
 * against rapidfuzz's exact output. Swapping in something merely similar would
 * silently re-tune the whole matcher.
 *
 * <p>So the definition is implemented directly, and
 * {@code FuzzTest} checks the result against 1,016 pairs recorded
 * from the Python.
 */
public final class Fuzz {

    private Fuzz() {
    }

    /**
     * Normalised Indel similarity, 0–100.
     *
     * <p>Indel distance is Levenshtein without substitutions, so
     * {@code distance = len(a) + len(b) − 2 × LCS} and the normalised
     * similarity reduces to {@code 2 × LCS / (len(a) + len(b))}.
     */
    public static double ratio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return a != null && b != null && a.isEmpty() && b.isEmpty() ? 100.0 : 0.0;
        }
        int lcs = lcsLength(a, b);
        return 2.0 * lcs / (a.length() + b.length()) * 100.0;
    }

    /**
     * Order-insensitive but length-sensitive: the tokens of each side are
     * sorted and rejoined before comparing.
     *
     * <p>Used for titles, where "Anthem" must <em>not</em> score 100 against
     * "Other Anthem Of Ours" the way a set comparison would.
     */
    public static double tokenSortRatio(String a, String b) {
        return ratio(sortedTokens(a), sortedTokens(b));
    }

    /**
     * Set semantics: extra tokens on one side do not hurt.
     *
     * <p>Right for artists — a query of "A, B, C" against a tag holding just
     * "A" — and for filename blobs.
     */
    public static double tokenSetRatio(String a, String b) {
        Set<String> tokensA = new TreeSet<>(splitTokens(a));
        Set<String> tokensB = new TreeSet<>(splitTokens(b));

        // Zero when either side has no tokens, even though ratio("", "") is
        // 100. rapidfuzz guards this deliberately: two strings with nothing in
        // them are not a match, they are an absence of one.
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        List<String> intersection = new ArrayList<>();
        List<String> onlyA = new ArrayList<>();
        List<String> onlyB = new ArrayList<>();
        for (String token : tokensA) {
            (tokensB.contains(token) ? intersection : onlyA).add(token);
        }
        for (String token : tokensB) {
            if (!tokensA.contains(token)) {
                onlyB.add(token);
            }
        }

        String shared = String.join(" ", intersection);
        String sharedPlusA = join(shared, String.join(" ", onlyA));
        String sharedPlusB = join(shared, String.join(" ", onlyB));

        return Math.max(ratio(shared, sharedPlusA),
                Math.max(ratio(shared, sharedPlusB), ratio(sharedPlusA, sharedPlusB)));
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + " " + b;
    }

    private static String sortedTokens(String s) {
        List<String> tokens = splitTokens(s);
        tokens.sort(String::compareTo);
        return String.join(" ", tokens);
    }

    private static List<String> splitTokens(String s) {
        List<String> tokens = new ArrayList<>();
        if (s == null) {
            return tokens;
        }
        for (String token : s.split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * Two rows rather than the full table: titles and artist lists are short,
     * but this runs across the whole candidate set for every query in a
     * playlist, so the allocation is worth avoiding.
     */
    private static int lcsLength(String a, String b) {
        int n = b.length();
        int[] previous = new int[n + 1];
        int[] current = new int[n + 1];
        for (int i = 1; i <= a.length(); i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                current[j] = ca == b.charAt(j - 1)
                        ? previous[j - 1] + 1
                        : Math.max(previous[j], current[j - 1]);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[n];
    }
}
