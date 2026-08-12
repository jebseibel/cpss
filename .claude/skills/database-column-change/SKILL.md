---
name: database-column-change
description: Change one or more existing columns on a table - widen or narrow a type, rename, change nullability, add or drop a column - and update every layer that pins that column's shape (Liquibase changeset, JPA entity, request DTOs, test builder, tests). Use whenever asked to alter, widen, resize, rename, or add/drop a column on an entity that already exists. For creating a brand-new entity, use database-restapi-template or entity-full-stack instead.
---

# Database Column Change

## Project Goal

The other database skills all **create**. This one **modifies**: the entity already exists,
the files already exist, and the work is finding every place a column's shape is duplicated
and moving them together.

Scoped to **one table, any number of columns**, because the expensive parts — the database
rebuild and the test run — are per-table, not per-column. Batching them is strictly cheaper
than running this once per column.

## `{basePackage}` in this project

This is the CPSS-local copy, so the package roots are already resolved:

- `{basePackage}` = `com.seibel.cpss`
- `{basePackagePath}` = `com/seibel/cpss`

**CPSS is single-module**, so all paths below are under one `src/` tree rather than the
upstream skill's separate `common/` and `database/` modules.

`{Entity}` is the PascalCase domain name (`Food`); `{table}` is the snake_case table
(`food`).

**Read `.claude/CLAUDE.md` before editing a changeset.** It defines "rebuild the database"
for this project as triggering the n8n webhook at
`http://localhost:5678/webhook/clear-cpss-db` (a GET), which is the resolution path when a
checksum breaks.

## Input

- **Table / entity** — e.g. `location` / `Location`
- **One or more column changes**, each stating the change type and the new spec:

```
zip        widen    varchar(16) -> varchar(64)
status     rename   status -> recruitment_status
notes      add      varchar(1000), nullable
old_flag   drop
city       nullable not null -> nullable
```

If the caller says only "make zip bigger", ask for the target size rather than guessing.

## Step 1 — Establish which layers actually pin the shape

**Do not assume the table below; verify it in the target project first.** It reflects the
common shape of this layout, but a project may add validation the others do not have.

Grep the column's field name across the candidate files and see which carry a constraint:

```bash
grep -rn "{fieldName}" \
  src/main/resources/db/changelog/changes/ \
  src/main/java/**/db/entity/{Entity}Db.java \
  src/main/java/**/domain/{Entity}.java \
  src/main/java/**/db/mapper/{Entity}Mapper.java \
  src/main/java/**/web/request/ src/main/java/**/web/response/
```

Typical result in this layout — usually **three** layers constrain a column, and knowing the
five that do *not* is what stops the skill touching files pointlessly:

| Layer | File | Pins the shape? |
| --- | --- | --- |
| Changeset | `src/main/resources/db/changelog/changes/NNN-{table}.yaml` | **Usually yes** — `type: varchar(n)`, `constraints: { nullable: false }` |
| Entity | `src/main/java/{basePackagePath}/database/db/entity/{Entity}Db.java` | **Usually yes** — `@Column(name = "...", length = n)` |
| Request DTOs | `src/main/java/{basePackagePath}/web/request/Request{Entity}Create.java` and `...Update.java` | **Usually yes** — `@Size(max = n)`, `@NotEmpty`/`@NotNull` |
| Response DTO | `src/main/java/{basePackagePath}/web/response/Response{Entity}.java` | Usually no — plain fields |
| Domain POJO | `src/main/java/{basePackagePath}/common/domain/{Entity}.java` | Usually no — plain POJO |
| Mapper | `src/main/java/{basePackagePath}/database/db/mapper/{Entity}Mapper.java` | Usually no — ModelMapper is type-agnostic |
| Test builder | `src/test/java/{basePackagePath}/testutils/DomainBuilder*.java` | Only if its generator can exceed the new bound |
| Tests | `src/test/java/**/db/{mapper,repository,service}/{Entity}*Test.java` | Only if they assert the old shape |

**Missing one of the constraining layers is the characteristic failure.** Widening only the
changeset leaves the column wide while the app still rejects the value — which reads as "the
schema change didn't work" rather than "a validation annotation was missed."

For a rename or a type change the non-constraining layers *do* come into play, because the
field name or Java type flows through all of them.

## Step 2 — Apply, per change type

### Widen (e.g. `varchar(16)` -> `varchar(64)`)

Safe — no existing data can violate a larger bound.

1. Changeset `type:`
2. Entity `@Column(length = ...)`
3. `@Size(max = ...)` in **both** request DTOs, and the message text with it
4. Test builder: usually nothing — generators are typically capped well below common widths
5. **Comment the changeset with *why*** if the widening absorbs malformed source data rather
   than accommodating legitimately longer values. That distinction is not recoverable from the
   number alone, and the next reader needs it.

### Narrow

Same layers, plus: **check existing data first.** Ask the user to count rows exceeding the new
bound — never query the database directly. If any exist, stop and report; narrowing past live
data truncates silently on rebuild.

