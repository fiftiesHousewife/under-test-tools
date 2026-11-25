package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.maps;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.strings;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// testing/external-calls.json, under-test-external-calls' facts: the tests that may reach a connection, the systems
// their reach points make, and the tests that start a framework.
final class Facts {

    private static final String LITERAL = "literal";

    private final Map<String, Object> root;
    private final Map<String, Map<String, Object>> tests;
    private final Map<String, Map<String, Object>> systems;
    private final Set<String> startingAFramework;

    Facts(final Map<String, Object> root) {
        this.root = root;
        this.tests = byKey(maps(root.get("tests")), "test");
        this.systems = byKey(maps(root.get("systems")), "name");
        this.startingAFramework = maps(root.get("tests_starting_a_framework")).stream()
            .map(t -> text(t, "test")).collect(Collectors.toSet());
    }

    Map<String, Object> madeFrom() {
        final Map<String, Object> made = new LinkedHashMap<>();
        made.put("schema", text(root, "schema"));
        made.put("tool_version", text(root, "tool_version"));
        return made;
    }

    Optional<Map<String, Object>> test(final String name) {
        return Optional.ofNullable(tests.get(name));
    }

    boolean startsAFramework(final String test) {
        return startingAFramework.contains(test)
            || test(test).map(t -> t.containsKey("starts_framework")).orElse(false);
    }

    // The test's system for a node: the one whose points meet the node's opened-by, else one of the entry's kind.
    Optional<Map<String, Object>> system(final Map<String, Object> test, final Map<String, Object> node,
                                         final String kind) {
        final List<Map<String, Object>> candidates = strings(test.get("systems")).stream()
            .map(systems::get).filter(s -> s != null).toList();
        final Set<String> openedBy = Set.copyOf(strings(map(map(node.get("metadata")).get("under-test"))
            .get("opened-by")));
        final Predicate<Map<String, Object>> meets =
            s -> strings(s.get("points")).stream().anyMatch(openedBy::contains);
        final Predicate<Map<String, Object>> ofKind = s -> strings(s.get("kinds")).contains(kind);
        return candidates.stream().filter(meets).findFirst().or(() -> candidates.stream().filter(ofKind).findFirst());
    }

    // Whether the system's address is a literal written in the main sources (external-calls/2's written_at); never when
    // the facts don't say where it's written.
    boolean addressInCode(final Map<String, Object> system) {
        final Map<String, Object> address = map(system.get("address"));
        final String file = text(map(address.get("written_at")), "file").replace('\\', '/');
        return LITERAL.equals(text(address, "source")) && ("/" + file).contains("/src/main/");
    }

    private static Map<String, Map<String, Object>> byKey(final List<Map<String, Object>> items, final String key) {
        return items.stream().collect(Collectors.toMap(i -> text(i, key), i -> i, (a, b) -> a, LinkedHashMap::new));
    }
}
