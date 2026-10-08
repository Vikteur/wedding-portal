package app.rekord.application.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Test helper: a handler on the root logger that keeps every record any logger emits. The handler accepts all levels,
 * but records below a logger's enabled level (INFO by default) are dropped before they reach it; DEBUG capture is
 * TASK-6.5.
 */
public final class LogCapture extends Handler {

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger root = Logger.getLogger("");

    public LogCapture() {
        setLevel(Level.ALL);
    }

    public void start() {
        root.addHandler(this);
    }

    public void stop() {
        root.removeHandler(this);
    }

    public List<LogRecord> records() {
        return List.copyOf(records);
    }

    /** Records at ERROR (SEVERE) or above. */
    List<LogRecord> errors() {
        return records.stream()
                .filter(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
                .toList();
    }

    /** Records written by the envelope mapper's own logger that reached the handler (INFO and above by default). */
    List<LogRecord> fromMapper() {
        return records.stream()
                .filter(r -> ErrorEnvelopeMapper.class.getName().equals(r.getLoggerName()))
                .toList();
    }

    /** Message, parameters and the rendered stack trace of a record. */
    public static String text(LogRecord r) {
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        pw.println(r.getMessage());
        if (r.getParameters() != null) {
            for (Object p : r.getParameters()) {
                pw.println(p);
            }
        }
        if (r.getThrown() != null) {
            r.getThrown().printStackTrace(pw);
        }
        pw.flush();
        return out.toString();
    }

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
}
