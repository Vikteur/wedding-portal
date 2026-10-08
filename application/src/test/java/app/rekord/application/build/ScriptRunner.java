package app.rekord.application.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.opentest4j.TestAbortedException;

/**
 * Runs a bash script of the repository for a build test, in a scrubbed environment, and gives back its exit code,
 * standard output and standard error. The one runner of {@code ImageTagsTest}, {@code ContractPinTest} and every
 * later test of a script under {@code .github/scripts}.
 *
 * <p>A script that does not finish within the timeout is killed with the processes it started, and the test fails
 * naming the script and the timeout, so a hung script fails one test and not the whole build. The environment is the
 * environment of the test JVM without its {@code GITHUB_*} variables (a CI run has them, a developer machine does not,
 * and a script must behave the same on both), plus the variables the caller sets and minus the ones the caller
 * removes.
 *
 * <p>A runner never changes: {@code with}, {@code without}, {@code timeout} and {@code using} return a new one, so a
 * test class can keep a base runner in a constant.
 */
final class ScriptRunner {

    /** What a script that hangs costs the build before the test fails. */
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private static final String SCRUBBED_PREFIX = "GITHUB_";
    private static final int OUTPUT_IN_FAILURE = 2_000;

    /** What a script did: its exit code and everything it wrote to standard output and standard error. */
    record Result(int exit, String stdout, String stderr) {}

    /**
     * Where bash is, or where it was looked for. On Linux and macOS bash is the first {@code bash} on the PATH, which is
     * what CI uses. On Windows it is the Git Bash that sits with {@code git.exe}, else a {@code bash.exe} on the PATH,
     * else a known install folder of Git for Windows; never the {@code bash.exe} of the Windows folder, which is the
     * launcher of WSL and does not read a Windows path.
     */
    record BashLookup(Optional<Path> found, List<String> looked) {

        /**
         * @param windows whether to look the Windows way
         * @param path the PATH variable, entries split by the platform's separator
         * @param wslDirs folders whose {@code bash.exe} is the WSL launcher (the Windows folder)
         * @param knownInstalls where Git Bash is installed by default, tried after the PATH
         */
        static BashLookup search(boolean windows, String path, List<Path> wslDirs, List<Path> knownInstalls) {
            List<String> looked = new ArrayList<>();
            List<Path> directories = directoriesOf(path);
            Optional<Path> found = windows
                    ? searchWindows(directories, wslDirs, knownInstalls, looked)
                    : searchUnix(directories, looked);
            return new BashLookup(found, List.copyOf(looked));
        }

        /** The lookup for the machine the tests run on, made once. */
        static BashLookup ofThisMachine() {
            return ThisMachine.LOOKUP;
        }

        /** The bash to run, or the test is skipped, with the places it looked, when there is none. */
        Path required() {
            if (found.isEmpty()) {
                throw new TestAbortedException(notFoundMessage());
            }
            return found.get();
        }

        String notFoundMessage() {
            return looked.isEmpty()
                    ? "no bash found: the PATH names no directory to look in"
                    : "no bash found, looked at: " + String.join(", ", looked);
        }

        private static Optional<Path> searchUnix(List<Path> directories, List<String> looked) {
            for (Path directory : directories) {
                Path bash = directory.resolve("bash");
                looked.add(bash.toString());
                if (Files.isRegularFile(bash) && Files.isExecutable(bash)) {
                    return Optional.of(bash);
                }
            }
            return Optional.empty();
        }

