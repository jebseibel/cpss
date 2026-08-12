# CPSS Design Decisions

Decisions taken while building CPSS, the alternatives rejected, and what each one cost.
Written to be argued with — where a decision has a downside, the downside is stated rather
than defended.

**Verified against the code: 2026-08-12.** Claims were checked against the files named in
them. Where older planning docs disagree with the code, the code wins and the discrepancy
is called out.

---

## 1. Domain models kept separate from JPA entities

**Decision.** Two parallel object graphs. `common/domain/Food` is a plain POJO with no
persistence annotations; `database/db/entity/FoodDb` carries the JPA mapping. Mappers
(ModelMapper) translate between them at the persistence boundary, and converters translate
domain → DTO at the web boundary.

**Alternative rejected: use JPA entities as the domain model.** One class instead of three,
no mapper layer, far less code.

**Why not.** Entities carry Hibernate semantics that leak — lazy-loading proxies,
persistence-context identity, `LazyInitializationException` when a detached entity is
touched outside a transaction. Once entities become the currency of the service layer,
every service is implicitly transaction-scoped and every serialization path risks
triggering a query. Keeping domain objects inert means the service layer works with plain
data.

**What it cost.** Real boilerplate: three classes and two translation steps per entity, and
a mapper bug is invisible to the type system. That bug happened here — `SaladMapper.toModel()`
did not populate `foodIngredients` at all, so salads came back from the database with empty
ingredient lists. Nothing failed to compile; the field was simply always empty. Fixed by
injecting `SaladFoodIngredientMapper` and mapping the collection explicitly.

**Assessment.** Correct for a layered app, and the mapper bug is the honest argument
against it. A generated or compile-time-checked mapper (MapStruct) would keep the
separation and eliminate that failure mode.

---

## 2. `extid` on the wire, numeric `id` internally

**Decision.** Every entity has both a numeric `id` and a UUID `extid` (`BaseDomain`). The
numeric id never leaves the backend; every API path, request body, and response uses
`extid`. `GET /api/salad/{extid}`, `foodExtid` in ingredient payloads, and so on.

**Alternative rejected: expose the numeric primary key.** Simpler, smaller, and faster to
index.

**Why not.** Sequential ids leak information — how many records exist, roughly when a row
was created relative to another — and they make enumeration trivial: `/api/food/1`,
`/api/food/2`. A UUID is opaque and unguessable.

**What it cost.** Every lookup by extid is a secondary-index hit rather than a primary-key
hit, and every entity carries a 36-char column. Joins internally still use the numeric id,
so the cost is confined to boundary lookups. At this data size it is not measurable.

**Caveat.** This is defense in depth, not authorization. An unguessable id is not an access
control — see §7.

---

## 3. Soft deletes everywhere

**Decision.** `BaseDomain` carries `deletedAt` and an `active` (`ActiveEnum`) field. Delete
endpoints set these rather than issuing `DELETE`.

**Alternative rejected: hard deletes.**

**Why not.** A user deleting a salad is usually correcting a mistake, and the data is
cheap to keep. Recovery is a flag flip instead of a restore from backup.

**What it cost.** Two real costs, both under-managed here. Every query must filter on
active status, and forgetting that filter surfaces deleted rows — a bug the type system
cannot catch. And nothing ever purges: the tables grow monotonically with no archival or
hard-delete path. At this scale that is fine; it is a decision with an expiry date.

---

## 4. Calories derived, never stored

**Decision.** No calorie column anywhere. Calories are computed from macros at response
time: `(carbs × 4) + (protein × 4) + (fat × 9)`.

**Alternative rejected: store calories alongside macros.** One less computation, and it
would let a food carry a lab-measured calorie count that differs from the macro-derived
figure.

**Why not.** Two sources of truth that can disagree. Correcting a food's fat content
without recomputing its calories leaves a row that is internally inconsistent, and nothing
would detect it.

**What it cost.** The Atwater factors (4/4/9) are approximations. Fiber in particular is
counted as carbohydrate at 4 cal/g here, while it is largely indigestible — so
high-fiber foods read as slightly more caloric than they are. Accepted because CPSS is
about relative balance, not precise intake.

---

## 5. Per-100g normalization for all nutrition and flavor

**Decision.** Every food stores nutrition and flavor per 100g. Any quantity is derived by
scaling: `(valuePer100g × grams) / 100`.

