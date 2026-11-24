package io.github.fiftieshousewife.undertesttools.existingtests;

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
 * The example repo of find-meaningless-and-unreliable-tests' eval, with its known answers for every kind this tool
 * decides. The skill adds the other-... kinds itself.
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
        MeaninglessAndUnreliableCommand.run(List.of("--repo", repo.toString(), "--commit", "1a2b3c4"));
        testsYaml = Files.readString(repo.resolve("testing/tests.yaml"));
        assessment = Files.readString(repo.resolve("testing/assessment.yaml"));
    }

    @Test
    void countsEveryKindAsTheEvalExpects() {
        final Map<String, Object> tests = YamlFile.parse(testsYaml);
        final Map<String, Integer> counts = Stream.of(TestList.values())
            .flatMap(list -> AssessmentFile.summarise(tests, list).stream()
                .map(s -> Map.entry(Decisions.key(list, s.id()), s.tests())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("meaningless/disabled", 5,
            "meaningless/never-runs", 7, "meaningless/no-real-assertion", 5, "meaningless/swallows-failures", 2,
            "meaningless/mocks-everything", 2, "unreliable/order-dependent", 3, "unreliable/sleeps", 2));
    }

    @Test
    void keysEachTestByClassAndNameAtItsMethodLine() {
        assertThat(testsYaml).contains("""
              - test: "com.example.orders.StockLevelTest.reservesStock"
                at: "src/test/java/com/example/orders/StockLevelTest.java:9"
                meaningless: [no-real-assertion]
            """);
    }

    @Test
    void putsEachKindOnItsList() {
        assertThat(testsYaml).contains("""
              - test: "com.example.orders.ReminderJobTest.sendsAReminderAfterADay"
                at: "src/test/java/com/example/orders/ReminderJobTest.java:11"
                unreliable: [sleeps]
            """);
    }

    @Test
    void leavesOutTheTestsThatShowNothing() {
        assertAll(
            () -> assertThat(testsYaml).doesNotContain("DiscountRulesContextTest"),
            () -> assertThat(testsYaml).doesNotContain("OrderLookupTest"),
            () -> assertThat(testsYaml).doesNotContain("OrderChecks"),
            () -> assertThat(testsYaml).doesNotContain("TaxTableTest"),
            () -> assertThat(testsYaml).doesNotContain("anti_patterns"));
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
                meaningless: [never-runs]
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
    void writesBothSectionsMeaninglessFirst() {
        assertAll(
            () -> assertThat(assessment).contains("""
                unreliable_tests:
                  found_by: {tool: find-meaningless-and-unreliable-tests, version: unreleased, commit: "1a2b3c4"}
                  findings:
                    - id: order-dependent
                """),
            () -> assertThat(assessment).contains("""
                    - id: sleeps
                      kind: sleeps
                      tests: 2
                      examples: ["src/test/java/com/example/orders/ReminderJobTest.java:11", \
                "src/test/java/com/example/orders/ReminderJobTest.java:18"]
                decisions:
                """),
            () -> assertThat(assessment.indexOf("meaningless_tests:")).isBetween(0, assessment.indexOf("unreliable")));
    }

    @Test
    void proposesADecisionForEachFinding() {
        assertAll(
            () -> assertThat(assessment).contains("""
                  meaningless/disabled:
                    state: proposed
                    drafted_by: ai
                    choice: "Application Owner decides: re-enable, or delete once the approval-test baseline exists"
                """),
            () -> assertThat(assessment).endsWith("""
                  unreliable/sleeps:
                    state: proposed
                    drafted_by: ai
                    choice: "fixed in the fix-flaky-tests workstream"
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
