package io.github.fiftieshousewife.undertesttools.existingtests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Updates testing/tests.yaml, the repo's list of tests, changing only what find-meaningless-and-unreliable-tests owns:
// its key under found_by, and each entry's meaningless and unreliable. The other-... kinds the skill decides by
// reading, and every other skill's fields, stay as they are.
final class TestsFile {

    static final String OWNER = "find-meaningless-and-unreliable-tests";
    static final String DEFAULT_SCHEMA =
        "https://raw.githubusercontent.com/fiftiesHousewife/under-test/main/schema/tests.schema.json";

    private TestsFile() {
    }

    /** What to write, and where: the found tests, the run's version and commit, and the modules covered. */
    record Update(Map<String, Finding> findings, Map<String, Object> foundBy, List<String> modules) {
    }

    static void update(final Path file, final Update update) {
        final String header = Files.exists(file) ? header(YamlFile.read(file))
            : "# yaml-language-server: $schema=" + DEFAULT_SCHEMA;
        final Map<String, Object> root =
            Files.exists(file) ? YamlFile.parse(YamlFile.read(file)) : new LinkedHashMap<>();
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
        YamlFile.write(file, header, rootWith(root, update, new ArrayList<>(entries.values())));
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

    // The test's entry, at its line now, with the kinds found added to each list's: meaningless, then unreliable.
    private static void apply(final Map<String, Map<String, Object>> entries, final String test, final Finding found) {
        final Map<String, Object> entry = entries.computeIfAbsent(test, t -> new LinkedHashMap<>(Map.of("test", t)));
        final Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("test", test);
        ordered.put("at", found.getAt());
        entry.forEach(ordered::putIfAbsent);
        for (final TestList list : TestList.values()) {
            final Set<String> kinds = new LinkedHashSet<>(strings(ordered.get(list.field())));
            kinds.addAll(found.on(list));
            put(ordered, list.field(), list.ordered(kinds));
        }
        entries.put(test, ordered);
    }

    // Removes the kinds this tool decides, keeping the skill's other-... kinds; a list left empty goes.
    private static void clearOwnedKinds(final Map<String, Object> entry) {
        entry.remove(TestList.UNRELIABLE.field());
        final String meaningless = TestList.MEANINGLESS.field();
        put(entry, meaningless, strings(entry.get(meaningless)).stream().filter(TestsFile::skillsOwn).toList());
    }

    private static void put(final Map<String, Object> entry, final String field, final List<String> kinds) {
        if (kinds.isEmpty()) {
            entry.remove(field);
        } else {
            entry.put(field, kinds);
        }
    }

    static boolean skillsOwn(final String kind) {
        return kind.startsWith("other-");
    }

    private static boolean covered(final Map<String, Object> entry, final List<String> modules) {
        final String at = String.valueOf(entry.get("at"));
        return modules.isEmpty() || modules.stream().anyMatch(m -> at.startsWith(m + "/"));
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
}
