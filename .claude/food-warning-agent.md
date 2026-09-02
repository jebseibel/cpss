# Food Warning Agent — CPSS

An agent that works the food catalog and writes **toxicity and health-interaction
warnings** for the foods that need them, leaving the rest alone. Closes the
stated gap in `PROJECT_DESCRIPTION.md` (known limitation #1: toxicity and
diabetes warnings are a project requirement and are not implemented).

Unlike a classifier-on-a-schedule, this agent decides per food whether it
already knows enough or must go look something up, chooses which sources to
consult, and abstains on the majority of the catalog. The interesting behavior
is what it *doesn't* flag.
## Locked-in decisions

- **Scope**: the existing 166-food catalog, one pass, then re-runs only for new
  or changed foods
- **Storage**: MySQL 8 (the CPSS database), via Liquibase changesets — no new backend
- **Warning target**: `food`, not `nutrition` — warnings are properties of the
  ingredient, not of its macro row
- **Action policy**: **agent never publishes directly.** Every warning it writes
  lands in `status = 'pending'` and requires human confirmation before the API
  serves it. Same manual-review stance as [`salad-abuse-monitor-agent.md`](salad-abuse-monitor-agent.md).
- **Abstention is a first-class outcome**: "no warning needed" is recorded, not
  just an absence of rows
- **Sourcing required**: a warning with no citation is rejected by the writer
  before it reaches the queue
- **Notification**: email digest via the existing `EmailService`
- **Trigger**: on-demand + scheduled sweep for unprocessed foods
- **Failure handling**: per-food, not per-run — one bad food does not stall the queue

---

## 1. Goals

- Attach accurate, sourced warnings to the foods that genuinely warrant them
  (oxalates, vitamin K / anticoagulant interaction, solanine, allergens, high
  glycemic impact, purines, tyramine, raw-consumption risk).
- **Abstain on everything else.** A catalog where 166 of 166 foods carry a scary
  banner is worse than no warnings at all — it trains users to ignore them.
- Never present an unreviewed model claim to a user as medical guidance.
- Leave an audit trail: what was checked, what was decided, on what basis.

## 2. What makes the research step an agent (and what doesn't)

Most of this system is a process, and it is worth saying so plainly rather than
claiming otherwise.

**Not agentic:**

- **Triage** is a classifier. One call, fixed output shape, no tools. The
  `needs_research` boolean is a branch, and a shell script with an `if` has
  branches too. Calling it a decision does not make it agency.
- **The glycemic branch** is arithmetic over `sugar` × `typicalServingGrams`.
  No model needs to be involved at all.
- **The per-salad aggregation** (see §12) is a pure function — sum the flagged
  ingredients, compare to a threshold. A feature, not an agent.

**Agentic:** the research step, and only if it is built as a real loop. The
distinguishing property is that **the trajectory cannot be drawn in advance.**

| | Fixed pipeline | Bounded tool loop |
|---|---|---|
| Step count | known before running | depends on what is found |
| Which tool, when | hardcoded per stage | model chooses each turn |
| Contradictory evidence | returns an error | revises the draft, or withdraws |
| Termination | last stage completes | model judges it has enough, or budget runs out |

Concretely: a food whose first source is authoritative and unambiguous costs one
lookup. Grapefruit does not — the interaction is real but drug-specific, so the
second search is chosen based on what the first turned up, and the third based
on the second. Nothing in the design knows in advance that grapefruit takes four
steps and romaine takes zero.

The other genuinely agentic property is **withdrawal**: the loop can end with
`needs_warning: false` after starting from a suspicion that looked solid. The
output can be the opposite of what the trajectory started toward. A pipeline
that always emits a warning once it enters the research stage has no such
property.

**Honest scope of the demo.** 166 foods, most needing zero or one lookup, is
process-shaped work. The loop earns its keep on the hard tail — the dozen or so
foods where the evidence is contested, dose-dependent, or drug-specific. That
tail is where the agentic behavior is actually visible, and it is a small
fraction of the catalog. This document does not claim the whole run is agentic;
it claims the tail is, and the tail is the part worth demonstrating.