**Alternative rejected: store values per typical serving.** Closer to how packaging is
labeled and how people think ("one handful of almonds").

**Why not.** Serving sizes are incommensurable across foods. Summing "one serving of
lettuce" and "one serving of feta" requires converting both to a common unit first, so the
conversion is unavoidable — the only question is whether it happens once at load time or
on every calculation.

**What it cost.** Data entry must normalize to 100g up front, which is the tedious part of
adding a food. `typicalServingGrams` survives on `Food` to keep the human-friendly unit
available for display.

**Related.** The original schema had a full `Serving` entity (cup, quarter, tablespoon,
teaspoon, gram). It was dropped in changeset `014-drop-serving-table.yaml` and replaced
with the single `typicalServingGrams` integer — an entity and its joins collapsed into a
column once per-100g normalization made the unit conversions unnecessary.

---

## 6. Foundation validation in the service layer

**Decision.** "A salad must contain at least one `foundation = true` food" is enforced in
`SaladService.validateFoundationCount()`, called from both `create()` and `update()`.

**Alternative rejected: a database constraint.** Impossible in practice — no constraint
expresses "at least one related row satisfies a predicate on a joined table" without a
trigger, and a trigger would give a violation message meaningless to the user.

**Alternative rejected: frontend-only validation.** The UI does enforce it (foundation
counter, "BASE" badge, submit blocked below 1), and that is good UX — but it is not
enforcement. The API is reachable directly.

**Why the service.** One authoritative place, a domain-specific message, and the same rule
applies regardless of client.

**What it cost.** A round trip to load the foods and check their flags on every write.
Originally that was one query per ingredient — an N+1 where a ten-ingredient salad cost ten
lookups. Fixed by adding `findByExtidIn()` through `FoodRepository` → `FoodDbService` →
`FoodService` and collapsing to a single batch query, with `.distinct()` on the extids so a
repeated food is not fetched twice.

**Worth noting about the fix.** The tests had mocked `findByExtid()`, so they kept passing
against the old call shape and had to be updated to mock `findByExtidIn()`. A mock-based
test suite verifies the interaction you wrote down, not the one the code makes — changing
the call pattern required changing the tests in lockstep, which is the tax that style of
test charges.

---

## 7. JWT, stateless, no refresh token

**Decision.** Stateless JWT, HS256, 24-hour expiry, stored in `localStorage` on the client,
validated per request by `JwtAuthenticationFilter`. Passwords BCrypt-hashed.

**Alternative rejected: server-side sessions.** Revocable immediately, no token-in-storage
question.

**Why not.** Stateless auth needs no session store and scales horizontally without sticky
sessions or shared state.

**What it cost — stated plainly.** *Tokens cannot be revoked before they expire.* A
compromised token is valid for up to 24 hours, and "log out everywhere" is not
implementable as built. Password reset does not invalidate outstanding tokens, so an
attacker holding a token keeps access even after the victim resets. Mitigating this needs
either a revocation list — which reintroduces the server-side state JWT was chosen to
avoid — or short-lived access tokens plus a refresh flow, which is the actual fix and is
not built.

`localStorage` is also readable by any XSS on the origin. React's auto-escaping makes XSS
unlikely rather than impossible; an httpOnly cookie would remove the class of attack at the
cost of CSRF handling.

**Assessment.** Right call for the architecture, and the 24-hour window is too long for it.
Shortening expiry and adding refresh is the highest-value security work outstanding.

---

## 8. Password reset: single-use, time-boxed, enumeration-resistant

**Decision.** A UUID token in `password_reset_token`, expiring in 1 hour, marked `used`
after redemption, with all of that user's other tokens deleted on success. Both
`/auth/forgot-password` and `/auth/forgot-username` return the same generic message whether
or not the email exists.

**Why the generic response.** A response that differs for known and unknown emails turns
the endpoint into an account-existence oracle. Returning "if that email exists..." either
way costs the user a little clarity and removes the oracle.

**Why delete sibling tokens on success.** Requesting a reset several times leaves several
valid tokens. Without cleanup, an older link in an inbox — or in an intercepted email —
still works after the password has been changed.

**Still absent:** no notification email when a password actually changes, so a successful
takeover is silent.

---

## 8a. Login rate limiting — added 2026-08-12