        private static Optional<Path> searchWindows(
                List<Path> directories, List<Path> wslDirs, List<Path> knownInstalls, List<String> looked) {
            Set<Path> candidates = new LinkedHashSet<>();
            for (Path directory : directories) {
                if (Files.isRegularFile(directory.resolve("git.exe"))) {
                    // git.exe is in <root>\cmd, <root>\bin or <root>\mingw64\bin of Git for Windows; bash.exe is in
                    // <root>\bin
                    Path up = directory;
                    for (int level = 0; level < 3 && up != null; level++, up = up.getParent()) {
                        candidates.add(up.resolve("bin").resolve("bash.exe"));
                    }
                }
            }
            directories.forEach(directory -> candidates.add(directory.resolve("bash.exe")));
            candidates.addAll(knownInstalls);
            for (Path candidate : candidates) {
                if (!Files.isRegularFile(candidate)) {
                    looked.add(candidate.toString());
                } else if (isWslLauncher(candidate, wslDirs)) {
                    looked.add(candidate + " (skipped: the bash.exe of the Windows folder is the WSL launcher,"
                            + " which does not read a Windows path)");
                } else {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }

        private static boolean isWslLauncher(Path bash, List<Path> wslDirs) {
            return wslDirs.stream().anyMatch(dir -> bash.normalize().startsWith(dir.normalize()));
        }

        private static List<Path> directoriesOf(String path) {
            List<Path> directories = new ArrayList<>();
            for (String entry : Objects.requireNonNullElse(path, "").split(Pattern.quote(File.pathSeparator))) {
                String directory = entry.strip();
                if (directory.length() >= 2 && directory.startsWith("\"") && directory.endsWith("\"")) {
                    directory = directory.substring(1, directory.length() - 1).strip();
                }
                if (directory.isEmpty()) {
                    continue;
                }
                try {
                    directories.add(Path.of(directory));
                } catch (InvalidPathException notAPath) {
                    // an entry of the PATH that is no path has no bash
                }
            }
            return directories;
        }
    }

    /** Looked up on first use, not on every run. */
    private static final class ThisMachine {

        static final BashLookup LOOKUP = lookup();

        private static BashLookup lookup() {
            boolean windows = System.getProperty("os.name").startsWith("Windows");
            List<Path> wslDirs = new ArrayList<>();
            List<Path> installs = new ArrayList<>();
            if (windows) {
                // getenv(String), not the getenv() map: on Windows only the first is case-insensitive, and
                // PowerShell and cmd name the variable Path
                add(wslDirs, System.getenv("SystemRoot"), "");
                add(installs, System.getenv("ProgramFiles"), "Git\\bin\\bash.exe");
                add(installs, System.getenv("ProgramW6432"), "Git\\bin\\bash.exe");
                add(installs, System.getenv("ProgramFiles(x86)"), "Git\\bin\\bash.exe");
                add(installs, System.getenv("LOCALAPPDATA"), "Programs\\Git\\bin\\bash.exe");
                add(installs, "C:\\Program Files", "Git\\bin\\bash.exe");
            }
            return BashLookup.search(windows, System.getenv("PATH"), wslDirs, List.copyOf(new LinkedHashSet<>(installs)));
        }

        private static void add(List<Path> into, String base, String relative) {
            if (base == null || base.isBlank()) {
                return;
            }
            try {
                into.add(relative.isEmpty() ? Path.of(base) : Path.of(base, relative));
            } catch (InvalidPathException notAPath) {
                // an environment variable that is no path names no folder
            }
        }
    }

    private final Path workingDirectory;
    private final Map<String, String> changes;
    private final Duration timeout;
    private final Optional<BashLookup> bash;

    private ScriptRunner(
            Path workingDirectory, Map<String, String> changes, Duration timeout, Optional<BashLookup> bash) {
        this.workingDirectory = workingDirectory;
        this.changes = changes;
        this.timeout = timeout;
        this.bash = bash;
    }

    /** A runner whose scripts start in {@code workingDirectory}, which a relative script path is relative to. */
    static ScriptRunner in(Path workingDirectory) {
        return new ScriptRunner(workingDirectory, Map.of(), DEFAULT_TIMEOUT, Optional.empty());
    }

    /** The environment of the process: the inherited one without its GITHUB_ variables, then the caller's changes. */
    static void prepare(Map<String, String> environment, Map<String, String> changes) {
        environment.keySet().removeIf(name -> name.regionMatches(true, 0, SCRUBBED_PREFIX, 0, SCRUBBED_PREFIX.length()));
        changes.forEach((name, value) -> {
            if (value == null) {
                environment.remove(name);
            } else {
                environment.put(name, value);
            }
        });
    }

    /** The script sees {@code name} set to {@code value}, a GITHUB_ variable too. */
    ScriptRunner with(String name, String value) {
        return change(name, Objects.requireNonNull(value, name));
    }

    ScriptRunner with(Map<String, String> variables) {
        ScriptRunner runner = this;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            runner = runner.with(variable.getKey(), variable.getValue());
        }
        return runner;
    }

