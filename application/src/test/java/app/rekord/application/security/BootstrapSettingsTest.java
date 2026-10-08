package app.rekord.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The bootstrap address and password come from the deployment secrets only (D6): no file in git names a value. */
class BootstrapSettingsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path PROPERTIES = REPO_ROOT.resolve("application/src/main/resources/application.properties");

    @Test
    void no_shipped_file_sets_the_bootstrap_address_or_password() throws IOException {
        List<String> hits = new ArrayList<>();
        for (Path file : shippedResourceFiles()) {
            String name = REPO_ROOT.relativize(file).toString().replace('\\', '/');
            for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
                if (settingOf(line, "email") || settingOf(line, "password")) {
                    hits.add(name + ": " + line.trim());
                }
            }
        }
        assertThat(hits).as("lines that set app.bootstrap.email or app.bootstrap.password").isEmpty();
    }

    @Test
    void application_properties_sets_only_the_display_name_and_the_business_name() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(PROPERTIES, StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        }

        assertThat(properties.stringPropertyNames())
                .filteredOn(name -> name.contains("app.bootstrap."))
                .containsExactlyInAnyOrder("app.bootstrap.display-name", "app.bootstrap.org-name");
        assertThat(properties.getProperty("app.bootstrap.display-name")).isEqualTo("The planner");
        assertThat(properties.getProperty("app.bootstrap.org-name")).isEqualTo("Rekord Match");
    }

    @Test
    void the_comment_names_the_two_environment_variables() throws IOException {
        assertThat(Files.readString(PROPERTIES, StandardCharsets.ISO_8859_1))
                .contains("APP_BOOTSTRAP_EMAIL")
                .contains("APP_BOOTSTRAP_PASSWORD");
    }

    /** A line that sets {@code app.bootstrap.<key>}, in any profile, as a property or a yaml-style entry. */
    private static boolean settingOf(String line, String key) {
        String text = line.strip().toLowerCase(Locale.ROOT);
        if (text.startsWith("#")) {
            return false;
        }
        return text.matches("(%[\\w-]+[.])?app[.]bootstrap[.]" + key + "\\s*[=:].*");
    }

    /** Every regular file under a {@code src/main/resources} folder of the repository: what the build ships. */
    private static List<Path> shippedResourceFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(REPO_ROOT, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
                String name = dir.getFileName().toString();
                if (!dir.equals(REPO_ROOT) && (name.startsWith(".") || name.equals("build"))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (dir.endsWith(Path.of("src", "main", "resources"))) {
                    try (Stream<Path> inside = Files.walk(dir)) {
                        inside.filter(Files::isRegularFile).forEach(files::add);
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