**Decision.** `LoginRateLimitFilter`, a `OncePerRequestFilter` registered *before*
`JwtAuthenticationFilter`. Eight consecutive failures from one IP+username pair returns 429
with `Retry-After` for 15 minutes. Covers `/login`, `/forgot-password`, and
`/forgot-username`.

**Why it was needed.** `/api/auth/login` has to be public — a browser cannot present a JWT
before it has one — so it is the single unauthenticated door into the app. Unthrottled, an
attacker guesses passwords as fast as the network allows and every other access control is
worth only as much as that door.

**Why it runs before authentication.** A locked-out caller must be turned away before
reaching BCrypt. BCrypt is deliberately expensive; that cost becomes the attacker's lever if
guesses are allowed to reach it.

**The decision that matters most: key on IP *and* username, never IP alone.** Keying on IP
alone inverts the control into a denial of service — failed guesses against *any* username
would lock out *every* user from that address, and behind a proxy or CGNAT that is
potentially everyone. An attacker would deliberately lock out legitimate users. The IP half
is still needed, or one attacker could lock a known username from anywhere.

Verified live rather than assumed: eight probes against a nonexistent username returned 429
for that username while a different username from the same address still reached the
controller at 401. `LoginRateLimitFilterTest.lockoutIsScopedToUsername_notIpAlone()` guards
against reintroducing it.

**Only consecutive failures count**; a success clears the counter, so someone who mistypes
twice and then gets it right is never locked out. What is limited is guessing, not using.

**The forgot-* endpoints are counted differently, and this is a real wrinkle.** They
deliberately return 200 whether or not the account exists (§8), so there is no failure signal
to count — every successful call is counted instead. Without that they would never throttle
at all, since they never return 401.

**Reading the username means reading the request body in a filter**, and a servlet input
stream is single-pass. Read naively, the controller receives an empty body and every login
breaks. The request is wrapped and the buffer replayed. The username is extracted with a
small regex rather than a JSON parse — this runs before authentication on a public endpoint,
so it must not throw on hostile input — and truncated at 64 chars, because an
attacker-controlled string is about to become a map key.

**What it cost, stated honestly.** It is in-memory and per-instance: it resets on restart and
does not coordinate across instances. Fine for this single-instance deployment; it needs
Redis if that ever changes. It stops online guessing from one source, not a distributed
attempt from many addresses — a strong password remains the real control. `X-Forwarded-For`
is honoured because Nginx would otherwise make every user share the proxy's address, but that
header is client-forgeable, so it is only trustworthy behind a proxy that overwrites it.

---

## 9. Pagination on Company only — an inconsistency, not a decision

**Decision as implemented.** `CompanyController.getAll()` returns `Page<ResponseCompany>`
with `@PageableDefault(size = 20, sort = "name")`, and `CompanyService` enforces a maximum
page size and a sort-field whitelist, falling back to `name ASC` if a client requests only
non-whitelisted sort fields.

Every other controller — Food, Salad, Mixture, Nutrition — returns an unbounded
`List<...>`.

**This is worth reporting accurately because the existing docs disagree with each other.**
`.claude/ARCHITECTURE.md` lists "Pagination with configurable limits" as a project-wide
feature. `documents/CPSS_TECHNICAL_DOCUMENTATION_README.md` states "no pagination (all endpoints
return full lists)." Both are wrong: one endpoint has it, the rest do not.

**Why Company got it first.** Company was the first entity scaffolded from the layered
template, and the template includes pagination. The others were built for a UI that shows
everything at once — the food list is a browsing surface, not a search result — so the
paged variant was never wired through.

**Why the whitelist matters.** Passing a user-supplied string into `Sort.by()` lets a
caller sort by any persistent field, including ones that should not be externally
observable, and turns sort order into an information-disclosure channel. `CompanyService`
filters against `ALLOWED_SORT_FIELDS` before building the `PageRequest`. The unpaginated
endpoints do not accept sort input at all, so they are not exposed — but they will be the
moment pagination is added, and the whitelist has to come with it.

**Risk as it stands.** The food catalog is ~100 rows and salad lists are per-user, so
unbounded responses are currently small. It is a scaling cliff rather than a live problem.

---

## 10. Frontend: no modals

