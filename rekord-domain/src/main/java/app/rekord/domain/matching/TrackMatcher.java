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
 * <p>This is rekord-api's {@code Matcher.matchOne} without remembered choices (P3-E05-T02): facets, the weighted
 * mean, the 0.45 floor, the duration delta, the playlist nudge (it orders candidates, the bucket reads raw
 * scores) and the cap of 8. The result is auto only when the leader clears the score, margin (or a playlist
 * leader over a runner-up in no playlist), version and duration guards and is the requested song: same
 * normalised artist and core title (UD-19.c, {@link Signature#songOf}), so a query without an artist is never
 * auto.
 */
public final class TrackMatcher {

    private static final Map<String, Double> WEIGHTS = Map.of(
            "title", Score.WEIGHT_TITLE,
            "artist", Score.WEIGHT_ARTIST,
            "combined", Score.WEIGHT_COMBINED,
            "version", Score.WEIGHT_VERSION,
            "duration", Score.WEIGHT_DURATION);

    /**
     * One file, scored against a query, with the reasoning left visible.
     *
     * @param durationDeltaSec file duration minus query duration in seconds, rounded half-even to one decimal
     *                         (signed: negative means the file is shorter); null when either duration is unknown
     * @param playlists        the names of the imported playlists holding this file, in the order given; as
     *                         matchOne builds it, empty when the file is in none, never null, and unmodifiable
     *                         (List.copyOf), so later changes to the caller's list do not show here
     */
    public record ScoredCandidate(LibraryIndex.Track track, double score,
                                  Map<String, Double> parts, Versions.TitleParts version,
                                  Double durationDeltaSec, List<String> playlists) {
    }

    public record MatchResult(MatchQuery input, Versions.TitleParts inputVersion, Bucket bucket,
                              List<ScoredCandidate> candidates, String autoSelectedId) {
    }

    private TrackMatcher() {
    }

    public static MatchResult matchOne(MatchQuery query, LibraryIndex index) {
        return matchOne(query, index, Map.of());
    }

    /**
     * @param playlistsByTrackId a track id to the names of the imported playlists holding that file, in order;
     *                           a missing id, or a null map, means no playlist; values and names must not be
     *                           null
     */
    public static MatchResult matchOne(MatchQuery query, LibraryIndex index,
                                       Map<String, List<String>> playlistsByTrackId) {
        QueryText text = QueryText.of(query.artist(), query.title());

        List<ScoredCandidate> scored = new ArrayList<>();
        for (LibraryIndex.IndexedTrack candidate : index.candidates(text.tokens(), text.allNorm())) {
            ScoredCandidate result = score(text, query.durationSec(), candidate,
                    playlistsByTrackId == null ? List.of()
                            : playlistsByTrackId.getOrDefault(candidate.track().id(), List.of()));
            if (result.score() >= Score.REPORT_THRESHOLD) {
                scored.add(result);
            }
        }

        // A stable sort: equal ranks keep the order retrieval gave them. The playlist nudge orders only.
        scored.sort(Comparator.comparingDouble((ScoredCandidate c) -> Score.ranked(c.score(), c.playlists()))
                .reversed());
        if (scored.size() > Score.MAX_CANDIDATES) {
            scored = new ArrayList<>(scored.subList(0, Score.MAX_CANDIDATES));
        }

        Bucketed bucketed = bucket(query, scored);
        return new MatchResult(query, text.parts(), bucketed.bucket(), scored, bucketed.autoSelectedId());
    }

    private static ScoredCandidate score(QueryText query, Double queryDuration,
                                         LibraryIndex.IndexedTrack candidate,
                                         List<String> playlists) {
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
                facets, candidate.parts(), durationDelta(queryDuration, candidate.track().durationSec()),
                List.copyOf(playlists));
    }

    private record Bucketed(Bucket bucket, String autoSelectedId) {
    }

    private static Bucketed bucket(MatchQuery query, List<ScoredCandidate> scored) {
        if (scored.isEmpty()) {
            return new Bucketed(Bucket.UNMATCHED, null);
        }
        ScoredCandidate best = scored.getFirst();
        if (best.score() >= Score.AUTO_SCORE) {
            // A close call is still decided when the leader is in a playlist and the
            // runner-up is in none: one of the two is a track the DJ actually plays.
            // The size() == 1 case must short-circuit first, or get(1) reads past the end.
            boolean marginOk = scored.size() == 1
                    || best.score() - scored.get(1).score() >= Score.AUTO_MARGIN
                    || (!best.playlists().isEmpty() && scored.get(1).playlists().isEmpty());

            Double versionPart = best.parts().get("version");
            double version = versionPart == null ? 0.0 : versionPart;
            Double durationPart = best.parts().get("duration");
            // With no duration to compare, only an exact version match is trusted.
            boolean durationOk = durationPart != null
                    ? durationPart >= Score.AUTO_MIN_DURATION
                    : version == 1.0;

            String querySong = Signature.songOf(query.artist(), query.title());
            boolean sameSong = querySong != null
                    && querySong.equals(Signature.songOf(best.track().artist(), best.track().title()));

            if (marginOk && version >= Score.AUTO_MIN_VERSION && durationOk && sameSong) {
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

    /** File minus query, one decimal; null when either duration is unknown. */
    private static Double durationDelta(Double query, Double file) {
        return query == null || file == null ? null : round(file - query, 1);
    }

    /** Python's round(): half to even, which matters at the recorded precision. */
    private static double round(double value, int places) {
        return new BigDecimal(Double.toString(value)).setScale(places, RoundingMode.HALF_EVEN).doubleValue();
    }
}
