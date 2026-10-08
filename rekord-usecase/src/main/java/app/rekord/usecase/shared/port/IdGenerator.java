package app.rekord.usecase.shared.port;

import java.util.UUID;

/** Source of new identifiers; a use case asks it and never calls {@code UUID.randomUUID()}. */
public interface IdGenerator {

    UUID newId();
}
