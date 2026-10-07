package app.rekord.application.transaction;

/** Test-only port of the transaction boundary proof; it touches no database. */
public interface WritePort {

    void record(String write);

    void confirm(String write);
}
