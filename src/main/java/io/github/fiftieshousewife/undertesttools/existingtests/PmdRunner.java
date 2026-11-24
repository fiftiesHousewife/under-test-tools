package io.github.fiftieshousewife.undertesttools.existingtests;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

// Runs PMD with the toolkit's rules for meaningless and unreliable tests over the test folders, in this process:
// nothing else needs installing.
final class PmdRunner {

    /** A rule's hit: the rule, the file from the repo root, and the line. */
    record Hit(String rule, String file, int line) {
        // The kind a rule finds: its name up to __, with _ as -.
        String kind() {
            final int end = rule.indexOf("__");
            return (end < 0 ? rule : rule.substring(0, end)).replace('_', '-');
        }
    }

    static final String BUILT_IN_RULES = "/rules.pmd.xml";

    private PmdRunner() {
    }

    static List<Hit> hits(final Path repo, final List<String> folders, final Optional<Path> rules) {
        final PMDConfiguration config = new PMDConfiguration();
        config.setOnlyRecognizeLanguage(LanguageRegistry.PMD.getLanguageById("java"));
        config.addRuleSet(rules.orElseGet(PmdRunner::builtInRules).toString());
        existing(repo, folders).forEach(config::addInputPath);
        config.setFailOnViolation(false);
        final List<Hit> hits = new ArrayList<>();
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            final Report report = pmd.performAnalysisAndCollectReport();
            for (final RuleViolation violation : report.getViolations()) {
                hits.add(new Hit(violation.getRule().getName(),
                    relative(repo, violation.getFileId().getAbsolutePath()), violation.getBeginLine()));
            }
        }
        hits.sort(Comparator.comparing(Hit::file).thenComparingInt(Hit::line).thenComparing(Hit::rule));
        return hits;
    }

    private static List<Path> existing(final Path repo, final List<String> folders) {
        return folders.stream().map(repo::resolve).filter(Files::isDirectory).toList();
    }

    private static String relative(final Path repo, final String absolute) {
        return repo.toAbsolutePath().normalize().relativize(Path.of(absolute).toAbsolutePath().normalize())
            .toString().replace('\\', '/');
    }

    private static Path builtInRules() {
        try (InputStream in = PmdRunner.class.getResourceAsStream(BUILT_IN_RULES)) {
            if (in == null) {
                throw new IllegalStateException("The built-in rules are missing from the jar");
            }
            final Path copy = Files.createTempFile("rules", ".pmd.xml");
            copy.toFile().deleteOnExit();
            Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
            return copy;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
