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
