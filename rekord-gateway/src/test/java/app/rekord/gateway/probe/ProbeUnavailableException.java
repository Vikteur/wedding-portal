package app.rekord.gateway.probe;

/** Test-local typed failure: callers of the probe gateway see no HTTP type. */
final class ProbeUnavailableException extends RuntimeException {

    ProbeUnavailableException(String message) {
        super(message);
    }

    ProbeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
