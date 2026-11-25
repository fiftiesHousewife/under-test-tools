---
status: accepted
decision-makers: under-test-tools maintainers
group: How this repo works
---

# The code follows the java-conventions plugin's standards, applied at a pinned version

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

under-test-tools held its own copy of the Gradle conventions and quality rules in `build-logic/`, as other repos of ours did, and the copies drifted.

## Considered Options

* **Apply the java-conventions plugin at a pinned version, and keep only this repo's own SpotBugs exclusions**  
  One set of standards across our repos; a new version is adopted deliberately. Its rules can't be loosened here.
* **Keep a copy of the rules here**  
  Free to differ, but it drifts from the other repos.

## Decision Outcome

Chosen: `settings.gradle.kts` pins `io.github.fiftieshousewife.java-conventions`, and `./gradlew check` fails on any breach of its standards (its README lists them). This repo keeps only `config/spotbugs-exclude.xml`, named by `underTest.spotbugsExcludeFilter`. `build-logic/` is deleted.

### Consequences

* Moving to a new version of the plugin is its own change, fixing whatever its rules then find.
* CI runs `./gradlew check` on every push and pull request.

## More Information

* Builds on: [All our Java applies the java-conventions plugin, which holds our code-quality standards](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0100-all-our-java-applies-the-java-conventions-plugin-which-holds-our-code-quality-standards.md) (under-test)
* Later decisions that build on this: none yet
