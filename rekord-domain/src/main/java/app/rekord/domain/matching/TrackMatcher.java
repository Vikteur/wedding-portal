package app.rekord.domain.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolving "this song, from a playlist" to "this file, that you own".
 *
 * <p>Named {@code TrackMatcher} because {@link Versions} already imports {@code java.util.regex.Matcher}.
 *
 * <p>This is the part of rekord-api's {@code Matcher.matchOne} that candidate retrieval reaches: facets, the
 * weighted mean, the 0.45 floor, the cap of 8 and the three-guard bucket. The playlist nudge, the duration delta
 * and the UD-19.c auto rule come with TASK-24.3, remembered choices with P3-E05-T02.
 */
public final class TrackMatcher {

    private static final Map<String, Double> WEIGHTS = Map.of(
            "title", Score.WEIGHT_TITLE,
            "artist", Score.WEIGHT_ARTIST,
            "combined", Score.WEIGHT_COMBINED,
            "version", Score.WEIGHT_VERSION,
            "duration", Score.WEIGHT_DURATION);

    /** One file, scored against a query, with the reasoning left visible. */
    public record ScoredCandidate(LibraryIndex.Track track, double score,
                                  Map<String, Double> parts, Versions.TitleParts version) {
    }

    public record MatchResult(MatchQuery input, Versions.TitleParts inputVersion, Bucket bucket,
                              List<ScoredCandidate> candidates, String autoSelectedId) {
    }

    private TrackMatcher() {
    }

    public static MatchResult matchOne(MatchQuery query, LibraryIndex index) {
        QueryText text = QueryText.of(query.artist(), query.title());

        List<ScoredCandidate> scored = new ArrayList<>();
        for (LibraryIndex.IndexedTrack candidate : index.candidates(text.tokens(), text.allNorm())) {
            ScoredCandidate result = score(text, query.durationSec(), candidate);
            if (result.score() >= Score.REPORT_THRESHOLD) {
                scored.add(result);
            }
        }

        // A stable sort: equal scores keep the order retrieval gave them.
        scored.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());
        if (scored.size() > Score.MAX_CANDIDATES) {
            scored = new ArrayList<>(scored.subList(0, Score.MAX_CANDIDATES));
        }

        Bucketed bucketed = bucket(scored);
        return new MatchResult(query, text.parts(), bucketed.bucket(), scored, bucketed.autoSelectedId());
    }

    private static ScoredCandidate score(QueryText query, Double queryDuration,
                                         LibraryIndex.IndexedTrack candidate) {
        Map<String, Double> facets = new LinkedHashMap<>();
        if (candidate.artistNorm() == null) {
            // A filename-only file: artist and title live in one undifferentiated string, so compare
            // against everything known about the query rather than pretending the halves can be told apart.
            facets.put("combined", Score.ratio(query.allNorm(), candidate.allNorm()));
        } else {
            facets.put("title", Score.titleRatio(query.coreNorm(), candidate.coreNorm()));
            facets.put("artist", query.artistNorm() == null ? null
                    : Score.ratio(query.artistNorm(), candidate.artistNorm()));
        }
        facets.put("version", Score.versionScore(query.parts(), candidate.parts()));
        facets.put("duration", Score.durationScore(queryDuration, candidate.track().durationSec()));

        return new ScoredCandidate(candidate.track(), round(Score.combine(facets, WEIGHTS), 4),
                facets, candidate.parts());
    }

    private record Bucketed(Bucket bucket, String autoSelectedId) {
    }

    private static Bucketed bucket(List<ScoredCandidate> scored) {
        if (scored.isEmpty()) {
            return new Bucketed(Bucket.UNMATCHED, null);
        }
        ScoredCandidate best = scored.getFirst();
        if (best.score() >= Score.AUTO_SCORE) {
            boolean marginOk = scored.size() == 1
                    || best.score() - scored.get(1).score() >= Score.AUTO_MARGIN;

            Double versionPart = best.parts().get("version");
            double version = versionPart == null ? 0.0 : versionPart;
            Double durationPart = best.parts().get("duration");
            // With no duration to compare, only an exact version match is trusted.
            boolean durationOk = durationPart != null
                    ? durationPart >= Score.AUTO_MIN_DURATION
                    : version == 1.0;

            if (marginOk && version >= Score.AUTO_MIN_VERSION && durationOk) {
                return new Bucketed(Bucket.AUTO, best.track().id());
            }
        }
        for (ScoredCandidate candidate : scored) {
            if (candidate.score() >= Score.STRONG_THRESHOLD) {
                return new Bucketed(Bucket.AMBIGUOUS, null);
            }
        }
        return new Bucketed(Bucket.UNMATCHED, null);
    }

    /** Python's round(): half to even, which matters at the recorded precision. */
    private static double round(double value, int places) {
        return new BigDecimal(Double.toString(value)).setScale(places, RoundingMode.HALF_EVEN).doubleValue();
    }
}
