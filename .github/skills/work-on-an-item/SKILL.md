---
name: work-on-an-item
description: Pick up a work item from this repo's backlog (plan/under-test-tools-backlog.yaml, synced to its issues) or a named under-test issue for under-test-tools, and finish it. Use when asked to "do the next item", "work the backlog", or to work on a named item.
---

# Working on an item

## 1. Pick an item
Take the item named. Otherwise take the first open item in
[plan/under-test-tools-backlog.yaml](../../../plan/under-test-tools-backlog.yaml) whose `blocked_by` items are all
done, in horizon order (now, next, later). A new sub-command's item is in
[under-test's backlog](https://github.com/fiftiesHousewife/under-test/blob/main/docs/BACKLOG.md), beside its skill.
Read the item's `done_when` list: that is the finish line. If it depends on an open decision, stop and say so.

## 2. Read before writing
- The [decision records](../../../docs/decisions/README.md) it touches, and under-test's records it links to.
- For a sub-command: its schema in under-test (`schema/`), the skill that will run it, and its example repo here.
- [The design](../../../docs/design.md) section it touches.

## 3. Do the work
| `done_by` | What you do | Finish |
|---|---|---|
| **tool** | Build or change a sub-command or the build, with its cases first ([decision 0003](../../../docs/decisions/0003-every-behaviour-has-a-case-in-an-example-repo-of-known-answers-or-a-test-of-its-own-and-a-fault-found-on-a-real-repo-gets-a-case-before-it-is-fixed.md)). Same inputs, same output. | `./gradlew check` passes. |
| **ai** | Draft code, tests or documents for the person to review. | `./gradlew check` passes; the person reviews. |
| **person** | Prepare what the person needs to decide, with options and a recommendation, then ask. | The person decides; record it with the write-a-decision-record skill. |

Never put an AI model inside a sub-command, and never run the code of a repo the tool reads.

## 4. Finish
- Tick off every `done_when` line, and set `status: "done"` on the item, with a short `notes:` saying what was done.
- Keep the docs in step: [README.md](../../../README.md) for what a user sees, [AGENTS.md](../../../AGENTS.md)'s table
  for where things are, [the design](../../../docs/design.md) for how it works.
- Run `./gradlew check`, then commit. The issues and `docs/BACKLOG.md` update in GitHub Actions after the push; pull
  before the next change.
