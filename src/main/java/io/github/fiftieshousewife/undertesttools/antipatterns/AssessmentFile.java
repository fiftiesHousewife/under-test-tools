package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Writes the anti_patterns section of testing/assessment.yaml from testing/tests.yaml, and proposes a decision for each
// finding. The file is edited as text: other sections, and the first line, stay as they are.
final class AssessmentFile {

    static final String DEFAULT_SCHEMA =
        "https://raw.githubusercontent.com/fiftiesHousewife/under-test/main/schema/assessment.schema.json";
    private static final List<String> FOLLOWING_SECTIONS = List.of("obstacles", "techniques", "decisions");

    private AssessmentFile() {
    }

    /** One finding: a kind (or other-... id), how many tests show it, and three examples as path:line. */
    record Summary(String id, int tests, List<String> examples) {
        String kind() {
            return id.startsWith("other-") ? "other" : id;
        }
    }

    // Works the findings out from the entries of testing/tests.yaml.
    static List<Summary> summarise(final Map<String, Object> testsYaml) {
        final Map<String, List<String>> atsByKind = new TreeMap<>();
        entries(testsYaml).forEach(entry -> kinds(entry).forEach(kind ->
            atsByKind.computeIfAbsent(kind, x -> new ArrayList<>()).add(String.valueOf(entry.get("at")))));
        return atsByKind.entrySet().stream().map(e -> new Summary(e.getKey(), e.getValue().size(),
            e.getValue().stream().sorted(BY_PATH_THEN_LINE).limit(3).toList())).toList();
    }

    private static List<Map<?, ?>> entries(final Map<String, Object> testsYaml) {
        return testsYaml.get("tests") instanceof List<?> list
            ? list.stream().filter(Map.class::isInstance).<Map<?, ?>>map(Map.class::cast).toList() : List.of();
    }

    private static List<String> kinds(final Map<?, ?> entry) {
        return entry.get("anti_patterns") instanceof Map<?, ?> found && found.get("kinds") instanceof List<?> kinds
            ? kinds.stream().map(String::valueOf).toList() : List.of();
    }

    static final Comparator<String> BY_PATH_THEN_LINE = Comparator
        .comparing((String at) -> at.substring(0, at.lastIndexOf(':')))
        .thenComparingInt(at -> Integer.parseInt(at.substring(at.lastIndexOf(':') + 1)));

    static void update(final Path file, final String section, final String repoName) {
        String text = Files.exists(file) ? YamlFile.read(file)
            : "# yaml-language-server: $schema=" + DEFAULT_SCHEMA + "\nschema: assessment/1\nrepo: {name: "
                + YamlText.scalar(repoName) + "}\n";
        text = replaceSection(text, "anti_patterns", section);
        write(file, text);
    }

    static String section(final Map<String, Object> foundBy, final List<String> modules, final List<Summary> found) {
        final Map<String, Object> header = new LinkedHashMap<>();
        header.put("tool", TestsFile.OWNER);
        header.putAll(foundBy);
        final StringBuilder out = new StringBuilder("anti_patterns:\n  found_by: ").append(YamlText.inline(header))
            .append('\n');
        if (!modules.isEmpty()) {
            out.append("  modules: ").append(YamlText.inline(modules)).append('\n');
        }
        out.append("  findings:").append(found.isEmpty() ? " []\n" : "\n");
        for (final Summary s : found) {
            out.append("    - id: ").append(s.id()).append('\n')
                .append("      kind: ").append(s.kind()).append('\n')
                .append("      tests: ").append(s.tests()).append('\n')
                .append("      examples: ").append(YamlText.inline(s.examples())).append('\n');
        }
        return out.toString();
    }

    // Replaces a top-level section, or inserts it before the first of the sections that follow it, else at the end.
    static String replaceSection(final String text, final String name, final String section) {
        final List<String> lines = new ArrayList<>(text.lines().toList());
        final int start = topLevel(lines, name);
        if (start >= 0) {
            int end = start + 1;
            while (end < lines.size() && !isTopLevelKey(lines.get(end))) {
                end++;
            }
            lines.subList(start, end).clear();
            lines.addAll(start, section.lines().toList());
            return String.join("\n", lines) + "\n";
        }
        final int before = FOLLOWING_SECTIONS.stream().mapToInt(s -> topLevel(lines, s)).filter(i -> i >= 0)
            .min().orElse(lines.size());
        lines.addAll(before, section.lines().toList());
        return String.join("\n", lines) + "\n";
    }

    static int topLevel(final List<String> lines, final String name) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(name + ":")) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isTopLevelKey(final String line) {
        return !line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith("#")
            && !line.startsWith("-");
    }

    static void write(final Path file, final String text) {
        try {
            final Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + file, e);
        }
    }
}
