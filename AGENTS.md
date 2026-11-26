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
| `src/main/java/.../undertesttools/repairorders/` | `repair-orders`, which joins `testing/external-calls.json`, `testing/tests.yaml` and `testing/external-systems.calm.json` into `testing/repair-orders.json`, one work order per external system: `Inputs` reads the three (`Facts`, `SystemsList`), `Grouping` sorts the tests into orders and those left out, `OrderedTest`, `Reaches` and `Injection` write each test's part, `SharedChain` the chain they share, `Orders` the file |
| `src/main/resources/rules.pmd.xml` | A copy of under-test's rules for meaningless and unreliable tests, used when `--rules` isn't given; under-test's is the source |
| `src/test/resources/sample-repo/` | A copy of the example repo of under-test's find-meaningless-and-unreliable-tests eval, whose known answers the tests check |
| `src/test/resources/repair-orders-repo/` | `repair-orders`' example repo: a shop's three `testing/` files, and the known answer in `expected/repair-orders.json` |
| `src/test/java/.../repairorders/ExampleRepo.java` | Copies `repair-orders`' example repo for a test, and reads its known answer |
| `config/spotbugs-exclude.xml` | This repo's own SpotBugs exclusions, named by `underTest.spotbugsExcludeFilter` in `gradle.properties` |
| `docs/design.md` | How the tool is put together, and how each sub-command does its work |
| `docs/decisions/` | This repo's decision records, with their index and template; the toolkit-wide ones are under-test's |
| `docs/releases/` | Each release's notes for users, `<version>.md`, which the release workflow puts on the GitHub release |
| `.github/skills/` | Skills for assistants working here: add-a-sub-command, work-on-an-item, write-a-decision-record; `.claude/skills/` links to them, and `apm.yml` adds the skills they use |
| `.github/workflows/` | `build.yml` (every check, on each push and pull request), `release.yml` (on a version tag), `sync-backlog.yml` |
| `plan/under-test-tools-backlog.yaml` | This repo's own work, checked by `schema/backlog-seed.schema.json` and synced to its issues; `docs/BACKLOG.md` is generated from them |
| `maintainer-tools/release/release.sh` | Starts a release: checks it is ready, then pushes its tag |
| `maintainer-tools/release/set-up-signing.sh`, `KEYS` | Sets up the key releases are signed with, as the secrets `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`; `KEYS` is its public half |

## Rules for a sub-command
Each rule is a [decision record](docs/decisions/README.md); the add-a-sub-command skill puts them in order.
* It does only what a program can decide the same way every time; what needs judgement stays in under-test's skill
  (0001). The toolkit's skills live in under-test, never here.
* It writes the toolkit's files exactly as their schemas in under-test say, and changes only the fields its skill owns
  (0002).
* Every behaviour has a case: in the example repo's known answers, or a test of its own. A fault found on a real repo
  gets a case before it is fixed (0003).
* The same inputs give the same files, byte for byte (0004).
* It reads the repo's files, runs none of its code, and writes only under its `testing/` folder (0005).

## Decisions and work
* A settled choice gets a record in `docs/decisions/` in the same change (the write-a-decision-record skill). Which
  steps this tool takes on, and the schemas, are under-test's decisions.
* This repo's own work is in `plan/under-test-tools-backlog.yaml` (the work-on-an-item skill); a new sub-command's
  item is in under-test's backlog, beside its skill.
* Keep the docs in step with the code: the README for users, `docs/design.md` for how it works, this file's table.

## Code standards (enforced by `./gradlew check`)
This repo applies the [java-conventions plugin](https://github.com/fiftiesHousewife/under-test-java-conventions#readme),
pinned in `settings.gradle.kts`; its README is the one list of the standards: short methods and files, low
complexity, no more than three parameters, exactly one assertion statement per test, no comment on a field or enum
entry, Javadoc only on public API, no copy-pasted code (50 tokens or more), packages of 20 classes or fewer,
SpotBugs with FindSecBugs, forbidden APIs, and at least 80% line coverage. For-each loops are reported by
`pmdAdvice`, never failing the build. Built with Java 25, runs on Java 17.

## Before you finish
Run `./gradlew check`. It must pass. A release is made only by `maintainer-tools/release/release.sh`, after the
version and its notes are pushed.
