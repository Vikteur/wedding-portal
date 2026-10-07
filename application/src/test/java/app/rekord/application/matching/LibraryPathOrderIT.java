package app.rekord.application.matching;

import static org.assertj.core.api.Assertions.assertThat;

import app.rekord.domain.matching.Bucket;
import app.rekord.domain.matching.LibraryIndex;
import app.rekord.domain.matching.LibraryIndex.Track;
import app.rekord.domain.matching.MatchQuery;
import app.rekord.domain.matching.TrackMatcher;
import app.rekord.domain.matching.TrackMatcher.MatchResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The library order is the order the database returns for {@code order by t.path} with no collation
 * (PIN-14-0373, PIN-14-0512). The tables are test DDL in a throwaway container: the library schema arrives with
 * TASK-22.1, and this is not a migration.
 *
 * <p>The same files are read from two databases, one with the C collation and one with ICU en-US, which sort the
 * four paths differently. The expectation is always read from the database, never written down.
 */
class LibraryPathOrderIT {

    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:17-alpine");
    private static final String C = "order_c";
    private static final String ICU = "order_icu";

    @BeforeAll
    static void startDatabasesWithFourTaggedFiles() throws SQLException {
        DATABASE.start();
        try (Connection c = connect(DATABASE.getDatabaseName());
                Statement s = c.createStatement()) {
            s.execute("create database " + C + " template template0 locale 'C'");
            s.execute("create database " + ICU + " template template0 locale_provider icu icu_locale 'en-US'");
        }
        for (String database : List.of(C, ICU)) {
            try (Connection c = connect(database);
                    Statement s = c.createStatement()) {
                s.execute("create table sources (id bigint primary key, library_id bigint not null)");
                s.execute("""
                        create table tracks (id text primary key, path text not null, artist text, title text,
                                             duration_sec double precision)""");
                s.execute("create table track_sources (track_id text not null, source_id bigint not null)");
                s.execute("insert into sources (id, library_id) values (1, 1)");
                String[][] files = {
                    {"t1", "/m/b.mp3"}, {"t2", "/m/B.mp3"}, {"t3", "/m/a-b.mp3"}, {"t4", "/m/ab.mp3"},
                };
                for (String[] file : files) {
                    s.execute("insert into tracks (id, path, artist, title) values ('%s', '%s', 'Band', 'Love')"
                            .formatted(file[0], file[1]));
                    s.execute("insert into track_sources (track_id, source_id) values ('%s', 1)".formatted(file[0]));
                }
            }
        }
    }

    @AfterAll
    static void stopDatabase() {
        DATABASE.stop();
    }

    private static Connection connect(String database) throws SQLException {
        String url = DATABASE.getJdbcUrl().replace("/" + DATABASE.getDatabaseName(), "/" + database);
        return DriverManager.getConnection(url, DATABASE.getUsername(), DATABASE.getPassword());
    }

    /** rekord-api's LibraryRepository.libraryTracks query, character for character (no COLLATE). */
    private static List<Track> libraryTracks(String database, long libraryId) throws SQLException {
        List<Track> tracks = new ArrayList<>();
        try (Connection c = connect(database);
                PreparedStatement ps = c.prepareStatement("""
                        select distinct t.* from tracks t
                          join track_sources ts on ts.track_id = t.id
                          join sources s on s.id = ts.source_id
                         where s.library_id = ?
                         order by t.path
                        """)) {
            ps.setLong(1, libraryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tracks.add(new Track(rs.getString("id"), rs.getString("artist"), rs.getString("title"), null));
                }
            }
        }
        return tracks;
    }

    private static List<String> ids(List<Track> tracks) {
        return tracks.stream().map(Track::id).toList();
    }

    @Test
    void the_two_collations_sort_the_paths_differently() throws SQLException {
        assertThat(ids(libraryTracks(C, 1))).isNotEqualTo(ids(libraryTracks(ICU, 1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {C, ICU})
    void the_index_numbers_files_in_the_order_the_database_returned(String database) throws SQLException {
        List<Track> rows = libraryTracks(database, 1);

        LibraryIndex index = new LibraryIndex(rows);

        assertThat(rows).hasSize(4);
        assertThat(index.items()).extracting(i -> i.track().id()).containsExactlyElementsOf(ids(rows));
    }

    @ParameterizedTest
    @ValueSource(strings = {C, ICU})
    void ties_are_listed_in_reverse_database_order_whatever_the_collation(String database) throws SQLException {
        List<Track> rows = libraryTracks(database, 1);
        LibraryIndex index = new LibraryIndex(rows);

        MatchResult result = TrackMatcher.matchOne(new MatchQuery(0, "Band", "Love", null), index);

        List<String> reversed = new ArrayList<>(ids(rows));
        Collections.reverse(reversed);
        assertThat(result.candidates()).hasSize(4);
        assertThat(result.candidates()).allSatisfy(c -> assertThat(c.score()).isEqualTo(1.0));
        assertThat(result.candidates()).extracting(c -> c.track().id()).containsExactlyElementsOf(reversed);
        assertThat(result.bucket()).isNotEqualTo(Bucket.UNMATCHED);
    }
}
