package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.List;
import java.util.Map;

// One test's entry in testing/tests.yaml for one node: the test's whole entry, its external_systems entry, and the
// nodes of the hosts its own code or settings name for the same client, when it's ordered under a code-path entry.
record Candidate(Map<String, Object> test, Map<String, Object> entry, List<String> namedHosts) {

    Candidate(final Map<String, Object> test, final Map<String, Object> entry) {
        this(test, entry, List.of());
    }

    Candidate withNamedHosts(final List<String> hosts) {
        return new Candidate(test, entry, hosts);
    }

    String name() {
        return text(test, "test");
    }

    String node() {
        return text(entry, "node");
    }

    String grade() {
        return text(entry, "grade");
    }

    String kind() {
        return text(entry, "kind");
    }

    String foundBy() {
        return text(entry, "found_by");
    }

    String repairState() {
        return text(map(test.get("repair")), "state");
    }

    Map<String, Object> measurement() {
        return map(test.get("measurement"));
    }
}
