package io.github.fiftieshousewife.undertesttools.antipatterns;

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
import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.Match;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

// Runs PMD with the toolkit's anti-pattern rules, and PMD's copy-paste detector, over the test folders, in this
// process: nothing else needs installing.
final class PmdRunner {

    /** A rule's hit: the rule, the file from the repo root, and the line. */
    record Hit(String rule, String file, int line) {
        // The kind a rule finds: its name up to __, with _ as -.
        String kind() {
            final int end = rule.indexOf("__");
            return (end < 0 ? rule : rule.substring(0, end)).replace('_', '-');
        }
    }

    /** One stretch of copied code: the file from the repo root and its lines. */
    record Copy(String file, int beginLine, int endLine) {
    }

    static final String BUILT_IN_RULES = "/anti-patterns.pmd.xml";
    private static final int MINIMUM_TOKENS = 75;

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

    // Groups of copied code, ignoring literals, of at least 75 tokens.
    static List<List<Copy>> copies(final Path repo, final List<String> folders) {
        final CPDConfiguration config = new CPDConfiguration();
        config.setOnlyRecognizeLanguage(LanguageRegistry.CPD.getLanguageById("java"));
        config.setMinimumTileSize(MINIMUM_TOKENS);
        config.setIgnoreLiterals(true);
        existing(repo, folders).forEach(config::addInputPath);
        final List<List<Copy>> groups = new ArrayList<>();
        try (CpdAnalysis cpd = CpdAnalysis.create(config)) {
            cpd.performAnalysis(report -> report.getMatches().forEach(match -> groups.add(group(repo, match))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return groups;
    }

    private static List<Copy> group(final Path repo, final Match match) {
        return match.getMarkSet().stream()
            .map(mark -> new Copy(relative(repo, mark.getLocation().getFileId().getAbsolutePath()),
                mark.getLocation().getStartLine(), mark.getLocation().getEndLine()))
            .sorted(Comparator.comparing(Copy::file).thenComparingInt(Copy::beginLine))
            .toList();
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
            final Path copy = Files.createTempFile("anti-patterns", ".pmd.xml");
            copy.toFile().deleteOnExit();
            Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
            return copy;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
