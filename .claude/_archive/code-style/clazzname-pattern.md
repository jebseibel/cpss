# CLAZZNAME Pattern Documentation

> **Status in this project (verified 2026-08-08).** Not yet used — zero occurrences of
> `CLAZZNAME` in the codebase. **Adopted going forward** (decision 2026-08-08): apply to new
> classes as they are written; do not retrofit existing ones in bulk.

## Overview
The CLAZZNAME pattern replaces hardcoded class name strings in log statements with a static final constant. This provides a single source of truth for the class identifier used in logging.

## Pattern Definition

### Declaration
Define a `private static final String` constant at the **top of the class**:

- **Variable Name:** `CLAZZNAME` (intentionally misspelled)
- **Visibility:** `private static final`
- **Position:** First constant declared in the class
- **Value:** The simple class name as a string (e.g., `"NutritionCalculator"`)

```java
private static final String CLAZZNAME = "NutritionCalculator";
```

### Usage in Log Statements
Replace hardcoded class name strings in `log` calls with `CLAZZNAME`:

Instead of:
```java
log.info("NutritionCalculator - processing {}", id);
```

Use:
```java
log.info("{} - processing {}", CLAZZNAME, id);
```

## Key Points

- **Naming Convention:** The intentional misspelling "CLAZZNAME" makes it distinctive and searchable
- **Scope:** `private` to each class — not shared across classes
- **Type:** `String` constant (`static final`)
- **No constructor required:** This pattern does NOT use a `thisName` instance variable or constructor assignment — it is a static constant only
- **No `thisName`:** Do not add `thisName` or modify constructors as part of this pattern

## Applied Scope

**Not yet used in this project** — verified against CPSS 2026-08-12, zero occurrences of
`CLAZZNAME` in the codebase. Adopted going forward; apply it to new classes as they are written
rather than retrofitting existing ones in bulk.

Where it would earn its keep in CPSS: `DataLoader` and the classes it calls during startup
seeding. `DataLoader` walks ~34 CSVs across four entity types through the `*DbService` layer, and
when a row fails, the log needs to say which loader stage was running. Beyond that, CPSS is
mostly request-scoped CRUD where a stack trace already identifies the class — the pattern pays
off in long-running loops with interleaved output, which is also where the
`.claude/skills/progress-ticker/` skill applies.

Honest note on value: in a single-module Spring app with SLF4J, the logger is already named for
its class, so most log configurations print the class name anyway. `CLAZZNAME` earns its place
where the *message* needs the name inline — grepping a mixed log for one pipeline stage — not as
a default for every class.

Note the existing convention in those classes is a bare method-name prefix —
`log.info("promote(): extid={}", extid)`. CLAZZNAME composes with it rather than replacing it:

```java
log.info("{} promote(): extid={}", CLAZZNAME, extid);
```
