# Design

How under-test-tools is put together, and how each sub-command does its work. Why it is this way is in
[the decisions](decisions/README.md); what each sub-command reads and writes, for a user, is in [the README](../README.md).

## The whole

```
java -jar under-test-tools.jar <sub-command> [options]
        │
        ▼
      Main ── chooses the sub-command by name, and turns what it throws into an exit code
        │
        ├── existingtests.MeaninglessAndUnreliableCommand ── meaningless-and-unreliable
        └── repairorders.RepairOrdersCommand ─────────────── repair-orders
```

* **One package per sub-command**, under `io.github.fiftieshousewife.undertesttools`, each with one public class,
  `<Name>Command`, whose `run(List<String>)` takes the sub-command's options. Everything else in the package is
  package-private. Sub-commands share no code: each reads and writes its own files.
* **Exit codes come from `Main`.** A command returns 0, or throws: `IllegalArgumentException` for a wrong option
  (exit code 2), `UncheckedIOException` or `IllegalStateException` for an input it can't read or use (exit code 1).
  The exception's message is logged, so it says what is wrong in the user's terms.
* **Logging** goes through SLF4J to Log4j 2 (from the java-conventions plugin). Each sub-command logs one summary line
  at the end, which the skill running it quotes.
* **Its version** is the jar's `Implementation-Version`, from `gradle.properties`; `unreleased` when run from the
  classes, as in the tests.

## meaningless-and-unreliable

Package `existingtests`. For each module given with `--module` (the whole repo when none is):

| Step | Classes | What it does |
|---|---|---|
| List the tests | `TestFolders`, `FilesEndingIn`, `SourceIndex`, `ClassReader`, `ClassInfo`, `MethodInfo`, `Junit3`, `Features` | Finds the folders the build runs tests from, and parses each test source with JavaParser: methods marked as JUnit 5 or 4 tests, JUnit 3 tests through superclasses in the repo, and Cucumber scenarios and example rows |
| Run PMD | `PmdRunner` | Runs PMD in this process with under-test's rules (`--rules`, or the copy in `src/main/resources/rules.pmd.xml`) |
| Read the options | `Options` | The command line, with its defaults |
| Give each hit to its test | `Finder`, `Finding` | The method holding the hit; every test of its class for a hit on the class or a sleep in set-up; drops the hits the README lists as not counting. A `Finding` is what was found about one test: where it is, and the kinds it counts under |
| Add what PMD can't see | `Finder`, `TestFolders` | Tests the build never runs: in a folder it doesn't run, in a class Maven's test runner doesn't pick up, or a disabled scenario |
| Write the list | `TestList`, `TestsFile`, `YamlFile`, `YamlText` | Sets each test's `meaningless` and `unreliable` lists in `testing/tests.yaml`, keeping the `other-...` kinds and every other skill's fields as they were |
| Summarise | `AssessmentFile`, `Decisions` | Works `meaningless_tests` and `unreliable_tests` in `testing/assessment.yaml` out from the list, with a proposed decision per finding |

`--summary-only` does only the last step, after the skill has added the kinds it decides by reading.

Its cases are in `src/test/resources/sample-repo/`, a copy of the example repo of under-test's
find-meaningless-and-unreliable-tests eval, whose known count for every kind the tests check.

## repair-orders

Package `repairorders`. It joins three files into one, and decides nothing a person or the repair skill decides.

```
testing/external-calls.json ──► Facts ───────┐
testing/tests.yaml ───────────► (a map) ─────┼──► Inputs ──► Grouping ──► Orders ──► testing/repair-orders.json
testing/external-systems.calm.json ► SystemsList ┘                  │
                                                         OrderedTest, Reaches,
                                                         Injection, SharedChain
```

| Class | What it does |
|---|---|
| `Inputs` | Reads the three files (Jackson 3, for JSON and YAML); a missing file or one that can't be parsed fails the run |
| `Facts` | under-test-external-calls' facts: the tests that may reach a connection, the systems their reach points make, the tests that start a framework, and whether a system's address is written in the main sources |
| `SystemsList` | The external systems list: its nodes, and the relationship from the application to each, which holds the chosen stand-in and its state |
| `Grouping` | Sorts `tests.yaml`'s entries graded `reaches` or `may-reach` into the tests each node's order holds, and those left out with why (`no-node`, `disabled`, `may-reach`, `paired`, `missed-by-discovery`). A test is ordered once per kind of connection, under its code-path entry, with the hosts its own code or settings name |
| `Candidate`, `Left` | One entry of `tests.yaml` that may be ordered; one test left out, and why |
| `OrderedTest` | One test's part of an order: who it is, its framework, where the stand-in goes, its chain, the stand-ins it already uses, the evidence, and what it waits for |
| `Reaches` | Chooses the reach in the facts that the test's chain comes from, by preference: to the system, at one of its points, of its kinds |
| `Injection` | Where the stand-in goes (`not-known`, `framework-settings`, `set-up`, `field`, `test-body`, the first that holds), the first call out of the test's own classes, and where that call is made |
| `SharedChain` | The calls every ordered test of a node goes through: the longest common end of their chains outside their own classes |
| `Orders` | The file: its schema, the tool's version, what it was made from, the orders by node, and those not ordered |
| `Fields` | Reads the parsed files without casts or null checks spread through the code |

Every list is sorted (tests, nodes, hosts, reasons), except a call chain, whose order is its meaning, so the same
inputs give the same file whatever order they list things in. Its cases are in
`src/test/resources/repair-orders-repo/`: a shop whose tests reach seven systems, with the known answer in
`expected/repair-orders.json`.

## Tests

* Each sub-command's example repo is copied into a temporary folder, the command is run on the copy, and its output
  is compared with the known answer byte for byte; further tests check one behaviour each.
* Tests that need a variation (inputs in another order, a later version of an input's schema, a missing field) edit
  the copy before the run.
* The java-conventions plugin requires one assertion statement per test and 80% line coverage.

## Build and release

| Task or file | What it does |
|---|---|
| `./gradlew check` | Compiles, runs the tests, and every check of the java-conventions plugin (pinned in `settings.gradle.kts`) |
| `./gradlew allInOneJar` | `build/libs/under-test-tools.jar`: the tool and every library, runnable with `java -jar` |
| `./gradlew releaseFiles` | `build/release/`: the jar and its CycloneDX bill of materials, named for the version |
| `.github/workflows/build.yml` | `check` and `releaseFiles` on each push to main and each pull request |
| `.github/workflows/release.yml` | On a `v<version>` tag: the build workflow, then signs the release files, writes `SHA256SUMS`, and makes the GitHub release |
| `maintainer-tools/release/release.sh` | Checks a release is ready, then pushes its tag |
| `maintainer-tools/release/set-up-signing.sh` | Makes the signing key, or uses one named, sets the two secrets, and writes `KEYS` |
| `.github/workflows/sync-backlog.yml` | Syncs `plan/under-test-tools-backlog.yaml` to the repo's issues, through under-test's shared workflow |
