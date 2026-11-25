---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# PMD runs inside the tool as a library, with under-test's rules

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

`meaningless-and-unreliable` finds most of its kinds with PMD rules that under-test keeps. Organisations' machines may have no PMD installed, or a different version.

## Considered Options

* **PMD as a library in the jar, run in the same process, with a copy of under-test's rules as the default**  
  Nothing to install; the PMD version is pinned with the tool. The jar is larger, and the copy of the rules must follow under-test's.
* **Run an installed PMD as a separate program**  
  A smaller jar, but its version and presence vary from machine to machine.

## Decision Outcome

Chosen: PMD is a dependency of the tool and runs in its process. `src/main/resources/rules.pmd.xml` is a copy of under-test's rules, used when `--rules` isn't given; under-test's file is the source.

### Consequences

* The PMD version is in `gradle/libs.versions.toml`, kept the same as the java-conventions plugin's where it can be.
* A change to under-test's rules is copied here, with its case in the example repo.

## More Information

* Builds on: [The tool is one runnable jar holding its libraries, built with Java 25 and running on Java 17 and later](0006-the-tool-is-one-runnable-jar-holding-its-libraries-built-with-java-25-and-running-on-java-17-and-later.md)
* Later decisions that build on this: none yet
