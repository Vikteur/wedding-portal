package app.rekord.application.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Roots every REST resource under {@code /api}, as the contract's {@code servers: /api} says.
 *
 * <p>It is an annotation and not {@code quarkus.http.root-path}, because that property would also move
 * {@code /q/*}, and the readiness probe looks for {@code /q/*} at the root.
 */
@ApplicationPath("/api")
public class ApiApplication extends Application {
}
