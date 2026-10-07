package app.rekord.domain.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A query's artist and title, normalised the way {@link LibraryIndex} normalises a file.
 *
 * <p>{@code allNorm} leaves the remixer out, while {@code tokens} offers it to the index.
 */
public record QueryText(Versions.TitleParts parts, String coreNorm, String artistNorm,
                        String allNorm, Set<String> tokens) {

    public static QueryText of(String artist, String title) {
        Versions.TitleParts parts = Versions.extract(title);
        String coreNorm = Normalize.normalize(parts.coreTitle());

        List<String> artistBits = new ArrayList<>();
        if (artist != null && !artist.isEmpty()) {
            artistBits.add(artist);
        }
        artistBits.addAll(parts.featured());
        String artistNorm = Normalize.normalize(String.join(" ", artistBits));
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
        String allNorm = String.join(" ", allBits);

        return new QueryText(parts, coreNorm, artistNorm, allNorm,
                LibraryIndex.queryTokens(artistNorm, coreNorm, parts));
    }
}
