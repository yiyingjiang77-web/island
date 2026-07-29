# Issue tracker: Local Markdown

Issues and specs for this repository live as Markdown files under `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The tracker spec is `.scratch/<feature-slug>/spec.md`
- Implementation tickets are separate files under `.scratch/<feature-slug>/issues/`
- Ticket filenames begin with a two-digit dependency-order number
- `Status:` records the triage state using `docs/agents/triage-labels.md`
- `Blocked by:` records ticket numbers that must be resolved first
- Comments and history are appended under `## Comments`

## Skill operations

- When a skill says **publish to the issue tracker**, create or update the relevant Markdown file under `.scratch/`.
- When a skill says **fetch the relevant ticket**, read the referenced local ticket file.
- A ticket is on the frontier when its status is `ready-for-agent` and all tickets listed under `Blocked by:` are resolved.
- Claim a ticket by changing its status to `claimed`.
- Resolve a ticket by changing its status to `resolved` and recording the result under `## Comments`.

## Existing GitHub PRD

GitHub Issue #1 remains a published copy of the Demo2.5 PRD, but local Markdown is the active tracker for subsequent engineering skills.
