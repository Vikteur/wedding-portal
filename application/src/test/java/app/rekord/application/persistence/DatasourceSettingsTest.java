package app.rekord.application.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DatasourceSettingsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path RESOURCES = REPO_ROOT.resolve("application/src/main/resources");
    private static final String SERVER_ERROR_DETAIL =
            "quarkus.datasource.jdbc.additional-jdbc-properties.logServerErrorDetail";
    private static final Pattern DB_KIND = Pattern.compile("(%[\\w-]+\\.)?quarkus\\.datasource\\.(.+\\.)?db-kind");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "rekord-api|sqlite|\\.db(?![\\w-])|\\.sqlite3?\\b|import|jdbc:[^\\s]*/rekord\\b", Pattern.CASE_INSENSITIVE);

    private static Properties settings() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(RESOURCES.resolve("application.properties"))) {
            properties.load(reader);
        }
        return properties;
    }

    private static void assertNoProfileOverride(Properties properties, String key) {
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> name.matches("%[\\w-]+\\." + Pattern.quote(key)));
    }

    @Test
    void every_profile_uses_postgresql() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then the unprofiled db-kind exists and every db-kind is postgresql
        assertThat(properties.getProperty("quarkus.datasource.db-kind")).isEqualTo("postgresql");
        assertThat(properties.stringPropertyNames())
                .filteredOn(name -> DB_KIND.matcher(name).matches())
                .allSatisfy(name -> assertThat(properties.getProperty(name)).isEqualTo("postgresql"));
    }

    @Test
    void flyway_migrates_at_start_from_classpath_db_migration_without_baseline_on_migrate() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then Flyway is set up and no profile overrides it
        assertThat(properties.getProperty("quarkus.flyway.migrate-at-start")).isEqualTo("true");
        assertThat(properties.getProperty("quarkus.flyway.locations")).isEqualTo("classpath:db/migration");
        assertThat(properties.getProperty("quarkus.flyway.baseline-on-migrate")).isEqualTo("false");
        for (String key : new String[] {
            "quarkus.flyway.migrate-at-start", "quarkus.flyway.locations", "quarkus.flyway.baseline-on-migrate"
        }) {
            assertNoProfileOverride(properties, key);
        }
    }

    @Test
    void hibernate_validates_and_uses_utc_for_jdbc() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then Hibernate validates, uses UTC, and nothing generates or loads a schema
        assertThat(properties.getProperty("quarkus.hibernate-orm.schema-management.strategy"))
                .isEqualTo("validate");
        assertThat(properties.getProperty("quarkus.hibernate-orm.jdbc.timezone")).isEqualTo("UTC");
        assertNoProfileOverride(properties, "quarkus.hibernate-orm.schema-management.strategy");
        assertNoProfileOverride(properties, "quarkus.hibernate-orm.jdbc.timezone");
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> name.contains("database.generation") || name.contains("sql-load-script"));
    }

    @Test
    void dev_services_pins_postgres_17_alpine_and_test_does_not_reuse() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then the image is pinned and the test profile starts fresh containers
        assertThat(properties.getProperty("quarkus.datasource.devservices.image-name"))
                .isEqualTo("postgres:17-alpine");
        assertThat(properties.getProperty("%test.quarkus.datasource.devservices.reuse"))
                .isEqualTo("false");
    }

    @Test
    void no_setting_names_a_rekord_api_database_a_sqlite_file_or_an_import() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then no key or value points at the old application's data
        for (String name : properties.stringPropertyNames()) {
            assertThat(FORBIDDEN.matcher(name).find()).as("key %s", name).isFalse();
            assertThat(FORBIDDEN.matcher(properties.getProperty(name)).find())
                    .as("value of %s", name)
                    .isFalse();
        }
        assertThat(properties.stringPropertyNames())
                .filteredOn(name -> name.endsWith("db-name"))
                .allSatisfy(name -> assertThat(properties.getProperty(name)).isNotEqualToIgnoringCase("rekord"));
        assertThat(properties.getProperty("%dev.quarkus.datasource.devservices.db-name"))
                .isEqualTo("wedding_portal");
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            assertThat(files.map(path -> path.getFileName().toString())).doesNotContain("import.sql");
        }
    }

    @Test
    void prod_reads_the_connection_from_the_environment_only() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then the prod connection is exactly three environment references
        assertThat(properties.getProperty("%prod.quarkus.datasource.jdbc.url")).isEqualTo("${DB_URL}");
        assertThat(properties.getProperty("%prod.quarkus.datasource.username")).isEqualTo("${DB_USER}");
        assertThat(properties.getProperty("%prod.quarkus.datasource.password")).isEqualTo("${DB_PASSWORD}");
    }

    @Test
    void the_server_error_detail_is_off_in_every_profile_and_no_profile_turns_it_on() throws IOException {
        // Given the shipped settings
        Properties properties = settings();

        // Then the unprofiled key is false, so every profile (production included) inherits it
        assertThat(properties.getProperty(SERVER_ERROR_DETAIL)).isEqualTo("false");
        // And no other key sets the pgjdbc property: not a %test. or %dev. rewrite, not a %prod. override
        assertThat(properties.stringPropertyNames())
                .filteredOn(name -> name.toLowerCase(Locale.ROOT).contains("logservererrordetail"))
                .containsExactly(SERVER_ERROR_DETAIL);
    }
}
