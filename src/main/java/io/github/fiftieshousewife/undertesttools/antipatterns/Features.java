package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Cucumber scenarios as tests: each scenario, and each row of a scenario outline's examples (not its heading row). A
 * scenario is disabled when it or its feature has the tag {@code @ignore} or {@code @disabled}, in any case.
 */
final class Features {

    /** A scenario or examples row: its name as the list of tests keys it, where it is, and whether it's disabled. */
    record Scenario(String test, String at, boolean disabled) {
    }

    private static final Pattern DISABLED = Pattern.compile("@(ignore|disabled)\\b");
    private static final Pattern SCENARIO = Pattern.compile("^\\s*(Scenario|Example):\\s*(.*)$");
    private static final Pattern OUTLINE = Pattern.compile("^\\s*Scenario (Outline|Template):\\s*(.*)$");

    private Features() {
    }

    /** Every scenario in the {@code .feature} files under the folders, given from the repo root. */
    static List<Scenario> read(final Path repo, final List<String> folders) {
        final List<Scenario> scenarios = new ArrayList<>();
        for (final String folder : folders) {
            for (final Path file : featureFiles(repo.resolve(folder))) {
                scenarios.addAll(scenarios(repo.relativize(file).toString().replace('\\', '/'), lines(file)));
            }
        }
        return scenarios;
    }

    static List<Scenario> scenarios(final String path, final List<String> lines) {
        final List<Scenario> found = new ArrayList<>();
        final State state = new State();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).strip();
            final String lower = line.toLowerCase(Locale.ROOT);
            if (line.startsWith("@")) {
                state.tagged = state.tagged || DISABLED.matcher(lower).find();
            } else if (line.startsWith("Feature:")) {
                state.featureDisabled = state.tagged;
                state.tagged = false;
            } else {
                read(new Line(path, line, i + 1), state, found);
            }
        }
        return found;
    }

    /** One line of a feature file, with its file and line number. */
    private record Line(String path, String text, int number) {
    }

    /** What the lines read so far say about the current feature and scenario. */
    private static final class State {
        boolean tagged;
        boolean featureDisabled;
        String outline;
        boolean outlineDisabled;
        boolean inExamples;
        boolean headingSeen;
    }

    private static void read(final Line line, final State state, final List<Scenario> found) {
        final var scenario = SCENARIO.matcher(line.text());
        final var outline = OUTLINE.matcher(line.text());
        if (scenario.matches()) {
            found.add(new Scenario(line.path() + ": " + scenario.group(2).strip(), line.path() + ":" + line.number(),
                state.featureDisabled || state.tagged));
            state.outline = null;
            state.tagged = false;
        } else if (outline.matches()) {
            state.outline = outline.group(2).strip();
            state.outlineDisabled = state.featureDisabled || state.tagged;
            state.inExamples = false;
            state.tagged = false;
        } else if (state.outline != null && line.text().startsWith("Examples:")) {
            state.inExamples = true;
            state.headingSeen = false;
        } else if (state.inExamples && line.text().startsWith("|")) {
            if (state.headingSeen) {
                found.add(new Scenario(line.path() + ": " + state.outline + " (row " + line.number() + ")",
                    line.path() + ":" + line.number(), state.outlineDisabled));
            }
            state.headingSeen = true;
        }
    }

    private static List<Path> featureFiles(final Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(p -> p.toString().endsWith(".feature")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + folder, e);
        }
    }

    private static List<String> lines(final Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }
}
