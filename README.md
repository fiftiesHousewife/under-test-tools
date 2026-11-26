# under-test-tools

[![build](https://img.shields.io/github/actions/workflow/status/fiftiesHousewife/under-test-tools/build.yml?branch=main&label=build)](https://github.com/fiftiesHousewife/under-test-tools/actions/workflows/build.yml)
[![release](https://img.shields.io/github/v/release/fiftiesHousewife/under-test-tools?label=release)](https://github.com/fiftiesHousewife/under-test-tools/releases)
[![runs on: Java 17+](https://img.shields.io/badge/runs%20on-Java%2017%2B-blue)](#running-it)
[![licence: Apache-2.0](https://img.shields.io/badge/licence-Apache--2.0-blue)](LICENSE)
[![decisions](https://img.shields.io/badge/decisions-MADR-informational)](docs/decisions/README.md)

Part of under-test.

**The mechanical steps of [under-test](https://github.com/fiftiesHousewife/under-test)'s journey: the parts a program
can decide the same way every time, one sub-command each, so its AI assistant skills keep only the judgement.**

under-test is a toolkit for getting old code properly tested, one repo at a time, with AI coding assistance. Its skills
were first written as instructions an assistant followed by hand. Where a step is mechanical (listing tests, giving
each hit of a rule to its test, counting, writing a file every field of which follows from others), an assistant is
slower than a program and comes out differently from run to run, so the step moves here
([under-test's decision](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0075-a-skills-mechanical-steps-are-done-by-deterministic-tools-and-the-skill-keeps-only-judgement.md);
[this repo's](docs/decisions/0001-under-test-tools-does-only-what-a-program-can-decide-the-same-way-every-time-and-the-judgement-stays-in-under-tests-skills.md)).

What it promises:

* **It reads a repo's files and runs none of its code.** It writes only under the repo's `testing/` folder, and needs
  no network ([decision 0005](docs/decisions/0005-it-reads-a-repos-files-and-runs-none-of-the-repos-code-writing-only-under-the-repos-testing-folder.md)).
* **The same inputs give the same files, byte for byte**, so a change in a committed file is always a real change
  ([decision 0004](docs/decisions/0004-the-same-inputs-give-the-same-files-byte-for-byte.md)).
* **It writes the toolkit's files exactly as [under-test's schemas](https://github.com/fiftiesHousewife/under-test/tree/main/schema)
  say**, keeping every field another skill owns ([decision 0002](docs/decisions/0002-each-sub-command-writes-the-toolkits-files-exactly-as-their-schemas-in-under-test-say-changing-only-the-fields-its-skill-owns.md)).

| Sub-command | The skill it serves | Reads | Writes |
|---|---|---|---|
| [`meaningless-and-unreliable`](#meaningless-and-unreliable) | find-meaningless-and-unreliable-tests | the repo's test sources and build files | `testing/tests.yaml` (`meaningless`, `unreliable`), `testing/assessment.yaml` (`meaningless_tests`, `unreliable_tests`) |
| [`repair-orders`](#repair-orders) | pair-the-tests-that-reach-external-systems | `testing/external-calls.json`, `testing/tests.yaml`, `testing/external-systems.calm.json` | `testing/repair-orders.json` |

## Running it

It needs Java 17 or later, and nothing else: PMD and every other library are inside the jar.

1. Download `under-test-tools-<version>.jar` from [the releases](https://github.com/fiftiesHousewife/under-test-tools/releases).
   Each release also holds the jar's software bill of materials (`-cyclonedx.json`, every library inside it), a PGP
   signature (`.asc`) of each, and `SHA256SUMS`.
2. Check it:

   ```bash
   sha256sum --check --ignore-missing SHA256SUMS
   gpg --verify under-test-tools-<version>.jar.asc under-test-tools-<version>.jar
   ```

3. Run a sub-command in the repo's root, or name the repo with `--repo`:

   ```bash
   java -jar under-test-tools-<version>.jar --help
   java -jar under-test-tools-<version>.jar <sub-command> --help
   ```

Usually a skill of under-test's runs it, at the step of the journey it belongs to, and says what to do with what it
writes. Each sub-command logs one line saying what it did.

| Exit code | Meaning |
|---|---|
| 0 | Done (or `--help`) |
| 1 | An input is missing, can't be parsed, or lacks what the sub-command needs; the message says which |
| 2 | A wrong option, or no sub-command; the usage follows |

## meaningless-and-unreliable

The find-meaningless-and-unreliable-tests skill's mechanical steps. It keeps two lists of a repo's existing tests:

* **meaningless tests** pass whatever the code does: they never run (`never-runs`, `disabled`), can't fail
  (`no-real-assertion`, `swallows-failures`), or check only their mocks (`mocks-everything`);
* **unreliable tests** may give a different answer on another run: they sleep (`sleeps`), or depend on the order tests
  run in (`order-dependent`).

For each chosen module it:

1. lists the tests: methods marked `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory` or `@TestTemplate`;
   JUnit 3 tests in classes extending `TestCase` or Spring's JUnit 3 base classes, through superclasses in the repo;
   and Cucumber scenarios and example rows;
2. runs PMD with under-test's [rules for meaningless and unreliable tests](https://github.com/fiftiesHousewife/under-test/blob/main/tools/meaningless-and-unreliable-tests/rules.pmd.xml)
   in this process (nothing to install);
3. gives each hit to its test: the method holding it, every test of its class for a hit on the class, every test of
   the class for a sleep in a set-up method; drops a detached-test hit in a JUnit 3 class, and a catch in the
   expected-exception idiom (a `try` ending in `fail(...)`);
4. adds what PMD can't see: tests in a folder the build doesn't run, tests in a class Maven's test runner doesn't pick
   up (by its `includes`, or `Test*`, `*Test`, `*Tests`, `*TestCase`, and `*IT` with Failsafe), and disabled
   scenarios;
5. writes each test found to `testing/tests.yaml`, setting only its `meaningless` and `unreliable` lists, keeping the
   `other-...` kinds the skill decides by reading and every other skill's fields;
6. works the `meaningless_tests` and `unreliable_tests` sections of `testing/assessment.yaml` out from that list, and
   proposes a decision for each finding, keyed `meaningless/<kind>` or `unreliable/<kind>`.

```bash
java -jar under-test-tools.jar meaningless-and-unreliable --repo <repo> [--module <folder>]... [--rules <rules.pmd.xml>]
java -jar under-test-tools.jar meaningless-and-unreliable --repo <repo> --summary-only   # after the skill adds its own kinds
```

| Option | Default | |
|---|---|---|
| `--repo <dir>` | the current folder | The repo |
| `--module <folder>` | the whole repo, as one module | A module to cover, from the repo's root; give it once per module |
| `--rules <file>` | the copy built in | under-test's `rules.pmd.xml` |
| `--version <version>` | `unreleased` | The toolkit's version, written in `found_by` |
| `--commit <commit>` | `git rev-parse --short HEAD` | The repo's commit, written in `found_by` |
| `--testing <folder>` | `testing` | Where `tests.yaml` and `assessment.yaml` are |
| `--summary-only` | | Only work the summary and decisions out again from `testing/tests.yaml`, after the skill has added its own kinds |

On under-test's eval example repo it gives the eval's known count for every kind it decides. On Openfire 4.6.0's
`xmppserver` module (415 tests) it takes about two seconds, and found, among others, four tests Maven never runs
because their class's name doesn't end in `Test`.

## repair-orders

The repair skill's mechanical step
([under-test's decision](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0095-repair-is-directed-by-a-work-order-per-external-system-that-under-test-tools-writes.md)).
It joins `testing/external-calls.json` (under-test-external-calls' facts), `testing/tests.yaml` and
`testing/external-systems.calm.json` into `testing/repair-orders.json`, as
[its schema](https://github.com/fiftiesHousewife/under-test/blob/main/schema/repair-orders.schema.json) says:

* an order for each external system with a test graded `reaches` that isn't disabled and whose `repair` is absent or
  `waiting`, giving the system's stand-in, where its address comes from, and the chain its tests share;
* for each test in an order, once per kind of connection: where the stand-in goes (`framework-settings`, `field`,
  `set-up`, `test-body`, or `not-known` when the facts have no chain for it), the first call into other code and the
  type to replace, its chain, the stand-ins it already uses, the evidence that it reaches the system with the hosts
  its own code names, and what it waits for (`stand-in-not-chosen`, `address-in-code`);
* every other test that reaches or called out, in `not_ordered`, with why: `may-reach`, `missed-by-discovery`,
  `paired`, `no-node` or `disabled`.

It only joins and copies. The same inputs give the same file, byte for byte. On jsoup 1.15.3 it gives one order, for
the HTTP client, holding 45 tests, each with the hosts its test names.

```bash
java -jar under-test-tools.jar repair-orders --repo <repo> [--testing <folder>]
```

| Option | Default | |
|---|---|---|
| `--repo <dir>` | the current folder | The repo |
| `--testing <folder>` | `testing` | Where the three inputs are, and `repair-orders.json` goes |

It fails, saying why, when an input is missing or can't be parsed, when `testing/tests.yaml` has no
`found_by.draft-the-external-systems-list` (the external systems list hasn't been drafted), or when a test reaches a
node the application has no relationship to in the external systems list. It logs
`<n> orders holding <n> tests; <n> left out of any order.`

## Building it

```bash
./gradlew check          # the tests, and the java-conventions plugin's code standards
./gradlew allInOneJar    # build/libs/under-test-tools.jar
./gradlew releaseFiles   # build/release: the jar and its bill of materials, named for the version
```

It builds with Java 25 (Gradle fetches it if none is installed) and runs on Java 17 and later
([decision 0006](docs/decisions/0006-the-tool-is-one-runnable-jar-holding-its-libraries-built-with-java-25-and-running-on-java-17-and-later.md)).
The code follows the [java-conventions plugin](https://github.com/fiftiesHousewife/under-test-java-conventions#readme)'s
standards, and `check` fails on any breach of them
([decision 0008](docs/decisions/0008-the-code-follows-the-java-conventions-plugins-standards-applied-at-a-pinned-version.md)).

## Working on it

* [The design](docs/design.md): how the tool is put together, and how each sub-command does its work.
* [AGENTS.md](AGENTS.md): where things are, and the rules every change follows. AI assistants read it first.
* [Decisions](docs/decisions/README.md): why it is the way it is. The toolkit-wide ones are
  [under-test's](https://github.com/fiftiesHousewife/under-test/tree/main/docs/decisions).
* Skills for assistants working here, in [.github/skills](.github/skills): add-a-sub-command, work-on-an-item and
  write-a-decision-record. `apm install` adds the skills they use.
* [The backlog](docs/BACKLOG.md), from [plan/under-test-tools-backlog.yaml](plan/under-test-tools-backlog.yaml). A
  new sub-command's item is in [under-test's backlog](https://github.com/fiftiesHousewife/under-test/blob/main/docs/BACKLOG.md),
  beside its skill.

## Releasing it

1. Put the version in `gradle.properties`, and write what's new for users in `docs/releases/<version>.md`. Push both.
2. Run `maintainer-tools/release/release.sh <version>`. It checks both, then tags main `v<version>` and pushes the tag.
3. The [release workflow](.github/workflows/release.yml) runs every check, then makes the GitHub release: the jar, its
   bill of materials, their PGP signatures and checksums. It needs the repository secrets `SIGNING_KEY` (an
   ASCII-armoured private key) and `SIGNING_KEY_PASSWORD`.

## Licence

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
