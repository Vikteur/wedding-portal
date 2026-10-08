package app.rekord.application.security;

import app.rekord.usecase.identity.port.PasswordHasher;
import app.rekord.usecase.shared.port.TokenGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.bouncycastle.crypto.generators.SCrypt;

/**
 * scrypt password hashing in rekord-api's stored format {@code scrypt$N$r$p$<salt hex>$<hash hex>}.
 *
 * <p>The format is kept, not replaced, so the hashes rekord-api already stores verify without a password reset, and
 * because each hash records the parameters it was made with, the cost can rise later without invalidating any of
 * them. bcrypt would be the usual default; it cannot verify those hashes.
 *
 * <p>{@link #verify} reads the cost, the salt and the key length from the stored value, but only inside bounds (see
 * the constants): the value comes from a row, a corrupt or imported one, and must not decide how much memory or time
 * one check costs. Beyond the bounds it answers {@code false} before any scrypt work.
 */
@ApplicationScoped
public class Passwords implements PasswordHasher {

    private static final String PREFIX = "scrypt";
    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;
    private static final int KEY_BYTES = 32;
    private static final int SALT_BYTES = 16;

    // What a stored value may ask for. One derivation needs about 128 * r * N bytes: 128 MiB at the largest cost and
    // block size together, 16 MiB at the D1 value. The ceilings leave room to raise the cost later, not to claim
    // gigabytes; the salt and key floors keep a hash from being trivial, and a key floor above zero also keeps a
    // value with no key at all (which every password "derives") from verifying anything.
    private static final int MAX_COST = 1 << 16;
    private static final int MAX_BLOCK_SIZE = 16;
    private static final int MAX_PARALLELISM = 4;
    private static final int MIN_SALT_BYTES = 8;
    private static final int MIN_KEY_BYTES = 16;
    private static final int MAX_BYTES = 64;

    private final TokenGenerator tokens;

    public Passwords(TokenGenerator tokens) {
        this.tokens = tokens;
    }

    @Override
    public String hash(String password) {
        byte[] salt = tokens.randomBytes(SALT_BYTES);
        byte[] key = derive(password, salt, N, R, P, KEY_BYTES);
        HexFormat hex = HexFormat.of();
        return String.join("$", PREFIX, Integer.toString(N), Integer.toString(R), Integer.toString(P),
                hex.formatHex(salt), hex.formatHex(key));
    }

    /**
     * True when {@code password} produces the stored hash; false, never an exception, on a null or malformed value or
     * one that asks for more than the bounds allow: N a power of two from 2 to 2^16, r 1 to 16, p 1 to 4, a salt of 8
     * to 64 bytes and a key of 16 to 64 bytes. A corrupt row fails to sign in; it does not fail the request.
     */
    public boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("[$]", -1);
        if (parts.length != 6 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int n = Integer.parseInt(parts[1]);
            int r = Integer.parseInt(parts[2]);
            int p = Integer.parseInt(parts[3]);
            byte[] salt = HexFormat.of().parseHex(parts[4]);
            byte[] expected = HexFormat.of().parseHex(parts[5]);
            if (!withinBounds(n, r, p, salt.length, expected.length)) {
                return false;
            }
            byte[] actual = derive(password, salt, n, r, p, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean withinBounds(int n, int r, int p, int saltBytes, int keyBytes) {
        return n >= 2 && n <= MAX_COST && Integer.bitCount(n) == 1
                && r >= 1 && r <= MAX_BLOCK_SIZE
                && p >= 1 && p <= MAX_PARALLELISM
                && saltBytes >= MIN_SALT_BYTES && saltBytes <= MAX_BYTES
                && keyBytes >= MIN_KEY_BYTES && keyBytes <= MAX_BYTES;
    }

    private static byte[] derive(String password, byte[] salt, int n, int r, int p, int keyBytes) {
        return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, n, r, p, keyBytes);
    }
}
