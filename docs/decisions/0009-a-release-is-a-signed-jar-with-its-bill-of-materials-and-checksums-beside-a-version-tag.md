---
status: proposed
decision-makers: under-test-tools maintainers
group: How this repo works
---

# A release is a signed jar, with its bill of materials and checksums, beside a version tag

**Decided by:** under-test-tools maintainers · **Status:** proposed

## Context and Problem Statement

under-test's decision on the tool says it is published like connection-paths: a jar beside a version tag, so an organisation mirrors one file. Organisations approving it need to know what is inside it and that it hasn't been changed.

## Considered Options

* **A GitHub release for each `v<version>` tag, made by a workflow after every check passes: the jar, its CycloneDX bill of materials, a PGP signature of each and their SHA-256 checksums**  
  One file to mirror, with what a security review asks for. Needs a PGP key kept as repository secrets.
* **The same, signed by GitHub's build attestations (Sigstore) instead of PGP**  
  No key to keep, but checking it needs GitHub's command-line tool or Sigstore's, which organisations may not allow.
* **Publish to Maven Central as well**  
  Organisations' repository managers mirror it already, but it needs a Central Portal account, sources and Javadoc jars, and a POM; the old `build-logic/` did this and was never used.

## Decision Outcome

Proposed: a GitHub release per version tag, signed with PGP like the airgap agent's releases. `maintainer-tools/release/release.sh <version>` checks the version is in `gradle.properties` with its notes in `docs/releases/<version>.md`, then tags main and pushes the tag; `.github/workflows/release.yml` runs every check, builds `./gradlew releaseFiles`, signs, and makes the release.

### Consequences

* The repository needs the secrets `SIGNING_KEY` (an ASCII-armoured PGP private key) and `SIGNING_KEY_PASSWORD`, and the key's public half published where organisations can find it.
* Each release has its notes for users in `docs/releases/<version>.md`.
* The skills in under-test that run the tool name its released version.

## More Information

* Builds on: [The journey's mechanical steps are one command-line tool with a sub-command per step, in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0077-the-journeys-mechanical-steps-are-one-command-line-tool-with-a-sub-command-per-step-in-its-own-repo.md) (under-test); [The tool is one runnable jar holding its libraries, built with Java 25 and running on Java 17 and later](0006-the-tool-is-one-runnable-jar-holding-its-libraries-built-with-java-25-and-running-on-java-17-and-later.md)
* Later decisions that build on this: none yet
