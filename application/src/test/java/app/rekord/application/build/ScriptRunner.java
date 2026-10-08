package app.rekord.application.build;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Red stub (TASK-31.7): the signatures the tests are written against; nothing works yet. */
final class ScriptRunner {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    record Result(int exit, String stdout, String stderr) {}

    record BashLookup(Optional<Path> found, List<String> looked) {

        static BashLookup search(boolean windows, String path, List<Path> wslDirs, List<Path> knownInstalls) {
            throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
        }

        static BashLookup ofThisMachine() {
            throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
        }

        Path required() {
            throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
        }

        String notFoundMessage() {
            throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
        }
    }

    static ScriptRunner in(Path workingDirectory) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    static void prepare(Map<String, String> environment, Map<String, String> changes) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    ScriptRunner with(String name, String value) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    ScriptRunner with(Map<String, String> variables) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    ScriptRunner without(String name) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    ScriptRunner timeout(Duration timeout) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    ScriptRunner using(BashLookup bash) {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }

    Result run(String script, String... args) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("TASK-31.7: not implemented yet");
    }
}
