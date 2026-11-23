package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code anti-patterns}: find-test-anti-patterns' mechanical steps. It finds the kinds a program can decide, writes
 * them to {@code testing/tests.yaml}, and works the {@code anti_patterns} section and proposed decisions of
 * {@code testing/assessment.yaml} out from that list. With {@code --summary-only} it only does the last part, after
 * the skill has added the kinds it decides by reading.
 */
@Slf4j
public final class AntiPatternsCommand {

    static final String USAGE = """
        java -jar under-test-tools.jar anti-patterns [--repo <dir>] [--module <folder>]... [--rules <pmd ruleset>]
                                                     [--version <toolkit version>] [--commit <commit>]
                                                     [--testing <folder>] [--summary-only]

          --repo <dir>          the repo (default: the current folder)
          --module <folder>     a module to cover, from the repo root (default: the whole repo as one module)
          --rules <file>        the toolkit's anti-patterns.pmd.xml (default: the copy built in)
          --version <version>   the toolkit's version, for found_by (default: unreleased)
          --commit <commit>     the repo's commit, for found_by (default: git rev-parse --short HEAD)
          --testing <folder>    where testing/tests.yaml and testing/assessment.yaml are (default: testing)
          --summary-only        only rework the summary and decisions from testing/tests.yaml
        """;

    private AntiPatternsCommand() {
    }

    public static int run(final List<String> args) {
        if (args.contains("--help")) {
            log.info(USAGE);
            return 0;
        }
        final Options options = Options.parse(args);
        final Path testing = options.getRepo().resolve(options.getTesting());
        final Map<String, Object> foundBy = new LinkedHashMap<>();
        foundBy.put("version", options.getVersion());
        foundBy.put("commit", options.givenCommit().orElseGet(() -> commit(options.getRepo())));
        if (!options.isSummaryOnly()) {
            final Finder.Result result = Finder.find(options.getRepo(), modulesOrRoot(options), options.rulesFile());
            final Map<String, Object> testsFoundBy = new LinkedHashMap<>(foundBy);
            if (!options.getModules().isEmpty()) {
                testsFoundBy.put("modules", options.getModules());
            }
            TestsFile.update(testing.resolve("tests.yaml"),
                new TestsFile.Update(result.findings(), testsFoundBy, options.getModules()));
            log.info("{} tests; {} with an anti-pattern this tool decides.", result.tests(), result.findings().size());
        }
        final List<AssessmentFile.Summary> summary =
            AssessmentFile.summarise(TestsFile.parse(TestsFile.read(testing.resolve("tests.yaml"))));
        final Path assessment = testing.resolve("assessment.yaml");
        AssessmentFile.update(assessment, AssessmentFile.section(foundBy, options.getModules(), summary),
            String.valueOf(options.getRepo().toAbsolutePath().normalize().getFileName()));
        AssessmentFile.write(assessment, Decisions.update(TestsFile.read(assessment),
            summary.stream().map(AssessmentFile.Summary::id).toList()));
        summary.forEach(s -> log.info("{}: {} tests, e.g. {}", s.id(), s.tests(), s.examples()));
        return 0;
    }

    private static List<String> modulesOrRoot(final Options options) {
        return options.getModules().isEmpty() ? List.of("") : options.getModules();
    }

    /** What {@code git rev-parse --short HEAD} prints in the repo; there must be a git repository. */
    static String commit(final Path repo) {
        try {
            final Process git = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(repo.toFile()).redirectErrorStream(true).start();
            final String out;
            try (InputStream in = git.getInputStream()) {
                out = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
            }
            if (git.waitFor() != 0) {
                throw new IllegalStateException("No git repository at " + repo + ": " + out);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("Can't run git", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted running git", e);
        }
    }

    /** The command line, read. */
    @lombok.Value
    @lombok.Builder
    static class Options {
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
            final Map<String, String> values = new LinkedHashMap<>();
            final List<String> modules = new ArrayList<>();
            boolean summaryOnly = false;
            for (int i = 0; i < args.size(); i++) {
                final String flag = args.get(i);
                if ("--summary-only".equals(flag)) {
                    summaryOnly = true;
                    continue;
                }
                if (i + 1 >= args.size() || !flag.startsWith("--")) {
                    throw new IllegalArgumentException("Expected an option with a value at " + flag + "\n\n" + USAGE);
                }
                final String value = args.get(++i);
                if ("--module".equals(flag)) {
                    modules.add(value.replace('\\', '/').replaceAll("/+$", ""));
                } else {
                    values.put(flag, value);
                }
            }
            final List<String> known = List.of("--repo", "--rules", "--version", "--commit", "--testing");
            values.keySet().stream().filter(k -> !known.contains(k)).findFirst().ifPresent(k -> {
                throw new IllegalArgumentException("Unknown option " + k + "\n\n" + USAGE);
            });
            return Options.builder().repo(Path.of(values.getOrDefault("--repo", "."))).modules(List.copyOf(modules))
                .rules(values.containsKey("--rules") ? Path.of(values.get("--rules")) : null)
                .version(values.getOrDefault("--version", "unreleased")).commit(values.get("--commit"))
                .testing(values.getOrDefault("--testing", "testing")).summaryOnly(summaryOnly).build();
        }
    }
}
