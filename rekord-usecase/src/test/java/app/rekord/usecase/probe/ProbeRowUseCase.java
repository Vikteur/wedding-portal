package app.rekord.usecase.probe;

import app.rekord.usecase.probe.port.ProbeRowPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Test-only use case: inserts a row, then confirms it, in one transaction. A failing confirm must roll the row back
 * (PIN-AC-0452).
 */
@ApplicationScoped
public class ProbeRowUseCase {

    private final ProbeRowPort port;

    public ProbeRowUseCase(ProbeRowPort port) {
        this.port = port;
    }

    @Transactional
    public void save(String marker) {
        port.insert(marker);
        port.confirm(marker);
    }
}
