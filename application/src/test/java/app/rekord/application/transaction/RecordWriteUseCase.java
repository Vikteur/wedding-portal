package app.rekord.application.transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Test-only use case written to the shape A3 allows: java types, its port and the two annotations. A3 does not refuse
 * its producer because that producer lives in test sources and this class is outside {@code app.rekord.usecase..}.
 */
@ApplicationScoped
public class RecordWriteUseCase {

    private final WritePort port;

    public RecordWriteUseCase(WritePort port) {
        this.port = port;
    }

    @Transactional
    public void save(String write) {
        port.record(write);
        port.confirm(write);
    }
}
