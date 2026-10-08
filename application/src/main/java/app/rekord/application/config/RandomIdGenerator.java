package app.rekord.application.config;

import app.rekord.usecase.shared.port.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Random (version 4) identifiers. */
@ApplicationScoped
public class RandomIdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
