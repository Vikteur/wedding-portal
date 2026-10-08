package app.rekord.usecase.identity;

import app.rekord.usecase.shared.port.IdGenerator;
import java.util.UUID;

final class SequentialIdGenerator implements IdGenerator {

    private long next = 1;

    @Override
    public UUID newId() {
        return new UUID(0L, next++);
    }
}