## 3. Architecture

Two loops: an outer work queue (a process) and an inner research loop (the
agent).

```
OUTER — work queue, deterministic
Trigger (on-demand or scheduled sweep)
  → select foods with no current assessment
  → for each food (independent, retryable, parallelizable):
        1. TRIAGE — cheap classifier, no tools
        2. if no  → record abstention, done  (~80% exit here)
        3. if yes → hand to the research loop
        4. VERIFY — reject if unsourced, vague, or restates the obvious
        5. write to `food_warning` with status='pending'
  → email digest of what awaits review
  → human confirms/edits/rejects → status='approved' → API serves it
```

```
INNER — research loop, agentic, one food
  given: food + suspected categories from triage
  tools: web_search, fetch_page, get_food(name), get_existing_warnings(category)
  budget: max 6 tool calls, max 4 turns

  loop:
    model inspects what it has so far
    → calls a tool of its choosing, OR
    → emits a verdict and stops

  stop conditions:
    - model emits needs_warning true/false with sources        (normal)
    - budget exhausted → record outcome 'research_failed'       (no partial warning)
    - contradictory sources → model may revise or withdraw      (not an error)
```

The budget is the safety rail that makes an open-ended loop safe to run
unattended over 166 items: worst case per food is bounded, and exhaustion is a
recorded outcome rather than a crash or a half-written warning.

`get_existing_warnings(category)` matters more than it looks — it lets the model
see how the same concern was phrased for other foods, so the catalog ends up
internally consistent instead of nine differently-worded oxalate notes.

Triage gating the loop is the efficiency story: expect roughly 30 of 166 foods
to reach the research step at all.

## 4. Data available per food

Already in the catalog — no new ingestion needed:

- `name`, `category`, `subcategory`, `description`, `notes`
- `foundation`, `mixable`, `typicalServingGrams`
- flavor: `crunch`, `punch`, `sweet`, `savory`
- nutrition per 100g: `carbohydrate`, `fat`, `protein`, `sugar`, `fiber`,
  `vitaminD`, `vitaminE`

`sugar` and `typicalServingGrams` together drive the diabetes-facing warnings
without any external lookup — that branch is arithmetic, not research.

## 5. Warning categories

| Category | Example foods | Basis |
|---|---|---|
| `oxalate` | spinach, beet greens, almonds | kidney-stone risk at volume |
| `vitamin_k` | kale, spinach, chard | anticoagulant (warfarin) interaction |
| `allergen` | tree nuts, peanuts, sesame, dairy, fish | major declarable allergens |
| `glycemic` | dried fruit, honey, sweetened dressings | sugar per typical serving |
| `purine` | anchovy, sardine, organ meats | gout |
| `tyramine` | aged cheese, cured meat, fermented | MAOI interaction |
| `solanine` | raw potato, green tomato, nightshades | glycoalkaloid |
| `raw_risk` | sprouts, raw egg, unpasteurized cheese | pathogen risk raw |
| `interaction` | grapefruit, pomegranate | CYP3A4 drug metabolism |

Severity is `info` | `caution` | `avoid_if`, where `avoid_if` always names the
population it applies to ("if taking warfarin"), never a blanket instruction.

## 6. Prompts

**Triage** (cheap, no tools, structured output):

```
You are triaging foods in a salad-app catalog to decide whether each one
needs a health warning researched.

Given a food's name, category, description, and per-100g nutrition, decide
whether it plausibly falls into any of these concerns:
oxalate, vitamin_k, allergen, glycemic, purine, tyramine, solanine,
raw_risk, interaction.

Most foods need NO warning. Lettuce, cucumber, bell pepper, olive oil,
and the like are unremarkable — say so and move on.

Respond ONLY as JSON:
{
  "needs_research": true | false,
  "suspected_categories": ["oxalate", ...],
  "reasoning": "one sentence"
}
```

