---
status: accepted
decision-makers: under-test-tools maintainers
group: How it works
---

# Each sub-command writes the toolkit's files exactly as their schemas in under-test say, changing only the fields its skill owns

**Decided by:** under-test-tools maintainers · **Status:** accepted (records what the repo already does)

## Context and Problem Statement

The files a sub-command writes (`testing/tests.yaml`, `testing/assessment.yaml`, `testing/repair-orders.json`) are read by other skills and other tools. Their schemas live in under-test, beside the skills. Several skills write different fields of the same file.

## Considered Options

* **under-test's schemas are the authority; a sub-command writes exactly what they say and keeps every field it doesn't own**  
  Files written by the tool and by hand look the same; other skills' work survives a run. A schema change in under-test needs a matching change here.
* **This repo keeps its own copy of each schema**  
  No dependency on under-test at build time, but the copies drift.

## Decision Outcome

Chosen: under-test's schemas are the authority. A sub-command writes a file exactly as its schema says, sets only the fields its skill owns, and keeps every other field as it found it. A file it makes whole, such as `testing/repair-orders.json`, carries the schema's name and version.

### Consequences

* A change to a schema in under-test is matched here in the same piece of work, and checked with under-test's own validator.
* A sub-command that writes into a file other skills also write has a test that their fields come through a run
  unchanged (`meaningless-and-unreliable`'s, for `testing/tests.yaml`).

## More Information

* Builds on: [The journey's mechanical steps are one command-line tool with a sub-command per step, in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0077-the-journeys-mechanical-steps-are-one-command-line-tool-with-a-sub-command-per-step-in-its-own-repo.md) (under-test); [Repair is directed by a work order per external system that under-test-tools writes](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0095-repair-is-directed-by-a-work-order-per-external-system-that-under-test-tools-writes.md) (under-test)
* Later decisions that build on this: none yet
