package app.rekord.application.config;

import app.rekord.usecase.shared.port.TokenGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;

/** Random bytes from one {@link SecureRandom}. */
@ApplicationScoped
public class SecureRandomTokenGenerator implements TokenGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public byte[] randomBytes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        byte[] bytes = new byte[count];
        random.nextBytes(bytes);
        return bytes;
    }
}
