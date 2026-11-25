package io.github.fiftieshousewife.undertesttools.repairorders;

import java.util.List;
import java.util.stream.IntStream;

// The calls every ordered test with a chain goes through: the longest common end of their chains' steps outside each
// test's own class, in call order; empty when they share only the connection point or nothing.
final class SharedChain {

    private SharedChain() {
    }

    static List<String> of(final List<List<String>> outside) {
        final int shortest = outside.stream().mapToInt(List::size).min().orElse(0);
        final int common = (int) IntStream.rangeClosed(1, shortest)
            .takeWhile(n -> outside.stream().map(t -> t.get(t.size() - n)).distinct().count() == 1).count();
        return common <= 1 ? List.of()
            : List.copyOf(outside.get(0).subList(outside.get(0).size() - common, outside.get(0).size()));
    }
}
