---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# The tool is one runnable jar holding its libraries, built with Java 25 and running on Java 17 and later

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

The tool runs on organisations' machines and build agents, which often have an older JDK and no access to public repositories, and are mirrored file by file.

## Considered Options

* **One jar with its libraries inside, compiled for Java 17**  
  One file to mirror and approve; `java -jar` is all it needs. The jar is large (about 19 MB, mostly PMD).
* **A library on Maven Central, run through a build plugin**  
  Smaller, but every repo's build has to change, and its dependencies must be resolved there.
* **A native executable**  
  No JDK needed, but a build per platform, and PMD's rules load classes at run time.

## Decision Outcome

Chosen: `./gradlew allInOneJar` builds one runnable jar holding the tool and every library it uses. It is compiled with Java 25 (the java-conventions plugin's toolchain) for Java 17 (`options.release = 17`), so it runs on Java 17 and later.

### Consequences

* The jar names its version in its manifest (`Implementation-Version`); `repair-orders` stamps it in its file as
  `tool_version`.
* Code can't use a JDK API newer than Java 17.
* A release publishes this jar with its bill of materials.

## More Information

* Builds on: [The journey's mechanical steps are one command-line tool with a sub-command per step, in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0077-the-journeys-mechanical-steps-are-one-command-line-tool-with-a-sub-command-per-step-in-its-own-repo.md) (under-test)
* Later decisions that build on this: none yet
