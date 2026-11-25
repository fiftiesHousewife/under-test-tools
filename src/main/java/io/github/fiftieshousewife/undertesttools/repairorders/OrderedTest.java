package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.flag;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.optional;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.strings;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// One test in an order: who it is, where the stand-in goes, its chain from the facts, and the evidence it reaches.
final class OrderedTest {

    private static final String CHAIN = "chain";

    private OrderedTest() {
    }

    /**
     * What the order needs of the test: its part of the file, and for the order's shared chain whether it has a chain
     * and that chain's steps outside the test's class.
     */
    record Built(Map<String, Object> json, boolean hasChain, List<String> outside) {
    }

    /** The order's node, its stand-in approach, and the facts the test's reach comes from. */
    record Context(Facts facts, Map<String, Object> node, String approach) {
    }

    static Built build(final Candidate candidate, final Context context) {
        final Optional<Map<String, Object>> factsTest = context.facts().test(candidate.name());
        final Optional<Map<String, Object>> system =
            factsTest.flatMap(t -> context.facts().system(t, context.node(), candidate.kind()));
        final Map<String, Object> reach =
            factsTest.flatMap(t -> Reaches.choose(t, system, candidate.kind())).orElse(Map.of());
        final List<String> chain = strings(reach.get(CHAIN));
        final Map<String, Object> json = identity(candidate);
        json.put("framework", Fields.optional(factsTest.orElse(Map.of()), "test_framework").orElse("unknown"));
        json.put("inject", Injection.of(candidate.name(), reach, context.facts()));
        json.put(CHAIN, chain);
        json.put("through_mock", Optional.ofNullable(reach.get("through_mock")).orElse(false));
        json.put("stand_ins", strings(factsTest.orElse(Map.of()).get("stand_ins")).stream().sorted().toList());
        json.put("evidence", evidence(candidate));
        waitsFor(context, system).ifPresent(w -> json.put("waits_for", w));
        return new Built(json, !chain.isEmpty(), Injection.outsideTheTest(candidate.name(), chain));
    }

    private static Map<String, Object> identity(final Candidate candidate) {
        final String test = candidate.name();
        final int dot = test.lastIndexOf('.');
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put("test", test);
        json.put("class", dot < 0 ? "" : test.substring(0, dot));
        json.put("method", test.substring(dot + 1));
        json.put("at", text(candidate.test(), "at"));
        return json;
    }

    private static Map<String, Object> evidence(final Candidate candidate) {
        final Map<String, Object> measurement = candidate.measurement();
        final Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("grade", candidate.grade());
        evidence.put("reason", text(candidate.entry(), "reason"));
        optional(candidate.entry(), "found_by").ifPresent(f -> evidence.put("found_by", f));
        if (measurement.containsKey("called")) {
            evidence.put("called", strings(measurement.get("called")).stream().sorted().toList());
        }
        evidence.put("missed_by_discovery", flag(measurement, "missed_by_discovery"));
        if (!candidate.namedHosts().isEmpty()) {
            evidence.put("named_hosts", candidate.namedHosts());
        }
        return evidence;
    }

    private static Optional<String> waitsFor(final Context context, final Optional<Map<String, Object>> system) {
        if ("not-chosen".equals(context.approach())) {
            return Optional.of("stand-in-not-chosen");
        }
        return system.filter(context.facts()::addressInCode).map(s -> "address-in-code");
    }
}