**Research + write** (the inner loop from §3 — tools enabled, multi-turn, runs
only when `needs_research`):

```
You are researching one food in a salad-app catalog to decide whether it
needs a health note, and writing the note if so.

You have tools: web_search, fetch_page, get_food, get_existing_warnings.
Call them as needed. You have a budget of 6 tool calls — spend them where
the answer is genuinely unclear, not to confirm what you already know.

Work until you can answer, then stop. Some foods take one lookup; some
take four because the concern is dose- or drug-specific. If you cannot
reach a sourced conclusion within budget, say so rather than guessing.

Before writing, call get_existing_warnings for your category and match
its phrasing — the catalog should read as one voice.

Rules:
- Cite a source for every factual claim. No citation, no warning.
- Name the affected population, never issue blanket medical advice.
  Good: "High in vitamin K; may interfere with warfarin."
  Bad:  "Consult your doctor before eating."
- Do not restate what a user can already see. "Contains sugar" on a food
  whose sugar content is displayed is not a warning.
- If the concern does not hold up under research, return needs_warning:false.
  Withdrawing a suspicion is a correct outcome, not a failure.
- This app is not a medical device. Notes are informational and are
  reviewed by a human before publication.

Respond ONLY as JSON:
{
  "needs_warning": true | false,
  "category": "...",
  "severity": "info" | "caution" | "avoid_if",
  "applies_to": "who this concerns, or null",
  "text": "one or two sentences, user-facing",
  "sources": [{"title": "...", "url": "..."}],
  "confidence": 0.0-1.0
}
```

## 7. Schema (MySQL, Liquibase — changeset 021)

Follows CPSS conventions: `bigint` surrogate key, `extid` UUID on the wire,
soft-delete columns, `active` flag, FK on numeric `id`.

```yaml
databaseChangeLog:
  - changeSet:
      id: create_food_warning_table
      author: claude
      labels: food_warning
      changes:

        - createTable:
            tableName: food_warning
            columns:
              - column: { name: id, type: bigint, autoIncrement: true, constraints: { primaryKey: true, nullable: false } }
              - column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true }, defaultValueComputed: "(UUID())" }
              - column: { name: food_id, type: bigint, constraints: { nullable: false } }
              - column: { name: category, type: varchar(30), constraints: { nullable: false } }
              - column: { name: severity, type: varchar(20), constraints: { nullable: false } }
              - column: { name: applies_to, type: varchar(200) }
              - column: { name: warning_text, type: varchar(500), constraints: { nullable: false } }
              - column: { name: sources, type: text }
              - column: { name: confidence, type: decimal(3,2) }
              - column: { name: status, type: varchar(20), constraints: { nullable: false }, defaultValue: pending }
              - column: { name: reviewed_by, type: varchar(36) }
              - column: { name: reviewed_at, type: datetime }
              - column: { name: created_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
              - column: { name: updated_at, type: datetime }
              - column: { name: deleted_at, type: datetime }
              - column: { name: active, type: int, defaultValueNumeric: 1 }

        - createIndex:
            indexName: idx_food_warning_food_id
            tableName: food_warning
            columns:
              - column: { name: food_id }

        - createIndex:
            indexName: idx_food_warning_status
            tableName: food_warning
            columns:
              - column: { name: status }

        - addForeignKeyConstraint:
            constraintName: fk_food_warning_food
            baseTableName: food_warning
            baseColumnNames: food_id
            referencedTableName: food
            referencedColumnNames: id
            onDelete: CASCADE
            onUpdate: CASCADE
```

`status`: `pending` | `approved` | `rejected`. **Only `approved` rows are served
by the API.**

A second table records the pass itself, including abstentions — so a re-run
knows what it already looked at, and "we checked cucumber and it was fine" is a
recorded fact rather than an inference from silence:

