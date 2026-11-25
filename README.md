# under-test-tools

[![build](https://img.shields.io/github/actions/workflow/status/fiftiesHousewife/under-test-tools/build.yml?branch=main&label=build)](https://github.com/fiftiesHousewife/under-test-tools/actions/workflows/build.yml)
[![runs on: Java 17+](https://img.shields.io/badge/runs%20on-Java%2017%2B-blue)](#building)
[![licence: Apache-2.0](https://img.shields.io/badge/licence-Apache--2.0-blue)](LICENSE)

**The mechanical steps of [under-test](https://github.com/fiftiesHousewife/under-test)'s journey: the parts a program
can decide the same way every time, one sub-command each, so its AI assistant skills keep only the judgement.**

under-test is a toolkit for getting old code properly tested, one repo at a time, with AI coding assistance. Its skills
were first written as instructions an assistant followed by hand. Where a step is mechanical (listing tests, giving
each hit of a rule to its test, counting, writing a file every field of which follows from others), an assistant is
slower than a program and comes out differently from run to run, so the step moves here
([under-test's decision](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0075-a-skills-mechanical-steps-are-done-by-deterministic-tools-and-the-skill-keeps-only-judgement.md)).

It reads a repo's files and writes only under its `testing/` folder. It runs none of the repo's code.

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

## Building

Java 17 or later to run it; `./gradlew check` runs the tests and the code standards, and `./gradlew allInOneJar` builds
`build/libs/under-test-tools.jar`. See [AGENTS.md](AGENTS.md).

## Licence

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
