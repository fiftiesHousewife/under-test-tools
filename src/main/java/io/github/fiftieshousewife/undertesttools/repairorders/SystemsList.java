package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.maps;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.optional;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.strings;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// testing/external-systems.calm.json: the nodes, and the relationship from the application to each, which holds the
// stand-in chosen for it.
final class SystemsList {

    private static final String UNDER_TEST = "under-test";

    private final Map<String, Map<String, Object>> nodes;
    private final Map<String, Map<String, Object>> relationships;
    private final String listPath;

    // The list's content, and its path from the repo's folder.
    SystemsList(final Map<String, Object> root, final String path) {
        this.listPath = path;
        final String application = text(map(map(root.get("metadata")).get(UNDER_TEST)), "node");
        this.nodes = maps(root.get("nodes")).stream()
            .collect(Collectors.toMap(n -> text(n, "unique-id"), n -> n, (a, b) -> a, LinkedHashMap::new));
        this.relationships = maps(root.get("relationships")).stream()
            .filter(r -> application.isEmpty() || application.equals(end(r, "source")))
            .collect(Collectors.toMap(r -> end(r, "destination"), r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    String path() {
        return listPath;
    }

    Optional<Map<String, Object>> node(final String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    // The relationship's stand-in and state; a node the application has no relationship to fails the run.
    Map<String, Object> standIn(final String node) {
        final Map<String, Object> relationship = Optional.ofNullable(relationships.get(node))
            .orElseThrow(() -> new IllegalStateException("The external systems list has no relationship from the "
                + "application to node " + node));
        final Map<String, Object> underTest = map(map(relationship.get("metadata")).get(UNDER_TEST));
        final Map<String, Object> chosen = map(underTest.get("stand-in"));
        final Map<String, Object> standIn = new LinkedHashMap<>();
        standIn.put("approach", text(chosen, "approach"));
        optional(chosen, "kit").ifPresent(kit -> standIn.put("kit", kit));
        standIn.put("state", text(underTest, "state"));
        return standIn;
    }

    static String addressFrom(final Map<String, Object> node) {
        final Map<String, Object> underTest = map(map(node.get("metadata")).get(UNDER_TEST));
        final List<String> addresses = strings(underTest.get("addresses"));
        return addresses.isEmpty() ? text(underTest, "address-from") : String.join(", ", addresses);
    }

    private static String end(final Map<String, Object> relationship, final String which) {
        return text(map(map(map(relationship.get("relationship-type")).get("connects")).get(which)), "node");
    }
}
