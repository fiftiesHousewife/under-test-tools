package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Updates {@code testing/tests.yaml}, the repo's list of tests, changing only what find-test-anti-patterns owns: its
 * key under {@code found_by}, and in each entry the kinds this tool decides. Kinds the skill decides by reading
 * ({@code reaches-external-system} with its {@code reaches}, and the {@code other-...} kinds) and every other skill's
 * fields stay as they are.
 */
final class TestsFile {

    static final String OWNER = "find-test-anti-patterns";
    static final String DEFAULT_SCHEMA =
        "https://raw.githubusercontent.com/fiftiesHousewife/under-test/main/schema/tests.schema.json";
    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    private TestsFile() {
    }

    /** What to write, and where: the found tests, the run's version and commit, and the modules covered. */
    record Update(Map<String, Finding> findings, Map<String, Object> foundBy, List<String> modules) {
    }

    static void update(final Path file, final Update update) {
        final String header = Files.exists(file) ? header(read(file))
            : "# yaml-language-server: $schema=" + DEFAULT_SCHEMA;
        final Map<String, Object> root = Files.exists(file) ? parse(read(file)) : new LinkedHashMap<>();
        final Map<String, Map<String, Object>> entries = new TreeMap<>();
        for (final Object item : list(root.get("tests"))) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> entry = new LinkedHashMap<>((Map<String, Object>) item);
            if (covered(entry, update.modules())) {
                clearOwnedKinds(entry);
            }
            entries.put(String.valueOf(entry.get("test")), entry);
        }
        update.findings().forEach((test, finding) -> apply(entries, test, finding));
        entries.values().removeIf(e -> e.keySet().equals(Set.of("test", "at")));
        write(file, header, rootWith(root, update, new ArrayList<>(entries.values())));
    }

    private static Map<String, Object> rootWith(final Map<String, Object> old, final Update update,
                                                final List<Map<String, Object>> tests) {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", "tests/1");
        @SuppressWarnings("unchecked")
        final Map<String, Object> foundBy = new TreeMap<>(old.get("found_by") instanceof Map<?, ?> m
            ? (Map<String, Object>) m : Map.of());
        foundBy.put(OWNER, update.foundBy());
        root.put("found_by", foundBy);
        root.put("tests", tests);
        return root;
    }

    private static void apply(final Map<String, Map<String, Object>> entries, final String test, final Finding found) {
        final Map<String, Object> entry = entries.computeIfAbsent(test, t -> new LinkedHashMap<>(Map.of("test", t)));
        final Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("test", test);
        ordered.put("at", found.getAt());
        entry.forEach(ordered::putIfAbsent);
        final Map<String, Object> antiPatterns = antiPatterns(ordered);
        final List<String> kinds = new ArrayList<>(strings(antiPatterns.get("kinds")));
        found.getKinds().stream().filter(k -> !kinds.contains(k)).forEach(kinds::add);
        kinds.sort(String::compareTo);
        antiPatterns.put("kinds", kinds);
        if (found.getCopyOf() != null) {
            antiPatterns.put("copy_of", found.getCopyOf());
        }
        ordered.put("anti_patterns", order(antiPatterns));
        entries.put(test, ordered);
    }

    /** Removes the kinds this tool decides, keeping the skill's; an anti_patterns left with no kinds goes. */
    private static void clearOwnedKinds(final Map<String, Object> entry) {
        if (!(entry.get("anti_patterns") instanceof Map<?, ?>)) {
            return;
        }
        final Map<String, Object> antiPatterns = antiPatterns(entry);
        final List<String> kept = strings(antiPatterns.get("kinds")).stream().filter(TestsFile::skillsOwn).toList();
        antiPatterns.remove("copy_of");
        if (!kept.contains("reaches-external-system")) {
            antiPatterns.remove("reaches");
        }
        if (kept.isEmpty()) {
            entry.remove("anti_patterns");
        } else {
            antiPatterns.put("kinds", kept);
            entry.put("anti_patterns", order(antiPatterns));
        }
    }

    static boolean skillsOwn(final String kind) {
        return "reaches-external-system".equals(kind) || kind.startsWith("other-");
    }

    private static boolean covered(final Map<String, Object> entry, final List<String> modules) {
        final String at = String.valueOf(entry.get("at"));
        return modules.isEmpty() || modules.stream().anyMatch(m -> at.startsWith(m + "/"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> antiPatterns(final Map<String, Object> entry) {
        final Object existing = entry.get("anti_patterns");
        final Map<String, Object> copy = existing instanceof Map<?, ?> m
            ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        entry.put("anti_patterns", copy);
        return copy;
    }

    private static Map<String, Object> order(final Map<String, Object> antiPatterns) {
        final Map<String, Object> ordered = new LinkedHashMap<>();
        for (final String key : List.of("kinds", "reaches", "copy_of")) {
            if (antiPatterns.containsKey(key)) {
                ordered.put(key, antiPatterns.get(key));
            }
        }
        antiPatterns.forEach(ordered::putIfAbsent);
        return ordered;
    }

    private static List<String> strings(final Object value) {
        return list(value).stream().map(String::valueOf).toList();
    }

    private static List<?> list(final Object value) {
        return value instanceof List<?> l ? l : List.of();
    }

    static String header(final String text) {
        final StringBuilder header = new StringBuilder();
        for (final String line : text.lines().toList()) {
            if (!line.startsWith("#")) {
                break;
            }
            header.append(header.length() == 0 ? "" : "\n").append(line);
        }
        return header.length() == 0 ? "# yaml-language-server: $schema=" + DEFAULT_SCHEMA : header.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(final String text) {
        final Object root = YAML.readValue(text, Object.class);
        return root instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
    }

    static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }

    private static void write(final Path file, final String header, final Map<String, Object> root) {
        final StringBuilder out = new StringBuilder(header).append('\n');
        root.entrySet().forEach(e -> YamlText.write(out, 0, e));
        try {
            final Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + file, e);
        }
    }
}
