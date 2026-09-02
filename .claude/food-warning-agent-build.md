# Food Warning Agent — How It Works

Companion to [`food-warning-agent.md`](food-warning-agent.md), which is the design.
This document is the **as-built**: what exists in the codebase, how a run
actually proceeds, and what is still missing.

Written 2026-08-12. Status: **built and tested, never run against the live API.**

---

## 1. What exists

| Layer | Files |
|---|---|
| Schema | `db/changelog/changes/021-create-food-warning.yaml` — `food_warning`, `food_warning_assessment` |
| Enums | `WarningCategoryEnum`, `WarningSeverityEnum`, `WarningStatusEnum`, `AssessmentOutcomeEnum` |
| Domain | `FoodWarning`, `FoodWarningAssessment` |
| Entities | `FoodWarningDb`, `FoodWarningAssessmentDb` |
| Mappers | `FoodWarningMapper`, `FoodWarningAssessmentMapper` |
| Repositories | `FoodWarningRepository`, `FoodWarningAssessmentRepository` |
| DbServices | `FoodWarningDbService`, `FoodWarningAssessmentDbService` |
| Agent | `service/agent/` — `AnthropicClient`, `FoodTriageService`, `TriageResult`, `ResearchTools`, `FoodResearchAgent`, `ResearchResult`, `FoodWarningAgentService` |
| Tests | `FoodTriageServiceTest`, `FoodResearchAgentTest` |

Test suite: **152 tests, 0 failures** (was 125 before this work).
Both tables are applied to the local `cpss` database.

## 2. A run, step by step

### Setup — the work queue

`FoodWarningAgentService.runSweep()` asks the database which foods have never
been assessed:

```sql
SELECT f.id FROM FoodDb f
WHERE f.id NOT IN (SELECT a.foodId FROM FoodWarningAssessmentDb a)
```

First run: all 166. Next run: zero, or whatever was added since.

This is why abstentions get their own row. "We checked cucumber and it was fine"
has to be a **stored fact** — otherwise a re-run cannot distinguish it from "we
never checked cucumber."

The service then loops one food at a time, each in its own try/catch. A food that
throws is recorded as `RESEARCH_FAILED` and the run continues. The unit of work
is a food, not the batch.

### Step 1 — Triage (cheap, no tools)

`FoodTriageService` renders the food from catalog data already in hand — name,
category, subcategory, description, notes, typical serving, per-100g nutrition.
No lookups happen here.

The system prompt's load-bearing line:

> Most foods need NO warning. Lettuce, cucumber, bell pepper, olive oil,
> and the like are unremarkable — say so and move on.

Returns JSON: `needs_research`, `suspected_categories`, `reasoning`.

**For romaine this returns false.** An assessment row is written with outcome
`NO_WARNING` and the food is done. Expected to be ~80% of the catalog, at one
cheap call each.

Two parsing defenses, both tested:

