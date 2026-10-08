package app.rekord.application.metrics;

import io.quarkus.vertx.http.runtime.filters.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only. Keeps a request that carries {@link #HEADER} in flight before JAX-RS matching: the filter never calls next
 * and never answers, so a test can reset the connection while the server still waits.
 */
@ApplicationScoped
public class HeldRequests {

    public static final String HEADER = "X-Test-Hold";

    private final Map<String, CompletableFuture<Void>> arrivals = new ConcurrentHashMap<>();

    void register(@Observes Filters filters) {
        filters.register(
                rc -> {
                    if (rc.request().getHeader(HEADER) == null) {
                        rc.next();
                        return;
                    }
                    arrival(rc.request().path()).complete(null);
                },
                10_000);
    }

    /** Completes when the request for this path has reached the filter. */
    public CompletableFuture<Void> arrival(String path) {
        return arrivals.computeIfAbsent(path, p -> new CompletableFuture<>());
    }
}
