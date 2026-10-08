package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.rekord.adapter.persistence.probe.JdbcProbeRowAdapter;
import app.rekord.usecase.probe.ProbeRowUseCase;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import javax.sql.DataSource;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PIN-AC-0452 at row level: a use case of one library module, a persistence adapter of another, both discovered from
 * their module's Jandex index, and a real row that the transaction takes back. {@code probe_row} is test-only: created
 * and dropped here, never by a migration.
 */
@QuarkusTest
class RowRollbackIT {

    @Inject
    ProbeRowUseCase useCase;

    @Inject
    JdbcProbeRowAdapter adapter;

    @Inject
    DataSource dataSource;

    @BeforeEach
    void createTable() throws SQLException {
        execute("create table if not exists probe_row (marker text primary key)");
        execute("delete from probe_row");
        adapter.reset();
    }

    @AfterEach
    void dropTable() throws SQLException {
        execute("drop table if exists probe_row");
    }

    @Test
    void a_failing_second_port_call_rolls_back_the_row_the_first_call_inserted() throws SQLException {
        // Given an adapter whose confirm fails
        adapter.failNextConfirm();

        // When the use case saves a row
        assertThatThrownBy(() -> useCase.save("rolled-back"))
                // Then the caller gets the adapter's own exception
                .isSameAs(adapter.failure());

        // And the insert really ran, yet no row is visible on a fresh connection
        assertThat(adapter.insertedRows()).isEqualTo(1);
        assertThat(count("rolled-back")).isZero();
    }

    @Test
    void a_successful_call_keeps_its_row() throws SQLException {
        // Given an adapter that does not fail (the control for the rollback test)
        // When the use case saves a row
        useCase.save("kept");

        // Then the row is there, so the zero above comes from the rollback
        assertThat(adapter.insertedRows()).isEqualTo(1);
        assertThat(count("kept")).isEqualTo(1);
    }

    @Test
    void the_adapter_and_the_use_case_are_class_beans_discovered_through_their_modules_jandex_index() throws Exception {
        for (Class<?> type : new Class<?>[] {JdbcProbeRowAdapter.class, ProbeRowUseCase.class}) {
            // Given the bean of this class
            InjectableBean<?> bean = Arc.container().instance(type).getBean();

            // Then it is a class bean in application scope
            assertThat(bean.getKind()).isEqualTo(InjectableBean.Kind.CLASS);
            assertThat(bean.getScope()).isEqualTo(ApplicationScoped.class);

            // And its class comes from its module's tests jar, not from a class directory of this build
            // (the Quarkus class loader hides the origin, so the test classpath is read instead)
            String module = type == JdbcProbeRowAdapter.class ? "rekord-adapter" : "rekord-usecase";
            String classFile = type.getName().replace('.', '/') + ".class";
            List<Path> entries = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                    .map(Path::of)
                    .toList();
            assertThat(entries)
                    .filteredOn(Files::isDirectory)
                    .noneMatch(directory -> Files.exists(directory.resolve(classFile)));
            List<Path> jars = entries.stream()
                    .filter(e -> e.getFileName().toString().startsWith(module)
                            && e.getFileName().toString().endsWith("-tests.jar"))
                    .toList();
            assertThat(jars).hasSize(1);
            Path jar = jars.get(0);

            // And that jar holds a Jandex index that lists the class
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                var entry = jarFile.getEntry("META-INF/jandex.idx");
                assertThat(entry).as("META-INF/jandex.idx in " + jar).isNotNull();
                try (InputStream in = jarFile.getInputStream(entry)) {
                    Index index = new IndexReader(in).read();
                    assertThat(index.getClassByName(DotName.createSimple(type.getName()))).isNotNull();
                }
            }
        }
    }

    private int count(String marker) throws SQLException {
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("select count(*) from probe_row where marker = '" + marker + "'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
