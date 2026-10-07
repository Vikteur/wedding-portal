package app.rekord.application.transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Test-only port that stages a write in the current transaction and keeps it only when the transaction commits. A
 * write made without a transaction is kept at once, so a use case that loses its boundary leaves a visible half write.
 */
@ApplicationScoped
public class RecordingWritePort implements WritePort {

    private static final String STAGED = RecordingWritePort.class.getName() + ".staged";

    private final TransactionSynchronizationRegistry registry;
    private final List<Integer> statusesSeen = new ArrayList<>();
    private final List<Integer> completions = new ArrayList<>();
    private final List<String> committedWrites = new ArrayList<>();
    private boolean failNextConfirm;
    private IllegalStateException failure;

    public RecordingWritePort(TransactionSynchronizationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void record(String write) {
        int status = registry.getTransactionStatus();
        statusesSeen.add(status);
        if (status != Status.STATUS_ACTIVE) {
            committedWrites.add(write);
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> staged = (List<String>) registry.getResource(STAGED);
        if (staged == null) {
            List<String> fresh = new ArrayList<>();
            registry.putResource(STAGED, fresh);
            registry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int completionStatus) {
                    completions.add(completionStatus);
                    if (completionStatus == Status.STATUS_COMMITTED) {
                        committedWrites.addAll(fresh);
                    }
                }
            });
            staged = fresh;
        }
        staged.add(write);
    }

    @Override
    public void confirm(String write) {
        if (failNextConfirm) {
            failNextConfirm = false;
            failure = new IllegalStateException("confirm failed for " + write);
            throw failure;
        }
    }

    public void failNextConfirm() {
        failNextConfirm = true;
    }

    public IllegalStateException failure() {
        return failure;
    }

    public List<Integer> statusesSeen() {
        return List.copyOf(statusesSeen);
    }

    public List<Integer> completions() {
        return List.copyOf(completions);
    }

    public List<String> committedWrites() {
        return List.copyOf(committedWrites);
    }

    public void reset() {
        statusesSeen.clear();
        completions.clear();
        committedWrites.clear();
        failNextConfirm = false;
        failure = null;
    }
}
