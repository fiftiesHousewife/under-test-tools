package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A module's folders of test code, and which of them the build runs. With Gradle, {@code test} runs, and so does any
 * other source set the build files name. With Maven, {@code src/test/java} runs, and any folder the pom names as a test
 * source directory; only classes matching the test runner's patterns run.
 */
public final class TestFolders {

    /** A folder of test code from the repo root, and whether the build runs the tests in it. */
    public record Folder(String path, boolean runs) {
    }

    private static final List<String> SUREFIRE_DEFAULTS = List.of("Test*", "*Test", "*Tests", "*TestCase");
    private static final List<String> FAILSAFE_DEFAULTS = List.of("IT*", "*IT", "*ITCase");
    private static final Pattern INCLUDE =
        Pattern.compile("<include>\\s*(?:\\*\\*/)?([^<]+?)(?:\\.java)?\\s*</include>");

    private final List<Folder> folders;
    private final boolean maven;
    private final List<String> runnerPatterns;

    private TestFolders(final List<Folder> folders, final boolean maven, final List<String> runnerPatterns) {
        this.folders = List.copyOf(folders);
        this.maven = maven;
        this.runnerPatterns = List.copyOf(runnerPatterns);
    }

    /** Finds a module's test folders; the module is a folder from the repo root, or empty for the root. */
    public static TestFolders of(final Path repo, final String module) {
        final Path moduleDir = module.isEmpty() ? repo : repo.resolve(module);
        final String builds = buildFiles(repo, moduleDir);
        final boolean maven = isMaven(moduleDir);
        final List<Folder> found = sourceFolders(moduleDir).stream().map(source -> {
            final String name = source.getName(source.getNameCount() - 2).toString();
            return new Folder(repo.relativize(source).toString().replace('\\', '/'),
                "test".equals(name) || named(builds, name, maven));
        }).toList();
        return new TestFolders(found, maven, maven ? patternsIn(builds) : List.of());
    }

    private static boolean isMaven(final Path moduleDir) {
        return !Files.exists(moduleDir.resolve("build.gradle")) && !Files.exists(moduleDir.resolve("build.gradle.kts"))
            && Files.exists(moduleDir.resolve("pom.xml"));
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public List<String> paths() {
        return folders.stream().map(Folder::path).toList();
    }

    /** Whether the test runner picks up a class by its simple name: always with Gradle. */
    public boolean runnerPicksUp(final String simpleName) {
        return !maven || runnerPatterns.stream().anyMatch(p -> matches(p, simpleName));
    }

    static boolean matches(final String pattern, final String name) {
        final String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        return name.matches(regex);
    }

    private static boolean named(final String builds, final String name, final boolean maven) {
        if (maven) {
            return builds.contains("src/" + name + "/java");
        }
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(builds).find();
    }

    private static List<String> patternsIn(final String pom) {
        final List<String> patterns = new ArrayList<>();
        final Matcher include = INCLUDE.matcher(pom);
        while (include.find()) {
            patterns.add(include.group(1).trim());
        }
        if (patterns.isEmpty()) {
            patterns.addAll(SUREFIRE_DEFAULTS);
        }
        if (pom.contains("maven-failsafe-plugin")) {
            patterns.addAll(FAILSAFE_DEFAULTS);
        }
        return patterns;
    }

    private static List<Path> sourceFolders(final Path moduleDir) {
        final Path src = moduleDir.resolve("src");
        if (!Files.isDirectory(src)) {
            return List.of();
        }
        try (Stream<Path> sets = Files.list(src)) {
            return sets.filter(Files::isDirectory)
                .filter(s -> !"main".equals(String.valueOf(s.getFileName())))
                .map(s -> s.resolve("java"))
                .filter(Files::isDirectory)
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + src, e);
        }
    }

    private static String buildFiles(final Path repo, final Path moduleDir) {
        final StringBuilder text = new StringBuilder();
        for (final Path dir : List.of(moduleDir, repo)) {
            for (final String name : List.of("build.gradle", "build.gradle.kts", "pom.xml")) {
                final Path file = dir.resolve(name);
                if (Files.isRegularFile(file)) {
                    text.append(read(file)).append('\n');
                }
            }
        }
        return text.toString();
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }
}
