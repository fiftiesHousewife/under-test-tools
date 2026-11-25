# Decisions

Decisions about under-test-tools: why it exists, how it works, and how this repo works. Made by its maintainers, in
[MADR](https://adr.github.io/madr/) form, one small decision per record. Accepted records are never edited; a change is
a new record that replaces one ([how to write one](../../.github/skills/write-a-decision-record/SKILL.md)). Words follow
under-test's [GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md).

Decisions about the toolkit as a whole, including which steps under-test-tools takes on, are under-test's, in
[its decision records](https://github.com/fiftiesHousewife/under-test/tree/main/docs/decisions); records here link to them.

## Why it exists

| Decision | Status | Who decides |
|---|---|---|
| [under-test-tools does only what a program can decide the same way every time, and the judgement stays in under-test's skills](0001-under-test-tools-does-only-what-a-program-can-decide-the-same-way-every-time-and-the-judgement-stays-in-under-tests-skills.md) | accepted (records what the repo already does) | under-test-tools maintainers |

## How it works

| Decision | Status | Who decides |
|---|---|---|
| [Each sub-command writes the toolkit's files exactly as their schemas in under-test say, changing only the fields its skill owns](0002-each-sub-command-writes-the-toolkits-files-exactly-as-their-schemas-in-under-test-say-changing-only-the-fields-its-skill-owns.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [Every behaviour has a case, in an example repo of known answers or a test of its own, and a fault found on a real repo gets a case before it is fixed](0003-every-behaviour-has-a-case-in-an-example-repo-of-known-answers-or-a-test-of-its-own-and-a-fault-found-on-a-real-repo-gets-a-case-before-it-is-fixed.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [The same inputs give the same files, byte for byte](0004-the-same-inputs-give-the-same-files-byte-for-byte.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [It reads a repo's files and runs none of the repo's code, writing only under the repo's testing folder](0005-it-reads-a-repos-files-and-runs-none-of-the-repos-code-writing-only-under-the-repos-testing-folder.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [The tool is one runnable jar holding its libraries, built with Java 25 and running on Java 17 and later](0006-the-tool-is-one-runnable-jar-holding-its-libraries-built-with-java-25-and-running-on-java-17-and-later.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [PMD runs inside the tool as a library, with under-test's rules](0007-pmd-runs-inside-the-tool-as-a-library-with-under-tests-rules.md) | accepted (records what the repo already does) | under-test-tools maintainers |

## How this repo works

| Decision | Status | Who decides |
|---|---|---|
| [The code follows the java-conventions plugin's standards, applied at a pinned version](0008-the-code-follows-the-java-conventions-plugins-standards-applied-at-a-pinned-version.md) | accepted (records what the repo already does) | under-test-tools maintainers |
| [A release is a signed jar, with its bill of materials and checksums, beside a version tag](0009-a-release-is-a-signed-jar-with-its-bill-of-materials-and-checksums-beside-a-version-tag.md) | proposed | under-test-tools maintainers |
| [This repo's backlog is plan/under-test-tools-backlog.yaml, synced to its issues by under-test's shared workflow after each push](0010-this-repos-backlog-is-plan-under-test-tools-backlog-yaml-synced-to-its-issues-by-under-tests-shared-workflow-after-each-push.md) | proposed | under-test-tools maintainers |
