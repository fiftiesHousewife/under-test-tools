# Agent guide

Guidance for any AI coding assistant working in this repo, and for the people directing it.

**What this repo is:** under-test-tools, one command-line jar with a sub-command for each mechanical step of
[under-test](https://github.com/fiftiesHousewife/under-test)'s journey. It follows under-test's plain-English rules
([GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)): no repurposed words, no dates in
text, things referred to by name.

## Where things are
| Path | What |
|---|---|
| `src/main/java/.../undertesttools/Main.java` | Chooses the sub-command |
| `src/main/java/.../undertesttools/existingtests/` | `meaningless-and-unreliable`, which finds a repo's meaningless tests (they never run, can't fail, or check only their mocks) and unreliable tests (they sleep, or depend on the order tests run in): `SourceIndex` and `TestFolders` list the tests, `PmdRunner` runs PMD, `Finder` gives hits to tests, `TestList` holds the two lists' kinds, `TestsFile`, `AssessmentFile` and `Decisions` write the files |
| `src/main/resources/rules.pmd.xml` | A copy of under-test's rules for meaningless and unreliable tests, used when `--rules` isn't given; under-test's is the source |
| `src/test/resources/sample-repo/` | A copy of the example repo of under-test's find-meaningless-and-unreliable-tests eval, whose known answers the tests check |
| `build-logic/` | How this repo is published (Maven Central, with a bill of materials) |
| `config/spotbugs-exclude.xml` | This repo's own SpotBugs exclusions, named by `underTest.spotbugsExcludeFilter` in `gradle.properties` |

## Rules for a sub-command
* It does only what a program can decide the same way every time; what needs judgement stays in under-test's skill.
* It writes the toolkit's files exactly as their schemas in under-test say, and changes only the fields its skill owns.
* Every behaviour has a case: in the example repo's known answers, or a test of its own. A fault found on a real repo
  gets a case before it is fixed (under-test's decision on checking tools against real public repos).

## Code standards (enforced by `./gradlew check`)
This repo applies the [java-conventions plugin](https://github.com/fiftiesHousewife/under-test-java-conventions#readme),
pinned in `settings.gradle.kts`; its README is the one list of the standards: short methods and files, low
complexity, no more than three parameters, exactly one assertion statement per test, no comment on a field or enum
entry, Javadoc only on public API, no copy-pasted code (50 tokens or more), packages of 20 classes or fewer,
SpotBugs with FindSecBugs, forbidden APIs, and at least 80% line coverage. For-each loops are reported by
`pmdAdvice`, never failing the build. Built with Java 25, runs on Java 17.

## Before you finish
Run `./gradlew check`. It must pass.
