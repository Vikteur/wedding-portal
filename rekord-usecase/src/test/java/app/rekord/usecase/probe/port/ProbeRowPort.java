package app.rekord.usecase.probe.port;

/**
 * Test-only port of the probe use case: two calls that a real persistence adapter implements, so a test can prove the
 * use case's transaction rolls back a row the first call wrote when the second call fails (PIN-AC-0452).
 */
public interface ProbeRowPort {

    void insert(String marker);

    void confirm(String marker);
}
