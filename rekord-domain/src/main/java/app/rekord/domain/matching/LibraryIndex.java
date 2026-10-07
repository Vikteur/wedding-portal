package app.rekord.domain.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An inverted token index over one library (BR-MX-06, BR-MX-07).
 *
 * <p>Files are numbered in the order given, which is the library order. The
 * postings are kept in that ordinal order, because the candidate order and its
 * ties depend on it.
 */
public final class LibraryIndex {

    /** A token appearing in no more than this many tracks counts as rare. */
    private static final int RARE_DF = 50;
    private static final int MIN_TOKEN_HITS = 2;
    private static final int CANDIDATE_CAP = 300;
    private static final int FALLBACK_LIMIT = 50;
    /** rapidfuzz's 0-100 scale. */
    private static final double FALLBACK_CUTOFF = 50;

    /** One library track, with everything the scorer needs precomputed. */
    public record IndexedTrack(Track track, Versions.TitleParts parts, String coreNorm,
                               String artistNorm, String allNorm, Set<String> tokens) {
    }

    /** What the matcher needs of a track, so it does not depend on persistence. */
    public record Track(String id, String artist, String title, Double durationSec) {
    }

    private final List<IndexedTrack> items = new ArrayList<>();
    private final Map<String, List<Integer>> postings = new HashMap<>();
    private final Map<String, IndexedTrack> byTrackId = new HashMap<>();

    public LibraryIndex(List<Track> tracks) {
        for (Track track : tracks) {
            items.add(build(track));
        }
        for (int ordinal = 0; ordinal < items.size(); ordinal++) {
            for (String token : items.get(ordinal).tokens()) {
                postings.computeIfAbsent(token, t -> new ArrayList<>()).add(ordinal);
            }
        }
        for (IndexedTrack item : items) {
            byTrackId.put(item.track().id(), item);
        }
    }

    /** Read-only: one index is cached and shared by every match of a library. */
    public List<IndexedTrack> items() {
        return Collections.unmodifiableList(items);
    }

    public IndexedTrack byId(String trackId) {
        return byTrackId.get(trackId);
    }

    public int size() {
        return items.size();
    }

    /**
     * The files that share enough tokens with the query: two tokens, or one
     * that is rare in the library. Most hits first, then the later file first,
     * capped at {@value #CANDIDATE_CAP}.
     */
    public List<IndexedTrack> candidates(Set<String> queryTokens, String queryNorm) {
        Map<Integer, Integer> hits = new HashMap<>();
        Set<Integer> rareHit = new HashSet<>();

        for (String token : queryTokens) {
            List<Integer> posting = postings.get(token);
            if (posting == null || posting.isEmpty()) {
                continue;
            }
            boolean rare = posting.size() <= RARE_DF;
            for (Integer ordinal : posting) {
                hits.merge(ordinal, 1, Integer::sum);
                if (rare) {
                    rareHit.add(ordinal);
                }
            }
        }

        List<int[]> selected = new ArrayList<>();
        hits.forEach((ordinal, count) -> {
            if (count >= MIN_TOKEN_HITS || rareHit.contains(ordinal)) {
                selected.add(new int[]{count, ordinal});
            }
        });
        if (selected.isEmpty()) {
            return fallback(queryNorm);
        }

        // Descending by hit count, then by ordinal, as Python's sort(reverse=True)
        // on (count, ordinal) tuples does.
        selected.sort(Comparator.<int[]>comparingInt(pair -> pair[0]).reversed()
                .thenComparing(Comparator.<int[]>comparingInt(pair -> pair[1]).reversed()));

        List<IndexedTrack> result = new ArrayList<>();
        for (int i = 0; i < Math.min(selected.size(), CANDIDATE_CAP); i++) {
            result.add(items.get(selected.get(i)[1]));
        }
        return result;
    }

    /**
     * No token overlap at all: scan everything, keep the closest few. Equal
     * scores keep library order (ordinal ascending, UD-8), which is not the
     * order Python's sort gives.
     */
    private List<IndexedTrack> fallback(String queryNorm) {
        if (queryNorm == null || queryNorm.isEmpty() || items.isEmpty()) {
            return List.of();
        }
        record Scored(double score, int ordinal) {
        }
        List<Scored> scored = new ArrayList<>();
        for (int ordinal = 0; ordinal < items.size(); ordinal++) {
            double score = Fuzz.tokenSetRatio(queryNorm, items.get(ordinal).allNorm());
            if (score >= FALLBACK_CUTOFF) {
                scored.add(new Scored(score, ordinal));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparingInt(Scored::ordinal));

        List<IndexedTrack> result = new ArrayList<>();
        for (int i = 0; i < Math.min(scored.size(), FALLBACK_LIMIT); i++) {
            result.add(items.get(scored.get(i).ordinal()));
        }
        return result;
    }

    private static IndexedTrack build(Track track) {
        Versions.TitleParts parts = Versions.extract(track.title());
        String coreNorm = Normalize.normalize(parts.coreTitle());

        List<String> artistBits = new ArrayList<>();
        if (track.artist() != null && !track.artist().isEmpty()) {
            artistBits.add(track.artist());
        }
        artistBits.addAll(parts.featured());
        String artistNorm = Normalize.normalize(String.join(" ", artistBits));
        // Null rather than empty: a file with no artist tag is a different case
        // from one whose artist normalised away, and the scorer treats it so.
        if (artistNorm.isEmpty()) {
            artistNorm = null;
        }

        List<String> allBits = new ArrayList<>();
        if (artistNorm != null) {
            allBits.add(artistNorm);
        }
        if (!coreNorm.isEmpty()) {
            allBits.add(coreNorm);
        }
        if (parts.remixer() != null) {
            allBits.add(parts.remixer());
        }
        String allNorm = String.join(" ", allBits).strip();

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : allNorm.split(" ")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return new IndexedTrack(track, parts, coreNorm, artistNorm, allNorm, Collections.unmodifiableSet(tokens));
    }

    /** The tokens a query offers the index: title, artist and any remixer. */
    public static Set<String> queryTokens(String artistNorm, String coreNorm,
                                          Versions.TitleParts parts) {
        Set<String> tokens = new LinkedHashSet<>();
        addAll(tokens, coreNorm);
        if (artistNorm != null) {
            addAll(tokens, artistNorm);
        }
        if (parts.remixer() != null) {
            addAll(tokens, parts.remixer());
        }
        return tokens;
    }

    private static void addAll(Set<String> tokens, String text) {
        for (String token : text.split(" ")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
    }
}
