package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.maps;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.strings;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

// Chooses the reach an ordered test's chain comes from: one to the system (external-calls/2), else one whose point is
// among the system's points, else one of the system's kinds, else, with no system, one of the entry's kind; of the
// entry's kind first each time. A test reaching two systems of one kind may have one reach for both.
final class Reaches {

    private static final String KIND = "kind";
    private static final String POINT = "point";

    private Reaches() {
    }

    static Optional<Map<String, Object>> choose(final Map<String, Object> test,
                                                final Optional<Map<String, Object>> system, final String kind) {
        final List<Map<String, Object>> reaches = maps(test.get("reaches"));
        final Predicate<Map<String, Object>> ofKind = r -> kind.equals(text(r, KIND));
        final Set<String> points = Set.copyOf(system.map(s -> strings(s.get("points"))).orElse(List.of()));
        final Set<String> kinds = Set.copyOf(system.map(s -> strings(s.get("kinds"))).orElse(List.of()));
        final String name = system.map(s -> text(s, "name")).orElse("");
        final Predicate<Map<String, Object>> toSystem = r -> !name.isEmpty() && name.equals(text(r, "system"));
        final Predicate<Map<String, Object>> atPoint = r -> points.contains(text(r, POINT));
        final List<Predicate<Map<String, Object>>> preferences = List.of(toSystem.and(ofKind), toSystem,
            atPoint.and(ofKind), atPoint, r -> kinds.contains(text(r, KIND)), ofKind);
        return preferences.stream().flatMap(p -> reaches.stream().filter(p).limit(1)).findFirst();
    }
}
