package app.rekord.domain.matching;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single calibration point.
 *
 * <p>A pair is scored on four facets — title, artist, version, duration — and
 * combined with fixed weights, renormalised over whichever facets are actually
 * available. The constants below were tuned against rapidfuzz's exact output,
 * which is why {@link Fuzz} reimplements it rather than substituting something
 * close.
 *
 * <p>Buckets: {@code auto} needs a high, well-separated (or a playlist leader
 * over a runner-up in no playlist), version-compatible best candidate that is
 * also the requested song (same normalised artist and core title, UD-19.c);
 * anything merely plausible becomes {@code ambiguous} and the DJ picks; the
 * rest is {@code unmatched}. A small playlist nudge ({@link #ranked}) orders
 * candidates but never changes a score: every guard compares raw scores; the
 * nudge only decides who leads.
 */
public final class Score {

    /** Below this a candidate is not shown at all. */
    public static final double REPORT_THRESHOLD = 0.45;
    /** "Could plausibly be it" — enough to make the whole result ambiguous. */
    public static final double STRONG_THRESHOLD = 0.60;
    public static final double AUTO_SCORE = 0.82;
    /** Leader minus runner-up. A close pair is a question, not an answer, unless only the leader is in a playlist. */
    public static final double AUTO_MARGIN = 0.10;
    /** Never auto-pick a different version of the song. */
    public static final double AUTO_MIN_VERSION = 0.90;
    /** Roughly within 22 seconds. */
    public static final double AUTO_MIN_DURATION = 0.55;
    public static final int MAX_CANDIDATES = 8;
    /** Added to a candidate's ordering key per imported playlist it is in. */
    public static final double PLAYLIST_BONUS = 0.02;
    /** Playlists beyond this many add nothing. */
    public static final int PLAYLIST_BONUS_CAP = 3;

    public static final double WEIGHT_TITLE = 0.40;
    public static final double WEIGHT_ARTIST = 0.30;
    /** Replaces title and artist together, for filename-only candidates. */
    public static final double WEIGHT_COMBINED = 0.70;
    public static final double WEIGHT_VERSION = 0.15;
    public static final double WEIGHT_DURATION = 0.15;

    /** How bad is offering this when the query asked for the original? */
    private static final Set<String> LIGHT =
            Set.of("extended", "radio", "edit", "club", "mix", "version");
    private static final double REMIXER_SAME_MIN = 0.85;
    /** "(Remix)" against "(X Remix)" — probably the same thing. */
    private static final double REMIXER_ONE_SIDED = 0.85;
    private static final double REMASTER_FACTOR = 0.90;

    private Score() {
    }

    /** The ordering key: the score plus a small nudge per playlist. It never replaces the score. */
    public static double ranked(double score, List<String> playlists) {
        return score + PLAYLIST_BONUS * Math.min(playlists.size(), PLAYLIST_BONUS_CAP);
    }

    /**
     * Set semantics: extra tokens on one side do not hurt. Right for artists —
     * a query listing "A, B, C" against a tag holding only "A" — and for
     * filename blobs.
     */
    public static double ratio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        return Fuzz.tokenSetRatio(a, b) / 100.0;
    }

    /**
     * Order-insensitive but length-sensitive: "Anthem" must not score 1.0
     * against "Other Anthem Of Ours" the way a set comparison would.
     */
    public static double titleRatio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        return Fuzz.tokenSortRatio(a, b) / 100.0;
    }

    /** Character-level and strict: "Artist Two" and "Artist Three" are not the same. */
    private static boolean remixerSame(String a, String b) {
        return Fuzz.ratio(a, b) / 100.0 >= REMIXER_SAME_MIN;
    }

    public static double versionScore(Versions.TitleParts query, Versions.TitleParts candidate) {
        Set<String> querySet = new HashSet<>(query.descriptors());
        Set<String> candSet = new HashSet<>(candidate.descriptors());
        querySet.remove("remaster");
        candSet.remove("remaster");

        // A remaster is the same performance, so mismatching one costs a little
        // rather than a lot.
        boolean remasterDiffers = query.descriptors().contains("remaster")
                != candidate.descriptors().contains("remaster");
        double factor = remasterDiffers ? REMASTER_FACTOR : 1.0;

        if (querySet.equals(candSet)) {
            double base;
            if (query.remixer() != null && candidate.remixer() != null) {
                base = remixerSame(query.remixer(), candidate.remixer()) ? 1.0 : 0.20;
            } else if (query.remixer() != null || candidate.remixer() != null) {
                base = querySet.isEmpty() ? 1.0 : REMIXER_ONE_SIDED;
            } else {
                base = 1.0;
            }
            return base * factor;
        }

        double base;
        if (querySet.isEmpty() || candSet.isEmpty()) {
            // One side says nothing about its version. An "extended mix" offered
            // for a plain title is a near miss; a "live acapella" is not.
            Set<String> other = querySet.isEmpty() ? candSet : querySet;
            base = LIGHT.containsAll(other) ? 0.60 : 0.25;
        } else if (!java.util.Collections.disjoint(querySet, candSet)) {
            base = 0.40;
        } else {
            base = 0.20;
        }
        return base * factor;
    }

    /** Null when either duration is unknown, so the facet drops out of the mean. */
    public static Double durationScore(Double a, Double b) {
        if (a == null || b == null) {
            return null;
        }
        double delta = Math.abs(a - b);
        return delta <= 3 ? 1.0 : Math.max(0.0, 1 - (delta - 3) / 42);
    }

    /** Weighted mean over the facets that are present. */
    public static double combine(Map<String, Double> parts, Map<String, Double> weights) {
        double total = 0.0;
        double weightSum = 0.0;
        for (Map.Entry<String, Double> entry : parts.entrySet()) {
            Double value = entry.getValue();
            if (value == null) {
                continue;
            }
            double weight = weights.get(entry.getKey());
            total += weight * value;
            weightSum += weight;
        }
        return weightSum == 0.0 ? 0.0 : total / weightSum;
    }
}
