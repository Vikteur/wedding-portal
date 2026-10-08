package app.rekord.adapter.persistence.probe;

import app.rekord.usecase.probe.port.ProbeRowPort;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Test-only persistence adapter of the probe port: proves PIN-AC-0452, that a failing second call takes back the row
 * the first one wrote. It may go only when a real repository test proves both things it proves here: a row-level
 * rollback across a use case and an adapter of two modules, and the discovery of both beans through the Jandex index of
 * their module's tests jar. {@code RowRollbackIT} goes with it.
 */
@ApplicationScoped
public class JdbcProbeRowAdapter implements ProbeRowPort {

    private final DataSource dataSource;
    private volatile RuntimeException armedFailure;
    private volatile RuntimeException lastFailure;
    private volatile int insertedRows;

    public JdbcProbeRowAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(String marker) {
        // Agroal enlists the connection in the JTA transaction of the use case.
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("insert into probe_row (marker) values (?)")) {
            statement.setString(1, marker);
            insertedRows += statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void confirm(String marker) {
        RuntimeException failure = armedFailure;
        if (failure != null) {
            armedFailure = null;
            throw failure;
        }
    }

    public void failNextConfirm() {
        lastFailure = new IllegalStateException("confirm failed on purpose");
        armedFailure = lastFailure;
    }

    public RuntimeException failure() {
        return lastFailure;
    }

    public int insertedRows() {
        return insertedRows;
    }

    public void reset() {
        armedFailure = null;
        lastFailure = null;
        insertedRows = 0;
    }
}
