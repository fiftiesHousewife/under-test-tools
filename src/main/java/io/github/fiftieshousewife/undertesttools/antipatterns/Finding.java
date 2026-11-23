package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.util.Set;
import java.util.TreeSet;

/** What was found about one test: where it is, the kinds it counts under, and for a copy, one test it copies. */
public final class Finding {

    static final String NEVER_RUNS = "never-runs";
    static final String DISABLED = "disabled";
    static final String COPY_PASTED = "copy-pasted";

    private final String at;
    private final Set<String> kinds = new TreeSet<>();
    private String copyOf;

    Finding(final String at) {
        this.at = at;
    }

    void add(final String kind) {
        kinds.add(kind);
    }

    void copies(final String test) {
        if (copyOf == null || test.compareTo(copyOf) < 0) {
            copyOf = test;
        }
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

    public String getCopyOf() {
        return getKinds().contains(COPY_PASTED) ? copyOf : null;
    }
}
