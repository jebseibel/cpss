# CPSS Skills Reference

Seven skills, checked into this repo under `.claude/skills/`. They encode the repetitive
parts of this codebase — the layered scaffold, its tests, schema changes — so those come
out consistent rather than being retyped from memory each time.

**Verified against the code: 2026-08-12.**

---

## Why these are checked in rather than left at user level

Six of these also exist at `~/.claude/skills/` and one came from a sibling project. Both
sets would already *work* here, so copying them in was a deliberate choice with a specific
payoff and a specific cost.

**The payoff.** The upstream copies are written for a Gradle **multi-module** layout —
separate `common/` and `database/` modules, paths like
`common/src/main/java/{basePackagePath}/common/domain/`. CPSS is **single-module**
(`settings.gradle` declares only `rootProject.name = 'cpss'`), so every layer lives under
one `src/main/java` tree. The upstream skills detect that mismatch and stop to ask for
confirmation on every run. The project copies have those paths rewritten and
`{basePackage}` pinned to `com.seibel.cpss`, so they run without interruption.

Note what did *not* change: the **package** structure is identical in both layouts
(`common.domain`, `web.request`, `database.db.entity`, `service`, `web.controller`). Only
the physical path prefix differs. That the same skills fit both a four-module project and
a single-module one, with only path edits, is itself a fact about how portable the layered
pattern is.

**The cost, stated plainly.** These are now forked copies. A fix made to the upstream
skill at `~/.claude/skills/` will not reach this project, and vice versa. That is a real
maintenance tax and it is accepted deliberately: this repo is a showcase, and tooling that
stops to ask a layout question mid-demo is worse than tooling that has drifted slightly.
If a skill here gains a genuine improvement, port it upstream by hand.

---

## The skills

### Generation — table spec to tested code

These four run in sequence, each picking up where the last stopped. `entity-full-stack`
chains the first three in a single pass.

| Skill | Takes | Produces |
| --- | --- | --- |
| `domain-pojo-from-tables-doc` | A table's column list in a markdown design doc | One Domain POJO under `common/domain/` |
| `database-restapi-template` | An existing Domain POJO | 9 files — Request/Response DTOs, Entity, Mapper, Repository, DbService, Service, Controller+Converter — plus a Liquibase changeset |
| `database-restapi-testing` | An already-scaffolded entity | 3 test files + builder methods appended to `DomainBuilderDatabase` |
| `entity-full-stack` | A table spec | All of the above in one uninterrupted run, with compile/test verification between stages |

**Why they are separate rather than one skill.** Each stage has a genuine decision point
that stalls a naive chain — `domain-pojo-from-tables-doc` always asks whether the entity
extends `BaseDomain` or `BaseUniqueDomain` and never infers it; FK columns are skipped by
default and have to be added back deliberately. `entity-full-stack` exists precisely to
collect all of those answers up front, in one question, so the run doesn't stop three
times.

### Modification

| Skill | Purpose |
| --- | --- |
| `database-column-change` | The only skill that **changes** an existing entity — widen, narrow, rename, add, drop, or change nullability on a column, updating every layer that pins that column's shape |

It is **table-scoped, not column-scoped**, because the expensive parts (a database rebuild
and the test run) are per-table. Batch every change to one table into a single run.

The characteristic bug it exists to prevent: changing only the Liquibase changeset. The
column goes wide in the database while `@Column(length = n)` and the DTOs' `@Size` still
reject the value, which reads as "the schema change didn't work."

### Security

| Skill | Purpose |
| --- | --- |
| `login-rate-limit` | Throttle repeated failed logins per IP+username, returning 429 with `Retry-After` |

**Already applied to this project** — see `src/main/java/com/seibel/cpss/security/LoginRateLimitFilter.java`
and §8a of `DESIGN_DECISIONS.md`. Kept here because the skill carries the reasoning that
the code cannot: in particular why the counter is keyed on IP **and** username rather than
IP alone, which is the difference between a security control and a self-inflicted denial of
service.

### Operations

| Skill | Purpose |
| --- | --- |
| `progress-ticker` | A character-per-record console progress bar for a long-running loop |

**Not currently applicable.** CPSS has no loop long enough to warrant it — the closest is
Liquibase CSV seed loading, which is fast and already logs. Kept for the bulk-import work
that would need it. Do not wire it into a loop that finishes in under a second.

---

## Running them

Three ways, all equivalent:

1. **Plain language** — "run database-restapi-template on Salad"
2. **Slash command** — `/database-restapi-template Salad`
3. **Established trigger phrase** — "follow the restapi template for Salad"

Several targets in one message works and runs sequentially:

> "Run database-restapi-template on Salad and Mixture."

For many targets (5+), ask for a parallel fan-out. A fresh sub-agent has no memory of the
conversation, so the prompt must name the skill file explicitly:

> "Launch one agent per target [list them]. Each agent should read
> `.claude/skills/database-restapi-template/SKILL.md` and run it on its assigned target."

Note the **`.claude/skills/`** path, not `~/.claude/skills/` — a sub-agent given the
user-level path will hit the multi-module layout question and stall.

---

## Things these skills need to know about CPSS

Both are already written into the relevant skill files; repeated here because they explain
failures that look like something else.

**Tests run against a real MySQL instance.** Repository and DbService tests use
`@SpringBootTest` with `@ActiveProfiles("test-database")`, configured in
`src/test/resources/application-test-database.yml`, which reads `RDS_HOSTNAME`, `RDS_PORT`,
`RDS_DB_NAME`, `RDS_USERNAME`, and `RDS_PASSWORD` from the environment. A connection
failure is an environment problem, not a defect in generated code.

**Editing an applied changeset breaks its Liquibase checksum**, and because the tests share
that schema, the failure surfaces as a Spring context-load error across the whole suite —
looking nothing like a column problem. Per `.claude/CLAUDE.md`, "rebuild the database"
means triggering the n8n webhook at `http://localhost:5678/webhook/clear-cpss-db` (a GET).

**New changesets register themselves.** `db.changelog-master.yaml` uses `includeAll`, so a
new numbered file under `src/main/resources/db/changelog/changes/` is picked up without a
master-file edit.

---

## Gap worth filling

There is no skill for **data fetching → normalization → persistence**. The skills above
take a schema design to tested CRUD and stop at an empty database; loading data into those
tables is just as repetitive and isn't captured. CPSS does this with Liquibase CSV seed
files, and the cancer project does it twice over with HTTP ingestion pipelines. Two
independent implementations is the right basis for a spec — whatever they do differently is
either an improvement worth standardizing or an inconsistency worth naming.

The questions such a skill would have to settle once rather than per project: idempotency
on re-run, what happens to one bad record in a large batch, provenance (which source and
which fetch produced a row), retry and rate-limit policy, incremental vs. full reload, and
what the tests do about the network.
