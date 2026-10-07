package app.rekord.domain.matching;

import java.util.ArrayList;
import java.util.HashMap;
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

    public List<IndexedTrack> items() {
        return items;
    }

    public IndexedTrack byId(String trackId) {
        return byTrackId.get(trackId);
    }

    public int size() {
        return items.size();
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
        return new IndexedTrack(track, parts, coreNorm, artistNorm, allNorm, tokens);
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
