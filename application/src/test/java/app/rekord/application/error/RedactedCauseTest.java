package app.rekord.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;

class RedactedCauseTest {

    private static String printed(Throwable t) {
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    private static String formatted(Throwable t) {
        ExtLogRecord record = new ExtLogRecord(
                java.util.logging.Level.SEVERE, "Unhandled exception", RedactedCauseTest.class.getName());
        record.setThrown(t);
        return new PatternFormatter("%s%e").format(record);
    }

    @Test
    void keeps_class_names_and_frames_and_drops_every_message() {
        // Given: messages, cause, suppressed exception and a cause cycle, all carrying sentinels
        IllegalStateException original = ErrorEnvelopeProbeResource.chain();

        // When
        Throwable redacted = RedactedCause.of(original);

        // Then
        assertThat(redacted.toString()).isEqualTo("java.lang.IllegalStateException");
        assertThat(redacted.getMessage()).isNull();
        assertThat(redacted.getLocalizedMessage()).isNull();
        assertThat(redacted.getStackTrace()).isEqualTo(original.getStackTrace());
        assertThat(redacted.getCause().toString()).isEqualTo("java.sql.SQLException");
        assertThat(redacted.getCause().getStackTrace()).isEqualTo(original.getCause().getStackTrace());
        assertThat(redacted.getSuppressed()).hasSize(1);
        assertThat(redacted.getSuppressed()[0].toString()).isEqualTo("java.lang.IllegalArgumentException");
    }

    @Test
    void the_rendered_trace_holds_the_class_names_and_frames_but_no_sentinel_value() {
        // Given
        Throwable redacted = RedactedCause.of(ErrorEnvelopeProbeResource.chain());

        // When / Then: printStackTrace and JBoss's pattern formatter, which the console and file handlers use
        for (String rendered : new String[] {printed(redacted), formatted(redacted)}) {
            assertThat(rendered)
                    .contains("java.lang.IllegalStateException")
                    .contains("Caused by: java.sql.SQLException")
                    .contains("java.lang.IllegalArgumentException")
                    .contains("\tat app.rekord.application.error.ErrorEnvelopeProbeResource.chain(");
            for (String sentinel : ErrorEnvelopeProbeResource.SENTINELS) {
                assertThat(rendered).doesNotContain(sentinel);
            }
        }
    }

    @Test
    void a_cause_cycle_terminates() {
        assertThat(RedactedCause.of(ErrorEnvelopeProbeResource.chain())).isNotNull();
    }
}
