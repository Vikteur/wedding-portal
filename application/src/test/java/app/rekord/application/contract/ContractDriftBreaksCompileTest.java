package app.rekord.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContractDriftBreaksCompileTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("wedding.repoRoot"));
    private static final Path RESOURCE =
            REPO_ROOT.resolve("rekord-adapter/src/main/java/app/rekord/adapter/web/health/HealthResource.java");

    @TempDir
    Path temp;

    private List<Diagnostic<? extends JavaFileObject>> compile(String name, List<Path> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path out = Files.createDirectories(temp.resolve("out-" + name));
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<String> options = List.of(
                    "-proc:none",
                    "-Xprefer:source",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", out.toString());
            compiler.getTask(new StringWriter(), files, diagnostics, options, null,
                            files.getJavaFileObjectsFromPaths(sources))
                    .call();
        }
        return diagnostics.getDiagnostics();
    }

    private Path stub(String name, String relative, String source) throws IOException {
        Path file = temp.resolve("stub-" + name).resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, source);
    }

    private static List<Diagnostic<? extends JavaFileObject>> errors(List<Diagnostic<? extends JavaFileObject>> all) {
        return all.stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
    }

    @Test
    void the_resource_alone_compiles_against_the_generated_contract() throws IOException {
        assertThat(RESOURCE).isRegularFile();
        assertThat(errors(compile("control", List.of(RESOURCE)))).isEmpty();
    }

    @Test
    void a_renamed_operation_breaks_the_resource_compile() throws IOException {
        assertThat(RESOURCE).isRegularFile();
        Path api = stub("op", "app/rekord/api/HealthApi.java", """
                package app.rekord.api;

                import app.rekord.api.model.Health;
                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.Path;
                import jakarta.ws.rs.Produces;

                @Path("/health")
                public interface HealthApi {
                    @GET
                    @Produces({"application/json"})
                    Health liveness();
                }
                """);
        List<Path> sources = new ArrayList<>(List.of(RESOURCE, api));

        List<Diagnostic<? extends JavaFileObject>> errors = errors(compile("op", sources));

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(d -> d.getSource() != null && d.getSource().getName().contains("HealthResource"));
    }

    @Test
    void a_renamed_field_breaks_the_resource_compile() throws IOException {
        assertThat(RESOURCE).isRegularFile();
        Path model = stub("field", "app/rekord/api/model/Health.java", """
                package app.rekord.api.model;

                public class Health {
                    private Boolean alive;

                    public Health alive(Boolean alive) {
                        this.alive = alive;
                        return this;
                    }

                    public Boolean getAlive() {
                        return alive;
                    }

                    public void setAlive(Boolean alive) {
                        this.alive = alive;
                    }
                }
                """);

        List<Diagnostic<? extends JavaFileObject>> errors = errors(compile("field", List.of(RESOURCE, model)));

        assertThat(errors).isNotEmpty();
    }
}
