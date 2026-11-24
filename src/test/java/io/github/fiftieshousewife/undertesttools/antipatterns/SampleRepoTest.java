package io.github.fiftieshousewife.undertesttools.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The example repo of find-test-anti-patterns' eval, with its known answers for every kind this tool decides. The
 * skill adds reaches-external-system itself.
 */
class SampleRepoTest {

    @TempDir
    static Path workspace;
    private static Path repo;
    private static String testsYaml;
    private static String assessment;

    @BeforeAll
    static void runTheToolOnACopy() throws IOException {
        repo = workspace.resolve("sample-repo");
        copy(Path.of("src/test/resources/sample-repo"), repo);
        AntiPatternsCommand.run(List.of("--repo", repo.toString(), "--commit", "1a2b3c4"));
        testsYaml = Files.readString(repo.resolve("testing/tests.yaml"));
        assessment = Files.readString(repo.resolve("testing/assessment.yaml"));
    }

    @Test
    void countsEveryKindAsTheEvalExpects() {
        final Map<String, Integer> counts = AssessmentFile.summarise(YamlFile.parse(testsYaml)).stream()
            .collect(Collectors.toMap(AssessmentFile.Summary::id, AssessmentFile.Summary::tests));
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("disabled", 5, "never-runs", 7,
            "no-real-assertion", 5, "swallows-failures", 2, "copy-pasted", 6, "mocks-everything", 2,
            "order-dependent", 3, "sleeps", 2));
    }

    @Test
    void keysEachTestByClassAndNameAtItsMethodLine() {
        assertThat(testsYaml).contains("""
              - test: "com.example.orders.StockLevelTest.reservesStock"
                at: "src/test/java/com/example/orders/StockLevelTest.java:9"
                anti_patterns:
                  kinds: [no-real-assertion]
            """);
    }

    @Test
    void leavesOutTheTestsThatShowNothing() {
        assertAll(
            () -> assertThat(testsYaml).doesNotContain("DiscountRulesContextTest"),
            () -> assertThat(testsYaml).doesNotContain("OrderLookupTest"),
            () -> assertThat(testsYaml).doesNotContain("OrderChecks"));
    }

    @Test
    void doesNotCountTheExpectedExceptionIdiomOrAnAssertingHelper() {
        assertThat(testsYaml).doesNotContain("NotificationTest.rejectsAnEmptyAddress");
    }

    @Test
    void countsTestsInAFolderTheBuildDoesNotRunAsNeverRunning() {
        assertThat(testsYaml).contains("""
              - test: "com.example.orders.CheckoutJourneyTest.checksOutABasketOfTwoItems"
                at: "src/functionalTest/java/com/example/orders/CheckoutJourneyTest.java:9"
                anti_patterns:
                  kinds: [never-runs]
            """);
    }

    @Test
    void countsADisabledScenarioByItsFeatureFileAndTitle() {
        assertThat(testsYaml).contains("""
              - test: "src/test/resources/features/refunds.feature: A refund after 30 days is refused"
                at: "src/test/resources/features/refunds.feature:9"
            """);
    }

    @Test
    void writesTheSummaryAndProposesADecisionForEachFinding() {
        assertAll(
            () -> assertThat(assessment).contains(
                "  found_by: {tool: find-test-anti-patterns, version: unreleased, commit: \"1a2b3c4\"}"),
            () -> assertThat(assessment).contains("""
                    - id: sleeps
                      kind: sleeps
                      tests: 2
                      examples: ["src/test/java/com/example/orders/ReminderJobTest.java:11", \
                "src/test/java/com/example/orders/ReminderJobTest.java:18"]
                """),
            () -> assertThat(assessment).contains("""
                  anti-patterns/disabled:
                    state: proposed
                    drafted_by: ai
                    choice: "Application Owner decides: re-enable, or delete once the approval-test baseline exists"
                """));
    }

    static void copy(final Path from, final Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (final Path file : files.toList()) {
                final Path target = to.resolve(from.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
    }
}
