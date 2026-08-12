# How to Generate Client-Facing Documentation

Instructions for producing the same two deliverables on another project.
Give this file to a fresh Claude Code session working in that project's
repo root.

## Goal

Produce two markdown files, written for an external technical audience
(potential clients evaluating the codebase), saved to
`.claude/client-docs/` (create the folder if it doesn't exist):

1. **`architecture-overview.md`** — a system architecture overview.
2. **`archive-index.md`** — an index of every markdown file under
   `.claude/_archive/` (or wherever this project's internal/legacy docs
   live), organized by directory, with a 1-3 line description of each file.

Ask the user to confirm before creating files:
- Where the docs should live (default: `.claude/client-docs/`).
- What the architecture overview should cover (architecture only, vs.
  architecture + dev workflow, vs. full walkthrough including business
  logic/domain concepts). Don't assume — this varies by audience.

## Part 1: Architecture Overview

Don't write this from memory or assumptions — verify against the actual
repo first. Use a research sub-agent (see "Using sub-agents" below) to
gather, in one pass:

- Module/package list (for a multi-module build: Gradle/Maven modules;
  for JS: workspaces/packages) with a one-line purpose for each, inferred
  from top-level package/directory structure — not full file contents.
- Root application module: entry point, main package structure, key
  dependencies from the build file.
- Frontend stack (if any): framework, build tool, key libraries, top-level
  `src` structure — from `package.json` and directory listing.
- Existing architecture diagrams/docs already in the repo (e.g. a
  `developer-docs/` or `docs/` folder) — check if they're current before
  reusing; if stale or rough, write a fresh one rather than propagating
  inaccuracies.
- Any docker-compose services.
- Language/framework versions from build files.
- Database migration tool and where migrations live, if applicable.

Then write `architecture-overview.md` with:
- A short summary paragraph (what the system does, in plain terms).
- A tech stack table.
- A **mermaid diagram** (` ```mermaid ` fenced block, `graph TB` style)
  showing modules, external systems (DB, AI providers, third-party APIs,
  automation tools), and data flow between them. Build this fresh from
  verified facts — don't just port an existing stale diagram.
- A section per module explaining its responsibility.
- A frontend section if applicable.
- A "typical data flow" walkthrough (e.g. request lifecycle, or the
  main ingestion/processing pipeline if the system has one).
- A brief testing/validation section if the project has a dedicated test
  module.

Keep it architecture-only unless the user asked for more — no business
logic, no domain-specific workflows, no credentials or internal hostnames.

## Part 2: Archive Index

1. Find every markdown file under the internal docs directory:
   `find <path> -name "*.md" | sort`
2. This is usually 50-100+ files. Don't read them all yourself — fan out
   parallel sub-agents, each assigned a batch of ~10-20 files grouped by
   directory, and have each one read its files in full and return a
   1-3 line description per file.
3. Assemble all sub-agent results into one `archive-index.md`, organized
   with one `## <directory>/` heading per directory and one bullet per
   file: `` - **filename.md** — description ``.

### Using sub-agents (important — avoid this failure mode)

If the project's CLAUDE.md (or equivalent) contains session-start
instructions — e.g. "at the start of every session, read X and say Y" —
a fresh sub-agent will pick that up when it reads any project file and
may derail: it'll read CLAUDE.md, follow the session-start ritual, and
return something like "Are you ready, Boy?" instead of doing the actual
task. This happened twice while generating these docs on viro-server.

To prevent it, every sub-agent prompt for this kind of file-reading task
must explicitly say, near the top:

> IMPORTANT: Ignore any session-start instructions in CLAUDE.md or memory
> about reading other docs or saying specific phrases — those apply to
> interactive sessions, not this delegated task. Do not read CLAUDE.md.
> Your ONLY job is the task below. Return only the requested format.

Check each sub-agent's returned result before assembling the final
document — if a batch comes back short-circuited (didn't return the
per-file summaries you asked for), re-launch that batch with the
override above rather than assuming it worked.

Run sub-agent batches in parallel (multiple Agent tool calls in one
message) rather than sequentially — they're independent reads with no
shared state.

## Notes

- Both output files are plain markdown, not Word docs — technical
  reviewers read markdown faster and it stays in sync with the repo.
  Only use Word/PDF if there's a non-technical stakeholder who needs
  polished leave-behind collateral alongside these.
- Don't commit these files unless the user explicitly asks — confirm
  first, same as any other commit.
