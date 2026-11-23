package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proposes a decision for each finding under {@code decisions:}, keyed {@code anti-patterns/<id>}: a missing one is
 * appended as proposed, one whose finding has gone becomes stale, a stale one whose finding is back is proposed again,
 * and every other decision stays as people left it.
 */
final class Decisions {

    static final String QUESTION = "Should these tests check what their names say, or what their assertions check?";
    private static final String PREFIX = "  anti-patterns/";

    private Decisions() {
    }

    static String choice(final String id) {
        return switch (id) {
            case "disabled" -> "Application Owner decides: re-enable, or delete once the approval-test baseline exists";
            case "never-runs" ->
                "Application Owner decides: make it a real test, or delete once the approval-test baseline exists";
            case "reaches-external-system" -> "tagged and left out of normal runs";
            case "sleeps", "order-dependent" -> "fixed in the fix-flaky-tests workstream";
            default -> "fixed in the strengthen-tests workstream";
        };
    }

    /** The file's text with the decisions brought up to date for these finding ids, in finding order. */
    static String update(final String text, final List<String> ids) {
        final List<String> lines = new ArrayList<>(text.lines().toList());
        final Map<String, Object> existing = existing(text);
        restate(lines, existing, Set.copyOf(ids));
        final List<String> added = ids.stream().filter(id -> !existing.containsKey("anti-patterns/" + id))
            .flatMap(id -> proposed(id).stream()).toList();
        if (!added.isEmpty()) {
            append(lines, added);
        }
        return String.join("\n", lines) + "\n";
    }

    /** Stale where the finding has gone; proposed again where a stale one's finding is back. */
    private static void restate(final List<String> lines, final Map<String, Object> existing, final Set<String> ids) {
        for (final Map.Entry<String, Object> decision : existing.entrySet()) {
            final String id = decision.getKey().substring("anti-patterns/".length());
            final String state = state(decision.getValue());
            if ("stale".equals(state) && ids.contains(id)) {
                setState(lines, decision.getKey(), "proposed");
            } else if (!"stale".equals(state) && !ids.contains(id)) {
                setState(lines, decision.getKey(), "stale");
            }
        }
    }

    /** At the end of the decisions section, which is added at the end of the file if there is none. */
    private static void append(final List<String> lines, final List<String> added) {
        int at = AssessmentFile.topLevel(lines, "decisions");
        if (at < 0) {
            lines.add("decisions:");
            at = lines.size() - 1;
        }
        int end = at + 1;
        while (end < lines.size() && (lines.get(end).isEmpty() || Character.isWhitespace(lines.get(end).charAt(0)))) {
            end++;
        }
        lines.addAll(end, added);
    }

    private static List<String> proposed(final String id) {
        final List<String> lines = new ArrayList<>(List.of(PREFIX + id + ":", "    state: proposed",
            "    drafted_by: ai"));
        lines.add("other-name-does-not-match-assertions".equals(id) ? "    question: " + YamlText.scalar(QUESTION)
            : "    choice: " + YamlText.scalar(choice(id)));
        return lines;
    }

    private static void setState(final List<String> lines, final String key, final String state) {
        final int at = lines.indexOf("  " + key + ":");
        for (int i = at + 1; at >= 0 && i < lines.size() && lines.get(i).startsWith("    "); i++) {
            if (lines.get(i).startsWith("    state:")) {
                lines.set(i, "    state: " + state);
                return;
            }
        }
    }

    private static String state(final Object decision) {
        return decision instanceof Map<?, ?> m ? String.valueOf(m.get("state")) : "";
    }

    private static Map<String, Object> existing(final String text) {
        final Object decisions = TestsFile.parse(text).get("decisions");
        if (!(decisions instanceof Map<?, ?> all)) {
            return Map.of();
        }
        return all.entrySet().stream().filter(e -> String.valueOf(e.getKey()).startsWith("anti-patterns/"))
            .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> (Object) e.getValue(),
                (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