**Decision.** A standing project constraint (`.claude/CLAUDE.md`): the frontend uses no
modal dialogs. Flows that would conventionally be modals — create, edit, confirm — are
full pages or inline sections instead.

**Why.** Modals are awkward on mobile, trap focus in ways that need care to get right for
accessibility, and hide context exactly when a user needs it — building a salad means
comparing against what is already in the bowl. They are also linkable-to as pages and not
as overlays.

**What it cost.** More routes and more navigation. `SaladBuilder` is a page rather than an
overlay on `Salads`, so the ingredient list and the builder cannot be seen simultaneously.

---

## 11. The layered scaffold is codified as skills, not conventions

**Decision.** The nine-file layered pattern — Domain → DTOs → Entity → Mapper → Repository
→ DbService → Service → Controller+Converter, plus a Liquibase changeset — is generated
from a checked-in skill (`.claude/skills/database-restapi-template/`) rather than being a
convention people are expected to follow by hand. Tests, column changes, and the full
table-spec-to-tested-code chain have their own skills.

**Alternative rejected: document the pattern and copy an existing entity.** What most
projects do, and what this project did first.

**Why not.** Copying an entity carries its accidents along with its structure. The layers
each have a rule that is invisible in the shape of the code — a DbService throws
`ServiceException` but a business service throws `ValidationException` and
`ResourceNotFoundException`; a converter is package-private in the controller's own file;
FK columns are deliberately *not* emitted as fields. Those are the parts a copy silently
gets wrong, and a code review is unlikely to catch a missing distinction it cannot see.

**What it cost.** The skills are forked from `~/.claude/skills/`, because the upstream
copies target a multi-module Gradle layout and CPSS is single-module. A fix upstream does
not reach this repo automatically. Accepted deliberately — see
`.claude/skills/skills-reference.md`, which states the tradeoff rather than hiding it.

**The thing worth noticing.** Adapting four-module skills to a single-module project needed
only path rewrites; the *package* structure was already identical. The layered pattern
turned out to be independent of the module layout, which is not something I would have
asserted confidently before doing it.

---

## 12. Liquibase with CSV seed data

**Decision.** Schema managed by Liquibase changesets (`001`–`020`, plus `100-load-csv-data.yaml`),
with the food and nutrition catalog loaded from CSVs under `src/main/resources/db/data/`.

**Alternative rejected: Hibernate `ddl-auto`.** No migration files to write.

**Why not.** `ddl-auto` cannot express a destructive change safely, has no rollback, and
leaves no record of what changed. Changeset `014-drop-serving-table.yaml` is exactly the
kind of change that needs to be explicit and reviewable.

**Why CSV for the catalog.** The food data is edited as a table — nutrition values row by
row — and CSV is the format that is actually editable in that shape. Numbering it `100-`
keeps seed data after all structural changesets.

---

## Fixed 2026-08-12

- ✅ **Rate limiting on the public auth endpoints** (§8a). Verified live: 8 failures → 429,
  and a second username from the same address is unaffected.
- ✅ **Calculation logic extracted** out of the two controllers into
  `service/NutritionCalculator`. Behavior unchanged — the existing `MixtureConverterTest`
  cases passed unmodified, which is what proves it.
- ✅ **`.claude/ARCHITECTURE.md` corrected.** It described `Flavor` and `Serving` entities
  that no longer exist and claimed project-wide pagination.

Test suite: **101 → 125 tests, 0 failures.**

## Outstanding, worst first

1. **Tokens cannot be revoked; 24h expiry** (§7). Password reset does not invalidate
   existing sessions. Now the most significant security item.
2. **Toxicity and diabetes warnings not implemented.** Required per `.claude/CLAUDE.md`;
   the app shows sugar with no context. The one gap where being incomplete could mislead.
3. **Integer truncation in nutrition scaling.** See `DOMAIN_MODEL.md` — small quantities
   round to zero. Needs `BigDecimal` through domain, entity, and schema. Now asserted in
   `NutritionCalculatorTest` so a future fix produces a deliberate, visible test failure
   rather than silently changing what the app reports.
4. **No notification email on password change** (§8), so a successful takeover is silent.
5. **Unbounded list endpoints** (§9).
6. **Mixtures cannot be added to salads.** Designed in `.claude/salad-plan.md`, not built;
   the open question is the second scaling denominator. See `DOMAIN_MODEL.md`.
