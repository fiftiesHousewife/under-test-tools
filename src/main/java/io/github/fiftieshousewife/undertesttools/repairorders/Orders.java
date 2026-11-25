package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.maps;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// testing/repair-orders.json's content: an order per node with tests to pair, and the tests left out, each sorted.
final class Orders {

    static final String SCHEMA = "repair-orders/1";
    private static final String DRAFTER = "draft-the-external-systems-list";

    private Orders() {
    }

    /** The orders file's content. */
    record Made(Map<String, Object> json) {

        String summary() {
            final List<Map<String, Object>> orders = maps(json.get("orders"));
            final int tests = orders.stream().mapToInt(o -> maps(o.get("tests")).size()).sum();
            return orders.size() + " orders holding " + tests + " tests; " + maps(json.get("not_ordered")).size()
                + " left out of any order.";
        }
    }

    static Made make(final Inputs inputs, final String version) {
        final Grouping grouping = Grouping.of(inputs.tests(), inputs.systems());
        final List<Map<String, Object>> orders = grouping.ordered().entrySet().stream()
            .map(e -> order(e.getKey(), e.getValue().values(), inputs)).toList();
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put("schema", SCHEMA);
        json.put("tool_version", version);
        json.put("made_from", madeFrom(inputs));
        json.put("orders", orders);
        json.put("not_ordered", grouping.left().stream().map(Left::json).toList());
        return new Made(json);
    }

    private static Map<String, Object> madeFrom(final Inputs inputs) {
        final Map<String, Object> stamp = map(map(inputs.tests().get("found_by")).get(DRAFTER));
        if (stamp.isEmpty()) {
            throw new IllegalStateException("testing/tests.yaml has no found_by." + DRAFTER
                + ": draft the external systems list first");
        }
        final Map<String, Object> tests = new LinkedHashMap<>();
        tests.put("version", text(stamp, "version"));
        tests.put("commit", text(stamp, "commit"));
        final Map<String, Object> made = new LinkedHashMap<>();
        made.put("external_calls", inputs.facts().madeFrom());
        made.put("tests", tests);
        made.put("external_systems", inputs.systems().path());
        return made;
    }

    private static Map<String, Object> order(final String nodeId, final Collection<Candidate> candidates,
                                             final Inputs inputs) {
        final Map<String, Object> node = inputs.systems().node(nodeId).orElseThrow();
        final Map<String, Object> standIn = inputs.systems().standIn(nodeId);
        final OrderedTest.Context context = new OrderedTest.Context(inputs.facts(), node, text(standIn, "approach"));
        final List<OrderedTest.Built> tests = candidates.stream().map(c -> OrderedTest.build(c, context)).toList();
        final Map<String, Object> order = new LinkedHashMap<>();
        order.put("node", nodeId);
        order.put("kind", kind(candidates));
        order.put("name", text(node, "name"));
        order.put("stand_in", standIn);
        order.put("address_from", SystemsList.addressFrom(node));
        order.put("shared_chain", SharedChain.of(tests.stream().filter(OrderedTest.Built::hasChain)
            .map(OrderedTest.Built::outside).toList()));
        order.put("tests", tests.stream().map(OrderedTest.Built::json).toList());
        return order;
    }

    // The most common kind among the ordered entries, ties going to the first in sort order.
    private static String kind(final Collection<Candidate> candidates) {
        final Map<String, Long> counts = candidates.stream()
            .collect(Collectors.groupingBy(Candidate::kind, Collectors.counting()));
        return counts.entrySet().stream()
            .min(Comparator.comparing((Map.Entry<String, Long> e) -> -e.getValue()).thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey).orElse("");
    }
}
