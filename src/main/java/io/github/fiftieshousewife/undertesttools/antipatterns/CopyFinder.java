package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Finds copied tests: every test holding a stretch of code the copy-paste detector found in another test, and every
 * test in a test class with a copy, the same or nearly the same, under another test folder.
 */
final class CopyFinder {

    /** Share of distinct lines two classes must have in common to count as copies of each other. */
    private static final double NEARLY_THE_SAME = 0.8;

    private final Path repo;
    private final SourceIndex index;
    private final BiFunction<String, String, Finding> entry;

    CopyFinder(final Path repo, final SourceIndex index, final BiFunction<String, String, Finding> entry) {
        this.repo = repo;
        this.index = index;
        this.entry = entry;
    }

    void find(final List<List<PmdRunner.Copy>> groups) {
        groups.forEach(this::group);
        copiedClasses();
    }

    private void group(final List<PmdRunner.Copy> copies) {
        final Set<MethodInfo> tests = new TreeSet<>((a, b) -> a.testName().compareTo(b.testName()));
        for (final PmdRunner.Copy copy : copies) {
            index.methodAt(copy.file(), copy.beginLine())
                .filter(MethodInfo::isTest)
                .ifPresent(tests::add);
        }
        if (tests.size() > 1) {
            mark(new ArrayList<>(tests));
        }
    }

    private void mark(final List<MethodInfo> tests) {
        for (final MethodInfo test : tests) {
            final Finding finding = entry.apply(test.testName(), test.at());
            finding.add(Finding.COPY_PASTED);
            tests.stream().filter(other -> other != test).findFirst().ifPresent(o -> finding.copies(o.testName()));
        }
    }

    /** Test classes of the same name under different folders, with nearly the same lines. */
    private void copiedClasses() {
        final Map<String, List<ClassInfo>> byName = index.getClasses().stream()
            .filter(c -> !c.tests().isEmpty())
            .collect(Collectors.groupingBy(ClassInfo::getName, TreeMap::new, Collectors.toList()));
        for (final List<ClassInfo> same : byName.values()) {
            final long folders = same.stream().map(ClassInfo::getFolder).distinct().count();
            if (folders > 1 && nearlyTheSame(same.get(0), same.get(1))) {
                markClasses(same.get(0), same.get(1));
                markClasses(same.get(1), same.get(0));
            }
        }
    }

    private void markClasses(final ClassInfo type, final ClassInfo copy) {
        for (final MethodInfo test : type.tests()) {
            final Finding finding = entry.apply(test.testName() + folderSuffix(type, copy), test.at());
            finding.add(Finding.COPY_PASTED);
            final Optional<MethodInfo> same = copy.tests().stream()
                .filter(t -> t.getName().equals(test.getName())).findFirst();
            finding.copies(same.orElse(copy.tests().get(0)).testName() + folderSuffix(copy, type));
        }
    }

    /**
     * Two classes with the same name in different folders also have the same test names; the one outside the first
     * folder in byte order gets its folder added, so both have an entry.
     */
    private static String folderSuffix(final ClassInfo type, final ClassInfo other) {
        return type.getFolder().compareTo(other.getFolder()) > 0 ? " (" + type.getFolder() + ")" : "";
    }

    private boolean nearlyTheSame(final ClassInfo a, final ClassInfo b) {
        final Set<String> linesA = lines(a);
        final Set<String> linesB = lines(b);
        final Set<String> shared = new HashSet<>(linesA);
        shared.retainAll(linesB);
        final int larger = Math.max(linesA.size(), linesB.size());
        return larger > 0 && (double) shared.size() / larger >= NEARLY_THE_SAME;
    }

    private Set<String> lines(final ClassInfo type) {
        try {
            return Files.readAllLines(repo.resolve(type.getFile()), StandardCharsets.UTF_8).stream()
                .map(String::strip).filter(l -> !l.isEmpty()).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + type.getFile(), e);
        }
    }
}
