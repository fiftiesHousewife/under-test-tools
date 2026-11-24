package io.github.fiftieshousewife.undertesttools.existingtests;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

// The two lists of tests find-meaningless-and-unreliable-tests keeps, each with its kinds in the skill's table order.
// An other-... kind is meaningless; one outside the four fixed ids comes after them, in byte order.
enum TestList {
    MEANINGLESS("meaningless", List.of("never-runs", "disabled", "no-real-assertion", "swallows-failures",
        "mocks-everything", "other-bent-expected-value", "other-server-nothing-starts",
        "other-name-does-not-match-assertions", "other-copied-setup-unchanged")),
    UNRELIABLE("unreliable", List.of("sleeps", "order-dependent"));

    private final String key;
    private final List<String> kinds;

    TestList(final String key, final List<String> kinds) {
        this.key = key;
        this.kinds = kinds;
    }

    // The field of an entry in testing/tests.yaml, and the start of each decision's key.
    String field() {
        return key;
    }

    // The section of testing/assessment.yaml.
    String section() {
        return key + "_tests";
    }

    boolean holds(final String kind) {
        return kinds.contains(kind) || this == MEANINGLESS && kind.startsWith("other-");
    }

    // The kinds of this list among these, each once, in table order.
    List<String> ordered(final Collection<String> found) {
        return found.stream().filter(this::holds).distinct().sorted(Comparator.comparingInt(this::rank)
            .thenComparing(Comparator.naturalOrder())).toList();
    }

    private int rank(final String kind) {
        final int at = kinds.indexOf(kind);
        return at < 0 ? kinds.size() : at;
    }
}