- Code fences get stripped — models wrap JSON in ``` despite instructions.
- A `needs_research: true` with **no recognizable category** is downgraded to an
  abstention. A flag with nothing to search for would burn budget on an
  unfocused search.

### Step 2 — The research loop (the ~20%)

Triage flags spinach with `["oxalate", "vitamin_k"]`. `FoodResearchAgent.research()`
runs. **This is the only genuinely agentic step in the system.**

The opening message includes:

> Triage suspected these concerns: oxalate, vitamin_k
> These are suspicions, not conclusions. Confirm or withdraw them.

Then `while (turnsUsed < maxTurns)`:

1. Send the whole conversation so far, with four tools available.
2. If the response contains `submit_verdict` → exit immediately.
3. Otherwise execute the requested tools, append results as a user message, loop.

**Tools:**

| Tool | Runs where | Purpose |
|---|---|---|
| `web_search` | Anthropic server-side | Research. Consumes budget; needs no local result block. |
| `get_existing_warnings` | CPSS | Approved warnings in a category, so phrasing matches. |
| `get_food` | CPSS | Look up another catalog food for comparison. |
| `submit_verdict` | — | The only exit that produces a decision. |

The defining property: **nothing in the code decides how many steps a food
takes.** The model asks for a tool, sees the result, decides what to ask next. A
food whose first source is authoritative costs one turn. Grapefruit — where the
interaction is real but drug-specific — shapes its second query from what the
first returned.

`get_existing_warnings` matters more than it looks. Without it you get nine
differently-worded oxalate notes; with it the catalog reads as one voice.

`submit_verdict` is a **tool, not parsed prose**. The schema forces every field
to be supplied, rather than regexing a verdict out of free text.

### Step 3 — The verify gate

`parseVerdict()` decides whether a claimed verdict is storable. This is the most
heavily tested code in the feature, because it is where a bad claim would reach
the review queue looking legitimate.

| Condition | Outcome |
|---|---|
| `needs_warning: false` | `WITHDRAWN` — **a success**, the loop disproved its own suspicion |
| Missing category / severity / text | `RESEARCH_FAILED` |
| No sources, or `"[]"` | `RESEARCH_FAILED` — unsourced health claims are rejected regardless of stated confidence |
| All present | `WARNING_DRAFTED` |

Also normalized: the literal string `"null"` for `applies_to`, which would
otherwise render to a user as "applies to: null."

### Step 4 — Storage

Every food gets an assessment row: outcome, reasoning, model name, and
`tool_calls_used` / `turns_used`. The last two exist so budget tuning is driven
by data rather than guesswork.

Foods with a drafted warning also get a `food_warning` row. Critically,
`FoodWarningDbService.create()` **hard-sets `status = PENDING`** regardless of
what the caller passes:

```java
warning.setStatus(WarningStatusEnum.PENDING);
```

The agent has no code path to publish. Approval is a separate method
(`review()`) that a human triggers.

## 3. Exit conditions and why they are separate

| Outcome | Meaning |
|---|---|
| `NO_WARNING` | Triage abstained — never entered the loop |
| `WITHDRAWN` | Loop ran, concern did not hold up |
| `WARNING_DRAFTED` | Sourced warning, now pending review |
| `BUDGET_EXHAUSTED` | Hit 6 tool calls or 4 turns without deciding |
| `RESEARCH_FAILED` | Malformed verdict, unsourced claim, or tool/API error |

`WITHDRAWN` and `BUDGET_EXHAUSTED` both produce no warning but mean opposite
things: the first says the system is working, the second says that food needs a
human or a bigger allowance. Collapsing them would hide the signal that tells
you whether 6 is the right number.

## 4. What is actually agentic

Being precise, because the distinction is the point of the exercise:

- **Triage** — a classifier. One call, fixed output, no tools.
- **The research loop** — agentic. Trajectory not knowable in advance.
- **The verify gate** — validation logic.
- **Storage** — CRUD.

One genuinely open-ended step, gated so it only runs where it earns its cost. On
most of the catalog the loop never executes at all.

The loop's value concentrates on the hard tail — foods where evidence is
contested, dose-dependent, or drug-specific. That tail is a small fraction of
166 items. This is not a claim that the whole run is agentic.

## 5. Configuration

```properties
app.agent.anthropic.api-key      # falls back to ANTHROPIC_API_KEY env var
app.agent.anthropic.model        # default: claude-sonnet-4-5
app.agent.anthropic.max-tokens   # default: 2048
app.agent.research.max-tool-calls # default: 6
app.agent.research.max-turns      # default: 4
```

`AnthropicClient` retries 3× with linear backoff and returns null on final
failure — callers treat null as `RESEARCH_FAILED` rather than throwing.

## 6. Design notes worth keeping

**`AnthropicClient` is deliberately dumb.** It sends a message array and returns
the body. Turn management, tool dispatch, and budget enforcement live in
`FoodResearchAgent`. That split is what lets the loop's decision logic be tested
with no API key — `new AnthropicClient("", "test-model", 1024)` is used for its
Jackson helpers only.

**`DatabaseFailureException` exists in this codebase** (`database.db.exceptions`),
contradicting `database-restapi-template`'s claim that it does not. The living
code won, per the skill's own instruction to prefer a working example over the
document.

**The `decimal(3,2)` YAML trap.** Inside an inline flow map, YAML splits
`type: decimal(3,2)` at the comma, producing a `ColumnConfig` parse error that
surfaces as 33 unrelated Spring context failures. It must be quoted:
`type: "decimal(3,2)"`. CPSS had no prior decimal column, so there was no
precedent to copy. The changeset now carries a comment.

## 7. Not built

- **Nothing has called the live API.** Tests stub it. `ANTHROPIC_API_KEY` is not
  in `.env`, and there is no CLI runner or endpoint to invoke a sweep.
- Review UI — listing pending warnings, approve/reject/edit.
- `ResponseFood` wiring — approved warnings are queryable but not served.
- Per-salad aggregation (§12 of the design) — the deterministic layer that sums
  flagged ingredients across a bowl. This is the part that actually protects a
  visitor.
- Digest email via `EmailService`.
- `@EnableScheduling` — not enabled anywhere in this project yet.
- Re-review on food edits: if a food's description changes, its approved warning
  goes stale with no mechanism to catch it.

## 8. The immediate next step

**Triage dry run over all 166 foods.** `FoodWarningAgentService.triageDryRun()`
exists and writes nothing. It needs an API key and a way to invoke it.

This is the go/no-go gate. If triage flags 100+ foods, the rubric is wrong and
the loop underneath it does not matter yet. Read the abstention list before
building anything downstream.

---

## Appendix — the disclaimer that should ship first

Independent of this agent: the app is publicly reachable with open registration,
and displays sugar content with no context. A standing disclaimer line on the
nutrition and salad-builder pages — informational, not medical advice — closes
most of the real exposure and is an afternoon's work.

That should not wait on the agent.