```yaml
        - createTable:
            tableName: food_warning_assessment
            columns:
              - column: { name: id, type: bigint, autoIncrement: true, constraints: { primaryKey: true, nullable: false } }
              - column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true }, defaultValueComputed: "(UUID())" }
              - column: { name: food_id, type: bigint, constraints: { nullable: false } }
              - column: { name: outcome, type: varchar(20), constraints: { nullable: false } }
              - column: { name: reasoning, type: varchar(500) }
              - column: { name: model, type: varchar(50) }
              - column: { name: assessed_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
              - column: { name: created_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
              - column: { name: updated_at, type: datetime }
              - column: { name: deleted_at, type: datetime }
              - column: { name: active, type: int, defaultValueNumeric: 1 }
```

`outcome`: `no_warning` (triage abstained) | `withdrawn` (research loop
started from a suspicion and disproved it) | `warning_drafted` |
`budget_exhausted` (loop hit its tool-call limit without concluding) |
`research_failed` (tool errors).

`withdrawn` and `budget_exhausted` are separated deliberately: the first is the
loop working correctly, the second is a food that needs a human or a bigger
budget. Collapsing them would hide the distinction that tells you whether the
budget is set right.

Two columns support the loop: `tool_calls_used` (int) and `turns_used` (int),
so budget tuning is driven by data rather than guesswork. Add them to the
`food_warning_assessment` table above.

## 8. Shared infrastructure with the abuse monitor

Both agents want the same four things. Build them once here, and the abuse
monitor becomes mostly configuration:

| Piece | Shared form |
|---|---|
| Agent run state | `agent_run` table: agent name, started/finished, counts, status — replaces the `settings`/`last_checked_at` row in the abuse-monitor doc, and generalizes it |
| Review queue | Same `pending`/`approved`/`rejected` lifecycle, different payload table |
| Digest email | One `sendAgentDigest(agentName, items)` on the existing `EmailService`; send only when non-empty |
| Retry | Per-item retry with backoff; a failed item is recorded and skipped, never blocking the run |

Note this differs from the abuse monitor's stated failure handling on purpose:
that doc holds back `last_checked_at` so a failed run re-processes its whole
window. Here the unit of work is a food, not a time window, so failures are
recorded per food and retried individually. If the two are unified, the
food-warning model is the more general one — a time window is just a query that
selects the work items.

## 9. Serving warnings to the UI

- `ResponseFood` gains a `warnings` array — approved rows only.
- The salad builder aggregates warnings across ingredients, deduplicated by
  category, so a bowl with three high-oxalate greens says so once.
- Per the project's no-modal rule, warnings render inline on the food card and
  as a section on the salad page — not as a popup.
- Add a standing disclaimer line: informational, not medical advice.

## 10. Build order

1. Changeset 021 + `FoodWarning` domain/entity/repository/DbService via the
   `database-restapi-template` skill.
2. Triage prompt + a script that runs it over all 166 foods, output to console.
   Read the abstention list first — if it flags 100+ foods, the rubric is wrong
   and nothing downstream is worth building yet.
3. Research/write step for the foods that pass triage, writing `pending` rows.
4. Review view: list pending, approve/reject/edit. This is the human gate.
5. Serve approved warnings in `ResponseFood` and the salad builder.
6. Digest email + scheduled sweep for new foods (`@EnableScheduling` is not yet
   on in this project — it gets added here).

## 11. Open questions

- **Model choice for triage.** 166 cheap calls; a smaller model may be right,
  but the abstention quality is the whole point and that's where a weaker model
  degrades first. Measure before optimizing.
- **Re-review on food edits.** If a food's description changes, does its
  approved warning go stale? Simplest answer: an edit to name/category/notes
  resets status to `pending`.
- **Aggregate thresholds.** Oxalate risk is dose-dependent — three high-oxalate
  greens in one bowl may warrant a warning none of them individually do. Needs
  the per-food work done first before this is answerable.
- **Diabetes framing.** The stated requirement says "diabetes." Real
  implementation is sugar-per-serving with context, not a diabetes diagnosis
  claim. Keep the warning about the food, never about the user.

---

**Status**: design only. Nothing in this document is built yet.
