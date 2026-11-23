package io.github.fiftieshousewife.undertesttools.antipatterns;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Finds the anti-patterns a program can decide, in the tests of the chosen modules, and gives each to its test:
 * everything find-test-anti-patterns counts except {@code reaches-external-system} and the {@code other} kinds, which
 * need its judgement.
 */
public final class Finder {

    private final Path repo;
    private final SourceIndex index;
    private final Map<String, Finding> findings = new TreeMap<>();

    private Finder(final Path repo, final SourceIndex index) {
        this.repo = repo;
        this.index = index;
    }

    /** The findings, keyed by test name, and how many tests there are in all. */
    public record Result(Map<String, Finding> findings, int tests, int filesNotParsed) {
        public Result {
            findings = Map.copyOf(findings);
        }
    }

    public static Result find(final Path repo, final List<String> modules, final Optional<Path> rules) {
        final List<TestFolders> layouts = modules.stream().map(m -> TestFolders.of(repo, m)).toList();
        final List<String> folders = layouts.stream().flatMap(l -> l.paths().stream()).toList();
        final Finder finder = new Finder(repo, SourceIndex.read(repo, folders));
        layouts.forEach(finder::notRun);
        PmdRunner.hits(repo, folders, rules).forEach(finder::hit);
        new CopyFinder(repo, finder.index, finder::entry).find(PmdRunner.copies(repo, folders));
        final List<Features.Scenario> scenarios = Features.read(repo, resourceFolders(layouts));
        scenarios.stream().filter(Features.Scenario::disabled)
            .forEach(s -> finder.entry(s.test(), s.at()).add(Finding.DISABLED));
        final int tests = finder.index.tests().size() + scenarios.size();
        return new Result(finder.findings, tests, finder.index.getFilesNotParsed());
    }

    Finding entry(final String test, final String at) {
        return findings.computeIfAbsent(test, t -> new Finding(at));
    }

    private Finding entry(final MethodInfo method) {
        return entry(method.testName(), method.at());
    }

    /** Tests in a folder the build doesn't run, or in a class Maven's test runner doesn't pick up, never run. */
    private void notRun(final TestFolders layout) {
        for (final TestFolders.Folder folder : layout.getFolders()) {
            index.getClasses().stream().filter(c -> c.getFolder().equals(folder.path()))
                .filter(c -> !folder.runs() || !layout.runnerPicksUp(topLevelName(c)))
                .forEach(c -> c.tests().forEach(t -> entry(t).add(Finding.NEVER_RUNS)));
        }
    }

    private void hit(final PmdRunner.Hit hit) {
        final Optional<MethodInfo> method = index.methodAt(hit.file(), hit.line());
        if (method.isEmpty()) {
            index.classAt(hit.file(), hit.line()).ifPresent(c -> c.tests().forEach(t -> entry(t).add(hit.kind())));
            return;
        }
        final MethodInfo m = method.get();
        if (dropped(hit, m)) {
            return;
        }
        if (m.isTest() || Finding.NEVER_RUNS.equals(hit.kind())) {
            entry(m).add(hit.kind());
        } else if (m.isSetUp() && "sleeps".equals(hit.kind())) {
            index.classAt(hit.file(), hit.line()).ifPresent(c -> c.tests().forEach(t -> entry(t).add(hit.kind())));
        }
    }

    /** A detached-test hit in a JUnit 3 class is a real test; the expected-exception idiom swallows nothing. */
    private boolean dropped(final PmdRunner.Hit hit, final MethodInfo method) {
        if (hit.rule().equals("never_runs__DetachedTest")) {
            return index.classAt(hit.file(), hit.line()).map(index::isJunit3).orElse(false);
        }
        return "swallows-failures".equals(hit.kind()) && expectedExceptionIdiom(hit, method);
    }

    /** A {@code try} whose last statement is {@code fail(...)}: the catch holds the exception the test expects. */
    private boolean expectedExceptionIdiom(final PmdRunner.Hit hit, final MethodInfo method) {
        final CompilationUnit unit = parse(repo.resolve(hit.file()));
        return unit.findAll(TryStmt.class).stream()
            .filter(t -> SourceIndex.line(t) >= method.getBeginLine() && SourceIndex.endLine(t) <= method.getEndLine())
            .filter(t -> SourceIndex.line(t) <= hit.line() && SourceIndex.endLine(t) >= hit.line())
            .anyMatch(Finder::endsInFail);
    }

    private static boolean endsInFail(final TryStmt tryStmt) {
        final List<Statement> body = tryStmt.getTryBlock().getStatements();
        if (body.isEmpty() || !(body.get(body.size() - 1) instanceof ExpressionStmt last)) {
            return false;
        }
        return last.getExpression().isMethodCallExpr()
            && "fail".equals(last.getExpression().asMethodCallExpr().getNameAsString());
    }

    private static CompilationUnit parse(final Path file) {
        try {
            return StaticJavaParser.parse(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }

    private static String topLevelName(final ClassInfo type) {
        final String file = type.getFile();
        return file.substring(file.lastIndexOf('/') + 1, file.length() - ".java".length());
    }

    private static List<String> resourceFolders(final List<TestFolders> layouts) {
        final List<String> folders = new ArrayList<>();
        layouts.forEach(l -> l.getFolders().stream().filter(TestFolders.Folder::runs)
            .forEach(f -> folders.add(f.path().substring(0, f.path().length() - "java".length()) + "resources")));
        return folders;
    }
}
