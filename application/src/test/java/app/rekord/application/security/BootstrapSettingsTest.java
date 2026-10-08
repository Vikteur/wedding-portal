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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The bootstrap address and password come from the deployment secrets only (D6). This test reads line by line, and
 * only these places: the files under every {@code src/main/resources} folder, and the {@code Dockerfile*}, compose and
 * {@code .env*} files at the repository root and every file under {@code .github}. A line there that gives
 * {@code app.bootstrap.email}, {@code app.bootstrap.password} or their environment variables a value fails the test.
 *
 * <p>It does not see: configuration nested over several lines (YAML written as {@code app:} / {@code bootstrap:} /
 * {@code password:}); a value that reaches the setting by indirection ({@code ARG PW=value} followed by
 * {@code ENV APP_BOOTSTRAP_PASSWORD=$PW}; a default written in the reference itself, {@code ${PW:-value}}, is seen);
 * a value that starts with {@code $} and a letter, which reads as a variable reference; and any file outside the
 * places above, such as {@code src/main/docker}, a {@code deploy} folder or Kubernetes manifests. None of those
 * exists today; the one that appears first has to be added to the places above.
 */
class BootstrapSettingsTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path PROPERTIES = REPO_ROOT.resolve("application/src/main/resources/application.properties");

    /**
     * {@code app.bootstrap.email} or {@code app.bootstrap.password}, or their environment variables, followed by a value:
     * {@code KEY=v}, {@code KEY: v}, Docker's {@code ENV KEY v}, {@code -Dkey=v}. A reference such as
     * {@code ${{ secrets.KEY }}}, {@code $KEY} or {@code "${KEY}"} names no value and passes; a reference with a
     * default ({@code ${KEY:-v}}, {@code ${KEY:=v}}, {@code ${KEY-v}}) or a value that starts with {@code $} and
     * a digit or symbol ({@code $3cret}) does not.
     */
    private static final Pattern DEPLOYMENT_SETTING = Pattern.compile(
            "(?i)(\\benv\\s+app_bootstrap_(email|password)\\s+|app[._]bootstrap[._](email|password)\\s*[=:]\\s*)"
                    + "(?![\"']?\\$(\\{(?![^}]*:?[-=])|[a-z_]))[^\\s,)}]");

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
    void no_image_compose_file_or_ci_workflow_sets_the_bootstrap_address_or_password() throws IOException {
        List<String> hits = new ArrayList<>();
        for (Path file : deploymentFiles()) {
            String name = REPO_ROOT.relativize(file).toString().replace('\\', '/');
            for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
                if (!line.strip().startsWith("#") && DEPLOYMENT_SETTING.matcher(line).find()) {
                    hits.add(name + ": " + line.trim());
                }
            }
        }
        assertThat(deploymentFiles()).contains(REPO_ROOT.resolve("Dockerfile"));
        assertThat(hits).as("lines that give the bootstrap address or password a value").isEmpty();
    }

    @Test
    void the_deployment_check_sees_every_form_of_a_value_and_lets_a_secret_reference_pass() {
        assertThat(List.of(
                        "ENV APP_BOOTSTRAP_PASSWORD=made-up-pass-1234",
                        "ENV APP_BOOTSTRAP_PASSWORD made-up-pass-1234",
                        "  APP_BOOTSTRAP_EMAIL: admin@example.com",
                        "ENV JAVA_OPTS=\"-Dapp.bootstrap.password=made-up-pass-1234\"",
                        "APP_BOOTSTRAP_PASSWORD=\"made-up-pass-1234\"",
                        "APP_BOOTSTRAP_PASSWORD=$3cret-made-up-1234",
                        "ENV APP_BOOTSTRAP_PASSWORD=${PW:-made-up-pass-1234}",
                        "ENV APP_BOOTSTRAP_PASSWORD=${PW:=made-up-pass-1234}",
                        "ENV APP_BOOTSTRAP_PASSWORD=${PW-made-up-pass-1234}"))
                .allMatch(line -> DEPLOYMENT_SETTING.matcher(line).find());
        assertThat(List.of(
                        "  APP_BOOTSTRAP_PASSWORD: ${{ secrets.APP_BOOTSTRAP_PASSWORD }}",
                        "  - APP_BOOTSTRAP_EMAIL=$APP_BOOTSTRAP_EMAIL",
                        "  - APP_BOOTSTRAP_EMAIL=\"${APP_BOOTSTRAP_EMAIL}\"",
                        "  - APP_BOOTSTRAP_EMAIL=${APP_BOOTSTRAP_EMAIL}",
                        "Set APP_BOOTSTRAP_EMAIL, APP_BOOTSTRAP_PASSWORD (secrets)."))
                .noneMatch(line -> DEPLOYMENT_SETTING.matcher(line).find());
    }

    @Test
    void the_property_check_sees_the_key_in_every_profile_and_ignores_comments() {
        assertThat(List.of(
                        "app.bootstrap.password=made-up-pass-1234",
                        "  APP.Bootstrap.Email = admin@example.com",
                        "app.bootstrap.password: made-up-pass-1234",
                        "%dev.app.bootstrap.password=made-up-pass-1234",
                        "%dev,test.app.bootstrap.password=made-up-pass-1234",
                        "%prod-eu.app.bootstrap.email=admin@example.com"))
                .allMatch(line -> settingOf(line, "password") || settingOf(line, "email"));
        assertThat(List.of(
                        "# app.bootstrap.password=made-up-pass-1234",
                        "app.bootstrap.display-name=The planner",
                        "app.bootstrap.org-name=Rekord Match",
                        "# they come from the deployment environment (APP_BOOTSTRAP_EMAIL, APP_BOOTSTRAP_PASSWORD)"))
                .noneMatch(line -> settingOf(line, "password") || settingOf(line, "email"));
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

    /**
     * A line that sets {@code app.bootstrap.<key>} on one line, in any profile (also {@code %dev,test.}), as a property
     * ({@code key=v}) or a flat YAML-style entry ({@code key: v}). Nested YAML is not read.
     */
    private static boolean settingOf(String line, String key) {
        String text = line.strip().toLowerCase(Locale.ROOT);
        if (text.startsWith("#")) {
            return false;
        }
        return text.matches("(%[\\w,-]+[.])?app[.]bootstrap[.]" + key + "\\s*[=:].*");
    }

    /** The image, any compose or {@code .env} file at the root, and every file of the CI workflows and actions. */
    private static List<Path> deploymentFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> root = Files.list(REPO_ROOT)) {
            root.filter(Files::isRegularFile)
                    .filter(file -> {
                        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.startsWith("dockerfile") || name.contains("compose") || name.startsWith(".env");
                    })
                    .forEach(files::add);
        }
        try (Stream<Path> github = Files.walk(REPO_ROOT.resolve(".github"))) {
            github.filter(Files::isRegularFile).forEach(files::add);
        }
        return files;
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
