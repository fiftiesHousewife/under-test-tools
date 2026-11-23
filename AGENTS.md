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
| `src/main/java/.../undertesttools/antipatterns/` | `anti-patterns`: `SourceIndex` and `TestFolders` list the tests, `PmdRunner` runs PMD and its copy-paste detector, `Finder` and `CopyFinder` give hits to tests, `TestsFile`, `AssessmentFile` and `Decisions` write the files |
| `src/main/resources/anti-patterns.pmd.xml` | A copy of under-test's rules, used when `--rules` isn't given; under-test's is the source |
| `src/test/resources/sample-repo/` | A copy of the example repo of under-test's find-test-anti-patterns eval, whose known answers the tests check |
| `build-logic/`, `config/quality/` | The same Gradle conventions and quality rules as under-test |

## Rules for a sub-command
* It does only what a program can decide the same way every time; what needs judgement stays in under-test's skill.
* It writes the toolkit's files exactly as their schemas in under-test say, and changes only the fields its skill owns.
* Every behaviour has a case: in the example repo's known answers, or a test of its own. A fault found on a real repo
  gets a case before it is fixed (under-test's decision on checking tools against real public repos).

## Code standards (enforced by `./gradlew check`)
The same as under-test's: at least 80% line coverage; no more than three parameters; no record with more than three
components; a test with more than one assertion wraps them in `assertAll`; logging through Lombok `@Slf4j`; no
forbidden JDK APIs; short, simple methods; no copy-pasted code; SpotBugs with FindSecBugs clean; final locals, braces
always, no star imports; lines of 120 characters or fewer. Built with Java 25, runs on Java 17.

## Before you finish
Run `./gradlew check`. It must pass.
