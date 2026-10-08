package app.rekord.application.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/** The one {@link Clock} of the application: the system clock in UTC. */
public class ClockProducer {

    @Produces
    @Singleton
    public Clock clock() {
        return Clock.systemUTC();
    }
}
