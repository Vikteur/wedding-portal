package app.rekord.domain.matching;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A stable identity for "the same song, in any playlist".
 *
 * <p>This is what a remembered version choice is filed under, so the decision
 * survives into every future playlist that asks for the song.
 *
 * <p>Built from the normalised artist, the core title and the version
 * descriptors — so "Strobe" and "Strobe (Radio Edit)" are deliberately
 * different songs with independent choices, while "Peaches (feat. Daniel
 * Caesar)" and "Peaches" are the same one. Featured artists are excluded on
 * purpose: playlists list them inconsistently, and a preference that only
 * applies half the time is worse than none.
 *
 * <p>{@link #songOf} is a second, looser identity: artist and core title
 * without the version, so "Strobe" and "Strobe (Radio Edit)" are the same
 * song there. The matcher uses it only to check that an auto pick is the
 * requested song (UD-19.c). It is never stored, so the byte-exact rule
 * below binds {@link #signatureOf} and {@link #signatureId} only.
 *
 * <p>Like {@link Normalize}, the output must stay byte-exact. Every
 * preference row in the database is keyed by {@link #signatureId}, and a change
 * here orphans all of them silently.
 */
public final class Signature {

    private Signature() {
    }

    public static String signatureOf(String artist, String title) {
        Versions.TitleParts parts = Versions.extract(title);
        return String.join("|",
                Normalize.normalize(artist == null ? "" : artist),
                Normalize.normalize(parts.coreTitle()),
                String.join("+", parts.descriptors()),
                Normalize.normalize(parts.remixer() == null ? "" : parts.remixer()));
    }

    /**
     * The song without its version: normalised artist and normalised core title, joined by "|". Null when the
     * artist normalises to empty, since then nothing says whose song it is (UD-19.c). As in {@link #signatureOf},
     * a featured artist in the title is left out; one written into the artist field ("A feat. B", "A & B")
     * stays part of the artist, so such a file is not the same song as a query for "A".
     */
    public static String songOf(String artist, String title) {
        String normArtist = Normalize.normalize(artist == null ? "" : artist);
        if (normArtist.isEmpty()) {
            return null;
        }
        return normArtist + "|" + Normalize.normalize(Versions.extract(title).coreTitle());
    }

    /** First 16 hex of the SHA-1. Preferences are addressed by this. */
    public static String signatureId(String artist, String title) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(signatureOf(artist, title).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by the platform", e);
        }
    }
}
