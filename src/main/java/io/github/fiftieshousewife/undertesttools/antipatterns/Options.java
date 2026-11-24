package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Value;

// The anti-patterns sub-command's options, read from its command line.
@Value
@Builder
class Options {
    private static final String MODULE = "--module";
    private static final List<String> KNOWN = List.of("--repo", "--rules", "--version", "--commit", "--testing");

    Path repo;
    List<String> modules;
    Path rules;
    String version;
    String commit;
    String testing;
    boolean summaryOnly;

    Optional<Path> rulesFile() {
        return Optional.ofNullable(rules);
    }

    Optional<String> givenCommit() {
        return Optional.ofNullable(commit);
    }

    static Options parse(final List<String> args) {
        final List<String> rest = new ArrayList<>(args);
        final boolean summaryOnly = rest.remove("--summary-only");
        final List<Map.Entry<String, String>> pairs = pairs(rest);
        final List<String> modules = pairs.stream().filter(p -> MODULE.equals(p.getKey()))
            .map(p -> p.getValue().replace('\\', '/').replaceAll("/+$", "")).toList();
        final Map<String, String> values = pairs.stream().filter(p -> !MODULE.equals(p.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        values.keySet().stream().filter(k -> !KNOWN.contains(k)).findFirst().ifPresent(k -> {
            throw new IllegalArgumentException("Unknown option " + k + "\n\n" + AntiPatternsCommand.USAGE);
        });
        return from(values, modules, summaryOnly);
    }

    private static List<Map.Entry<String, String>> pairs(final List<String> args) {
        return IntStream.iterate(0, i -> i < args.size(), i -> i + 2).mapToObj(i -> pair(args, i)).toList();
    }

    private static Map.Entry<String, String> pair(final List<String> args, final int at) {
        final String flag = args.get(at);
        if (at + 1 >= args.size() || !flag.startsWith("--")) {
            throw new IllegalArgumentException("Expected an option with a value at " + flag + "\n\n"
                + AntiPatternsCommand.USAGE);
        }
        return Map.entry(flag, args.get(at + 1));
    }

    private static Options from(final Map<String, String> values, final List<String> modules, final boolean summary) {
        return Options.builder().repo(Path.of(values.getOrDefault("--repo", "."))).modules(modules)
            .rules(Optional.ofNullable(values.get("--rules")).map(Path::of).orElse(null))
            .version(values.getOrDefault("--version", "unreleased")).commit(values.get("--commit"))
            .testing(values.getOrDefault("--testing", "testing")).summaryOnly(summary).build();
    }
}