    /** The script does not see {@code name} at all, whether the test JVM has it or an earlier call set it. */
    ScriptRunner without(String name) {
        return change(name, null);
    }

    ScriptRunner timeout(Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("the timeout must be positive, not " + timeout);
        }
        return new ScriptRunner(workingDirectory, changes, timeout, bash);
    }

    /** Looks for bash the way {@code lookup} says instead of on this machine; for the tests of the lookup. */
    ScriptRunner using(BashLookup lookup) {
        return new ScriptRunner(workingDirectory, changes, timeout, Optional.of(lookup));
    }

    /**
     * Runs {@code bash <script> <args>}. The script is skipped, not failed, when the machine has no bash; the test
     * fails when the script does not finish within the timeout.
     */
    Result run(String script, String... args) throws IOException, InterruptedException {
        Path bashExecutable = bash.orElseGet(BashLookup::ofThisMachine).required();
        List<String> command = new ArrayList<>();
        command.add(bashExecutable.toString());
        command.add(script.replace('\\', '/'));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile());
        prepare(builder.environment(), changes);
        // Output goes to files, so a script that writes a lot, or that leaves a process holding its output open,
        // cannot block the read of it
        Path stdout = Files.createTempFile("script-runner-", ".out");
        Path stderr = Files.createTempFile("script-runner-", ".err");
        try {
            Process process = builder.redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
            process.getOutputStream().close();
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    kill(process);
                    throw new AssertionError(timedOut(script, read(stdout), read(stderr)));
                }
            } finally {
                if (process.isAlive()) {
                    kill(process);
                }
            }
            return new Result(process.exitValue(), read(stdout), read(stderr));
        } finally {
            delete(stdout);
            delete(stderr);
        }
    }

    private ScriptRunner change(String name, String value) {
        Map<String, String> next = new LinkedHashMap<>(changes);
        next.put(name, value);
        return new ScriptRunner(workingDirectory, Collections.unmodifiableMap(next), timeout, bash);
    }

    private String timedOut(String script, String stdout, String stderr) {
        String message = script + " did not finish within " + describe(timeout) + " and was killed";
        if (stdout.isBlank() && stderr.isBlank()) {
            return message;
        }
        return message + "\nstdout so far: " + tail(stdout) + "\nstderr so far: " + tail(stderr);
    }

    private static String describe(Duration duration) {
        if (duration.toMillis() % 1000 != 0) {
            return duration.toMillis() + " ms";
        }
        return duration.toSeconds() + (duration.toSeconds() == 1 ? " second" : " seconds");
    }

    private static String tail(String output) {
        String trimmed = output.strip();
        return trimmed.length() <= OUTPUT_IN_FAILURE
                ? trimmed
                : "..." + trimmed.substring(trimmed.length() - OUTPUT_IN_FAILURE);
    }

    /**
     * The shell and everything Java can see it started: the descendants are taken first, as they are lost once their
     * parent is gone. Under Git Bash on Windows an external command that the shell runs (such as {@code sleep}) hangs
     * off a parent that no longer exists, so Java does not see it: the shell is killed and that command ends on its own.
     */
    private static void kill(Process process) throws InterruptedException {
        List<ProcessHandle> descendants = process.descendants().toList();
        process.destroyForcibly();
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.waitFor(10, TimeUnit.SECONDS);
    }

    /** Bytes, not {@code readString}: output that is not UTF-8 must not hide the exit code behind a decoding error. */
    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException stillOpen) {
            file.toFile().deleteOnExit();
        }
    }
}
