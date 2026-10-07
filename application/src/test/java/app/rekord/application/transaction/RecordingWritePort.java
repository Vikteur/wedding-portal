package app.rekord.application.transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.ArrayList;
import java.util.List;

/** Test-only port that records the transaction status it sees; staging and completion come with the rollback proof. */
@ApplicationScoped
public class RecordingWritePort implements WritePort {

    private final TransactionSynchronizationRegistry registry;
    private final List<Integer> statusesSeen = new ArrayList<>();

    public RecordingWritePort(TransactionSynchronizationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void record(String write) {
        statusesSeen.add(registry.getTransactionStatus());
    }

    @Override
    public void confirm(String write) {}

    public List<Integer> statusesSeen() {
        return List.copyOf(statusesSeen);
    }

    public void reset() {
        statusesSeen.clear();
    }
}