### Rename

The widest ripple. Beyond the constraining layers: domain field, entity field name, mapper if
it maps explicitly, both request DTOs, response DTO, every repository finder naming the
property (`findByOldName`), every service and controller reference, the test builder setter,
tests, and — if the project has a frontend — its API types and any component reading the field.

Grep the old name across `--include="*.java" --include="*.ts" --include="*.tsx"` before
starting and report the count, so the caller sees the scope before committing to it.

### Nullability

A changeset `constraints: { nullable: false }` pairs with `@NotEmpty` (String) or `@NotNull`
(other types) on the **create** DTO. The update DTO stays unconstrained — partial updates omit
fields by design.

Making a column NOT NULL requires every existing row to have a value; ask the user to check.

### Add

Effectively a mini-scaffold: changeset column, entity field + `@Column`, domain field, both
request DTOs, response DTO, mapper if explicit, a builder method, and — easy to miss —
**`validateUpdateRequest()` in the controller's converter**, if the project uses that pattern.
A new field absent from that null-check means a partial update containing only the new field
is rejected as empty.

### Drop

Reverse of add. Grep the field name first. A drop that leaves a Java reference behind fails to
compile, which is the *good* case; the bad case is a native query or a frontend field still
selecting it.

## Step 3 — The rebuild trap

**Editing an already-applied changeset breaks its Liquibase checksum.** Liquibase stores a hash
per changeset; changing the file makes it stop matching, and startup fails with:

```
Validation Failed:
  1 changesets check sum
    db/changelog/changes/NNN-{table}.yaml::create_{table}_table::author
    was: 9:9c47... but is now: 9:1e27...
```

**Two project-dependent facts decide how much this hurts. Check both before starting:**

1. **Is `spring.liquibase.drop-first` true or false?** If true, the changelog is re-applied on
   every boot and the checksum resolves itself on restart. If **false**, the changelog is not
   re-applied and the database must be rebuilt before anything works.

   ```bash
   grep -rn "drop-first" src/main/resources/application*.yml
   ```

2. **Do the tests share a database with the app?** Compare the datasource URL in
   `src/main/resources/application*.yml` with the test profile under
   `src/test/resources/`. If they resolve to the same schema, a checksum failure
   breaks the *entire test suite* as a Spring context-load error that looks nothing like a
   column problem — which is deeply misleading if you don't expect it.

   ```bash
   grep -rn "url:" src/main/resources/application*.yml src/test/resources/
   ```

**Whether to edit in place or add a new changeset is a project convention — check `CLAUDE.md`.**
Projects not yet in production commonly edit in place and accept the rebuild; production
projects add a new changeset with `modifyDataType` / `renameColumn` / `addColumn` / `dropColumn`,
which applies cleanly with no rebuild. Do not switch conventions unilaterally.

**The rebuild is the user's action, always.** Never run it, never drop a schema, never touch
the database directly. Tell the user it is needed, by whatever mechanism that project uses, and
warn them what it costs — typically all ingested/seeded data, and any stored tokens or state
that live only in the database.

## Step 4 — Verify

1. **Read the changeset first** to confirm the current spec. Do not trust the request's "from"
   value; it may be stale.
2. **Apply every change to all affected layers in one pass**, before compiling.
3. **Quote comma-bearing types**: `type: "decimal(10,2)"`. Unquoted, Liquibase's YAML parser
   reads the comma as a map separator and aborts the entire changelog with a parse error far
   from the actual line.
4. **Compile** — `./gradlew compileJava compileTestJava`. This is what catches renames and drops.
5. **If a rebuild is required, stop and ask for it.** Tests cannot pass first; a failure at this
   point is the checksum error, not a defect in the change.
6. **After any rebuild**, run `./gradlew test --tests "*{Entity}*"`, then the full suite —
   the shared test builder can affect unrelated entities.
7. **Read test counts from the XML, not Gradle's exit code** — Gradle reports success when zero
   tests run:

```bash
python3 -c "
import xml.etree.ElementTree as ET, glob
for p in sorted(glob.glob('database/build/test-results/test/TEST-*.xml')):
    r=ET.parse(p).getroot()
    print(r.get('name').split('.')[-1], r.get('tests'), 'failures='+r.get('failures'))
"
```

## Report back

- Every file changed, grouped by layer
- Any layer deliberately **not** touched, and why (e.g. "response DTO carries no constraints")
- For a rename: grep counts before and after, confirming zero stale references
- Whether a rebuild is required, and what it costs
- Test counts after the rebuild

## Do not

- Run a rebuild, drop a schema, or connect to the database. All three are the user's.
- Switch between edit-in-place and new-changeset without checking the project's convention.
- Change only the changeset. That is the characteristic bug this skill exists to prevent.
- Assume the layer table above without verifying it in the target project.
- Widen a column to accommodate malformed data without saying so in a comment. The next reader
  needs to know whether `64` means "these values can legitimately be this long" or "one source
  sends two values crammed into one field."
