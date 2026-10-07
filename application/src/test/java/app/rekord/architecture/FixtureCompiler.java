package app.rekord.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Compiles source text blocks at test time (release 25, test classpath) and imports the class files, so a rule is
 * checked against fixtures that never live under a reserved layer package in the repository.
 */
public final class FixtureCompiler {

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE =
            Pattern.compile("(?m)^\\s*(?:public\\s+)?(?:final\\s+|abstract\\s+|sealed\\s+)*(?:class|interface|record|enum|@interface)\\s+(\\w+)");

    private FixtureCompiler() {}

    /** Each source is one compilation unit whose first top-level type names the file. Throws on a compile error. */
    public static JavaClasses compile(String... sources) {
        try {
            Path root = Files.createTempDirectory("arch-fixtures");
            Path src = Files.createDirectories(root.resolve("src"));
            Path out = Files.createDirectories(root.resolve("out"));
            List<Path> files = new ArrayList<>();
            for (String source : sources) {
                Matcher pkg = PACKAGE.matcher(source);
                Matcher type = TYPE.matcher(source);
                if (!type.find()) {
                    throw new IllegalArgumentException("no top-level type in fixture:\n" + source);
                }
                Path dir = pkg.find() ? src.resolve(pkg.group(1).replace('.', '/')) : src;
                Files.createDirectories(dir);
                files.add(Files.writeString(dir.resolve(type.group(1) + ".java"), source));
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            boolean ok;
            try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
                ok = compiler.getTask(
                                new StringWriter(),
                                fm,
                                diagnostics,
                                List.of(
                                        "--release", "25",
                                        "-proc:none",
                                        "-classpath", System.getProperty("java.class.path"),
                                        "-d", out.toString()),
                                null,
                                fm.getJavaFileObjectsFromPaths(files))
                        .call();
            }
            if (!ok) {
                StringBuilder message = new StringBuilder("fixture does not compile:");
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    message.append('\n').append(d);
                }
                throw new IllegalStateException(message.toString());
            }
            return new ClassFileImporter().importPath(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
