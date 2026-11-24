package io.github.fiftieshousewife.undertesttools.existingtests;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** What was found about one test: where it is, and the kinds it counts under. */
public final class Finding {

    static final String NEVER_RUNS = "never-runs";
    static final String DISABLED = "disabled";

    private final String at;
    private final Set<String> kinds = new TreeSet<>();

    Finding(final String at) {
        this.at = at;
    }

    void add(final String kind) {
        kinds.add(kind);
    }

    public String getAt() {
        return at;
    }

    /** A test that never runs counts only under never-runs, and a disabled one only under disabled. */
    public Set<String> getKinds() {
        if (kinds.contains(NEVER_RUNS)) {
            return Set.of(NEVER_RUNS);
        }
        if (kinds.contains(DISABLED)) {
            return Set.of(DISABLED);
        }
        return Set.copyOf(kinds);
    }

    // The kinds it counts under on one of the two lists, in table order.
    List<String> on(final TestList list) {
        return list.ordered(getKinds());
    }
}
