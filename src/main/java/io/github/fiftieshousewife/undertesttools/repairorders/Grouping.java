package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.flag;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.maps;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Sorts testing/tests.yaml's entries into the tests each node's order holds, by node then test, and those left out.
// A test is ordered once per kind: under its code-path entry, with the hosts its own code or settings name.
final class Grouping {

    private static final String REACHES = "reaches";
    private static final String MAY_REACH = "may-reach";
    private static final Set<String> NAMING = Set.of("test-code", "test-settings");

    private final Map<String, Map<String, Candidate>> byNode = new TreeMap<>();
    private final Set<Left> leftOut = new TreeSet<>(Left.ORDER);

    private Grouping() {
    }

    static Grouping of(final Map<String, Object> tests, final SystemsList systems) {
        final Grouping grouping = new Grouping();
        maps(tests.get("tests")).forEach(test -> grouping.add(test, systems));
        return grouping;
    }

    Map<String, Map<String, Candidate>> ordered() {
        return byNode;
    }

    Set<Left> left() {
        return leftOut;
    }

    private void add(final Map<String, Object> test, final SystemsList systems) {
        final boolean disabled = Fields.strings(test.get("meaningless")).contains("disabled")
            || maps(test.get("meaningless")).stream().anyMatch(m -> "disabled".equals(text(m, "kind")));
        final List<Candidate> kept = maps(test.get("external_systems")).stream()
            .map(entry -> new Candidate(test, entry)).filter(c -> toOrder(c, systems, disabled)).toList();
        fold(kept).forEach(c ->
            byNode.computeIfAbsent(c.node(), n -> new TreeMap<>()).putIfAbsent(c.name(), c));
        if (flag(map(test.get("measurement")), "missed_by_discovery")) {
            leftOut.add(new Left(text(test, "test"), "", "missed-by-discovery"));
        }
    }

    // Records why an entry that reaches or may reach isn't ordered; true when it is to be ordered.
    private boolean toOrder(final Candidate candidate, final SystemsList systems, final boolean disabled) {
        final String grade = candidate.grade();
        if (!REACHES.equals(grade) && !MAY_REACH.equals(grade)) {
            return false;
        }
        final String why = why(candidate, systems, disabled);
        if (!why.isEmpty()) {
            leftOut.add(new Left(candidate.name(), candidate.node(), why));
        }
        return why.isEmpty();
    }

    // Why an entry that reaches or may reach is left out, by the first that holds; empty when it is ordered.
    private static String why(final Candidate candidate, final SystemsList systems, final boolean disabled) {
        if (systems.node(candidate.node()).isEmpty()) {
            return "no-node";
        }
        if (disabled) {
            return "disabled";
        }
        if (MAY_REACH.equals(candidate.grade())) {
            return MAY_REACH;
        }
        return "paired".equals(candidate.repairState()) ? "paired" : "";
    }

    private static Stream<Candidate> fold(final List<Candidate> candidates) {
        return candidates.stream()
            .collect(Collectors.groupingBy(Candidate::kind, LinkedHashMap::new, Collectors.toList()))
            .values().stream().flatMap(Grouping::foldKind);
    }

    // Of one kind: the code-path entries, carrying the test-code and test-settings entries' nodes, and any others.
    private static Stream<Candidate> foldKind(final List<Candidate> ofKind) {
        final List<Candidate> codePath = ofKind.stream().filter(c -> "code-path".equals(c.foundBy())).toList();
        final Set<String> codePathNodes = codePath.stream().map(Candidate::node).collect(Collectors.toSet());
        final List<String> named = ofKind.stream().filter(c -> NAMING.contains(c.foundBy())).map(Candidate::node)
            .filter(n -> !codePathNodes.contains(n)).distinct().sorted().toList();
        if (codePath.isEmpty() || named.isEmpty()) {
            return ofKind.stream();
        }
        return Stream.concat(codePath.stream().map(c -> c.withNamedHosts(named)),
            ofKind.stream().filter(c -> !"code-path".equals(c.foundBy()) && !NAMING.contains(c.foundBy())));
    }
}
