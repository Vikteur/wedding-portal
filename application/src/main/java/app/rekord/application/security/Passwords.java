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
 * <p>{@link #verify} reads the cost, the salt and the key length from the stored value, so the cost can rise later.
 */
@ApplicationScoped
public class Passwords implements PasswordHasher {

    private static final String PREFIX = "scrypt";
    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;
    private static final int KEY_BYTES = 32;
    private static final int SALT_BYTES = 16;

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

    /** True when {@code password} produces the stored hash; false, never an exception, on a null or malformed value. */
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
            if (expected.length == 0) {
                return false;
            }
            byte[] actual = derive(password, salt, n, r, p, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int n, int r, int p, int keyBytes) {
        return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, n, r, p, keyBytes);
    }
}
