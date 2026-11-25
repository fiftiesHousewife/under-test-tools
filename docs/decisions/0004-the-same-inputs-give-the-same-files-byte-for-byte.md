---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# The same inputs give the same files, byte for byte

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

The files are committed to the repos the journey works on, and compared between runs and between machines. A file that changes when nothing it is made from changed hides real changes in the noise.

## Considered Options

* **Deterministic output: everything sorted, nothing from the clock or the machine**  
  Reruns give no diff; a diff always means a real change. Every list needs a stated order.
* **Output in whatever order the inputs gave**  
  Simpler code, but reordering an input changes the file.

## Decision Outcome

Chosen: a sub-command's output depends only on its inputs. Lists are sorted by a stated key (except where order is the meaning, such as a call chain), and nothing comes from the clock, the machine or the order a folder lists its files in. The tool's version is stamped in the file, so a new version may change it.

### Consequences

* `repair-orders` has a test that the inputs in reverse order give the same file.
* The known answers are checked byte for byte.

## More Information

* Builds on: [under-test-tools does only what a program can decide the same way every time, and the judgement stays in under-test's skills](0001-under-test-tools-does-only-what-a-program-can-decide-the-same-way-every-time-and-the-judgement-stays-in-under-tests-skills.md)
* Later decisions that build on this: none yet
