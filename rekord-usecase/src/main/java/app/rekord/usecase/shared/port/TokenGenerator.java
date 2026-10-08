package app.rekord.usecase.shared.port;

/** Source of cryptographically secure random bytes (salts, tokens). */
public interface TokenGenerator {

    /** Returns {@code count} random bytes; a negative count is refused. */
    byte[] randomBytes(int count);
}
