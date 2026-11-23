package io.github.fiftieshousewife.undertesttools.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test folders, Maven's naming rule, feature files and the command line. */
class LayoutTest {

    @Test
    void mavenRunsOnlyTheClassesItsTestRunnerPicksUp(@TempDir final Path repo) throws IOException {
        Files.writeString(repo.resolve("pom.xml"), "<project><build><plugins><plugin>"
            + "<artifactId>maven-failsafe-plugin</artifactId></plugin></plugins></build></project>");
        Files.createDirectories(repo.resolve("src/test/java"));
        Files.createDirectories(repo.resolve("src/it/java"));
        final TestFolders layout = TestFolders.of(repo, "");
        assertAll(
            () -> assertThat(layout.getFolders()).containsExactly(new TestFolders.Folder("src/it/java", false),
                new TestFolders.Folder("src/test/java", true)),
            () -> assertThat(layout.runnerPicksUp("OrderTest")).isTrue(),
            () -> assertThat(layout.runnerPicksUp("OrderIT")).isTrue(),
            () -> assertThat(layout.runnerPicksUp("OrderChecks")).isFalse());
    }

    @Test
    void mavenIncludesReplaceTheDefaults(@TempDir final Path repo) throws IOException {
        Files.writeString(repo.resolve("pom.xml"), "<project><includes><include>**/*Spec.java</include></includes>"
            + "</project>");
        final TestFolders layout = TestFolders.of(repo, "");
        assertAll(
            () -> assertThat(layout.runnerPicksUp("OrderSpec")).isTrue(),
            () -> assertThat(layout.runnerPicksUp("OrderTest")).isFalse());
    }

    @Test
    void readsScenariosAndExampleRowsWithTheirTags() {
        final List<Features.Scenario> scenarios = Features.scenarios("f.feature", List.of(
            "@Disabled", "Feature: Orders", "  Scenario: one", "  Scenario Outline: many", "    Examples:",
            "      | a |", "      | 1 |"));
        assertThat(scenarios).containsExactly(new Features.Scenario("f.feature: one", "f.feature:3", true),
            new Features.Scenario("f.feature: many (row 7)", "f.feature:7", true));
    }

    @Test
    void wrongArgumentsAreExplained() {
        assertAll(
            () -> assertThatThrownBy(() -> AntiPatternsCommand.Options.parse(List.of("--colour", "red")))
                .hasMessageContaining("Unknown option --colour"),
            () -> assertThatThrownBy(() -> AntiPatternsCommand.Options.parse(List.of("--repo")))
                .hasMessageContaining("Expected an option with a value"),
            () -> assertThat(AntiPatternsCommand.run(List.of("--help"))).isZero());
    }

    @Test
    void readsModulesAndTheSummaryOnlySwitch() {
        final AntiPatternsCommand.Options options = AntiPatternsCommand.Options.parse(
            List.of("--module", "server/", "--summary-only", "--version", "1.2"));
        assertAll(
            () -> assertThat(options.getModules()).containsExactly("server"),
            () -> assertThat(options.isSummaryOnly()).isTrue(),
            () -> assertThat(options.getVersion()).isEqualTo("1.2"));
    }

    @Test
    void aRepoWithoutGitIsRefused(@TempDir final Path dir) {
        assertThatThrownBy(() -> AntiPatternsCommand.commit(dir)).isInstanceOf(IllegalStateException.class);
    }
}
