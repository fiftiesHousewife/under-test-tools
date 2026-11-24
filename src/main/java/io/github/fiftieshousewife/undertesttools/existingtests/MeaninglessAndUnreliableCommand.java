package io.github.fiftieshousewife.undertesttools.existingtests;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code meaningless-and-unreliable}: find-meaningless-and-unreliable-tests' mechanical steps. It finds the kinds a
 * program can decide, writes them to {@code testing/tests.yaml}, and works the {@code meaningless_tests} and
 * {@code unreliable_tests} sections and proposed decisions of {@code testing/assessment.yaml} out from that list. With
 * {@code --summary-only} it only does the last part, after the skill has added the {@code other-...} kinds it decides
 * by reading.
 */
@Slf4j
public final class MeaninglessAndUnreliableCommand {

    static final String USAGE = """
        java -jar under-test-tools.jar meaningless-and-unreliable [--repo <dir>] [--module <folder>]...
                                       [--rules <pmd ruleset>] [--version <toolkit version>] [--commit <commit>]
                                       [--testing <folder>] [--summary-only]

          --repo <dir>          the repo (default: the current folder)
          --module <folder>     a module to cover, from the repo root (default: the whole repo as one module)
          --rules <file>        the toolkit's rules.pmd.xml (default: the copy built in)
          --version <version>   the toolkit's version, for found_by (default: unreleased)
          --commit <commit>     the repo's commit, for found_by (default: git rev-parse --short HEAD)
          --testing <folder>    where testing/tests.yaml and testing/assessment.yaml are (default: testing)
          --summary-only        only rework the summary and decisions from testing/tests.yaml
        """;

    private MeaninglessAndUnreliableCommand() {
    }

    public static int run(final List<String> args) {
        if (args.contains("--help")) {
            log.info(USAGE);
            return 0;
        }
        final Options options = Options.parse(args);
        final Path testing = options.getRepo().resolve(options.getTesting());
        final Map<String, Object> foundBy = foundBy(options);
        if (!options.isSummaryOnly()) {
            findAndWrite(options, testing.resolve("tests.yaml"), foundBy);
        }
        summarise(options, testing, foundBy);
        return 0;
    }

    private static Map<String, Object> foundBy(final Options options) {
        final Map<String, Object> foundBy = new LinkedHashMap<>();
        foundBy.put("version", options.getVersion());
        foundBy.put("commit", options.givenCommit().orElseGet(() -> commit(options.getRepo())));
        return foundBy;
    }

    private static void findAndWrite(final Options options, final Path testsFile, final Map<String, Object> foundBy) {
        final List<String> modules = options.getModules().isEmpty() ? List.of("") : options.getModules();
        final Finder.Result result = Finder.find(options.getRepo(), modules, options.rulesFile());
        final Map<String, Object> testsFoundBy = new LinkedHashMap<>(foundBy);
        if (!options.getModules().isEmpty()) {
            testsFoundBy.put("modules", options.getModules());
        }
        TestsFile.update(testsFile, new TestsFile.Update(result.findings(), testsFoundBy, options.getModules()));
        log.info("{} tests; {} meaningless or unreliable by the kinds this tool decides.", result.tests(),
            result.findings().size());
    }

    private static void summarise(final Options options, final Path testing, final Map<String, Object> foundBy) {
        final Map<String, Object> tests = YamlFile.parse(YamlFile.read(testing.resolve("tests.yaml")));
        final AssessmentFile.Header header = new AssessmentFile.Header(foundBy, options.getModules());
        final List<String> sections = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        for (final TestList list : TestList.values()) {
            final List<AssessmentFile.Summary> summary = AssessmentFile.summarise(tests, list);
            sections.add(AssessmentFile.section(list, header, summary));
            summary.forEach(s -> keys.add(Decisions.key(list, s.id())));
            summary.forEach(s -> log.info("{}/{}: {} tests, e.g. {}", list.field(), s.id(), s.tests(), s.examples()));
        }
        final Path assessment = testing.resolve("assessment.yaml");
        AssessmentFile.update(assessment, sections,
            String.valueOf(options.getRepo().toAbsolutePath().normalize().getFileName()));
        AssessmentFile.write(assessment, Decisions.update(YamlFile.read(assessment), keys));
    }

    // What git rev-parse --short HEAD prints in the repo; there must be a git repository.
    static String commit(final Path repo) {
        try {
            return gitHead(repo);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't run git", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted running git", e);
        }
    }

    private static String gitHead(final Path repo) throws IOException, InterruptedException {
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
    }
}
