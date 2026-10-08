package app.rekord.application.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Keeps ids and personal data out of metric labels. With {@code suppress4xx-errors} Micrometer tags the {@code uri} of
 * an untemplated answer of 400 or more UNKNOWN, but it records a request the client reset with the raw path, because
 * the reset hook hard-codes a status code of 0 and no suppression.
 */
public class MetricLabels {

    private static final String RESET = "RESET";
    private static final String UNKNOWN = "UNKNOWN";

    /** A request reset before matching carries no template: label it UNKNOWN, never its path. */
    @Produces
    @Singleton
    MeterFilter resetRequestsCarryNoPath() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().startsWith("http.server.") && RESET.equals(id.getTag("status"))) {
                    return id.replaceTags(Tags.of(id.getTagsAsIterable()).and("uri", UNKNOWN));
                }
                return id;
            }
        };
    }
}
