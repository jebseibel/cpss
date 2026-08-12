---
name: login-rate-limit
description: Throttle repeated failed login attempts per IP on a Spring Boot + Spring Security app, returning 429 with Retry-After after N consecutive failures. Adds a OncePerRequestFilter registered ahead of authentication, plus config properties. Use whenever asked to rate limit login, throttle password attempts, stop brute-force or credential-stuffing against /api/auth/login, or when reviewing an app whose login endpoint is public and unthrottled.
---

# Login Rate Limiting

## Project Goal

`/api/auth/login` has to be public — a browser cannot present a JWT before it has one — so it is
the single unauthenticated door into an otherwise-secured app. Unthrottled, an attacker guesses
passwords as fast as the network allows, and a weak password falls in seconds.

**Every other access control is worth only as much as this door.** Restoring endpoint security
across an API buys nothing if the login endpoint can be brute-forced.

The goal: after N consecutive failures from one IP, return **429** with a `Retry-After` header
for a lockout window, while never inconveniencing someone who simply mistyped.

## When to apply this

- Any Spring Boot app with a public login endpoint and no throttle (the common default).
- Reviewing an app before it becomes internet-reachable.
- Any app holding data that matters — medical, financial, personal.

## What to build

Three pieces. Do all three; the third is the one that gets forgotten.

1. **`LoginRateLimitFilter`** — a `OncePerRequestFilter`. Copy from
   `references/LoginRateLimitFilter.java` and change the package.
2. **Register it before authentication** in `SecurityConfig`:
   ```
   .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
   ```
   Order matters: a locked-out address must be rejected **before** any password hashing, or the
   throttle still pays the cost of every guess. bcrypt is deliberately expensive — that cost is
   an attacker's lever if you let them reach it.
3. **Config properties** with sane defaults (see `references/application-snippet.yml`).

## The five design points that make it correct

Each of these was a real decision, and getting any one wrong makes the filter useless or harmful.

**Key on IP *and* username — never IP alone.** This is the one that bites, and it is easy to
convince yourself IP-only is correct. It is not: failed guesses against *any* username lock out
*every* user from that address. Behind Nginx or CGNAT many people share one address, so an
attacker throttles your legitimate users on purpose — a denial of service built out of a
security control.

Found exactly that way in the project this came from: eight probes against a *nonexistent*
username locked the real account out of the same machine, and the "verify it works" step turned
into "verify it broke login". Keep the IP half too — without it, one attacker locks a known
username from anywhere.

Reading the username means reading the request body in a filter, and a servlet input stream is
**single-pass**: read it naively and the controller receives an empty body, breaking every login.
Wrap the request and replay the buffer (`CachedBodyRequest` in the reference file). Extract with
a small regex rather than a JSON parse — this runs before authentication on a public endpoint,
so it must not throw on malformed or hostile input — and truncate the value, because an
attacker-controlled string is about to become a map key.

**Count only consecutive failures; clear the counter on success.** Someone who mistypes twice and
then gets it right must never be locked out. You are limiting *guessing*, not *using*. Without
this, a legitimate user who fumbles a password gets locked out of their own data.

**Bound the tracking map.** An unbounded `ConcurrentHashMap` keyed on IP is a memory-exhaustion
vector — a spray from forged addresses grows it without limit. Cap it (10k is ample) and drop
new entries when full: the addresses already tracked are the ones actively guessing.

**Honour `X-Forwarded-For`, but know what it means.** Behind Nginx the socket address is the
proxy for everyone, so without this the limiter throttles all users as one. The header is
client-forgeable, so it is only trustworthy when the proxy overwrites it — which is a reason to
terminate at a proxy, not a reason to ignore it. Read directly on a public port, an attacker
rotates the header and evades the limit entirely.

**Detect failure by response status, not by exception.** Wrap `chain.doFilter`, then inspect
`response.getStatus()`: 401 is a failed credential, anything under 400 is a real login. This
keeps the filter independent of how the controller reports errors.

**Return 429 with `Retry-After`.** Not 403. A well-behaved client can back off; a human sees an
honest message instead of a generic denial.

## Verify it, do not assume it

A security control that silently fails is worse than none. With the app running:

```bash
# N+1 wrong passwords should end in 429
for i in $(seq 1 9); do
  curl -s -o /dev/null -w "%{http_code} " -X POST http://localhost:8080/api/auth/login \
    -H 'Content-Type: application/json' -d '{"username":"you","password":"wrong"}'
done; echo

# The correct password must ALSO be refused while locked out
curl -s -o /dev/null -w "locked: %{http_code}\n" -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"you","password":"CORRECT"}'
```

Expect `401 401 401 401 401 401 401 429 429` then `locked: 429`. If the correct password still
returns 200, the filter is not registered — check that the bean is injected into `SecurityConfig`
and that `addFilterBefore` actually runs.

**Then the test that catches the IP-only mistake** — lock out a junk username, and confirm a real
one still works from the same machine:

```bash
for i in $(seq 1 9); do
  curl -s -o /dev/null -X POST http://localhost:8080/api/auth/login \
    -H 'Content-Type: application/json' -d '{"username":"nosuchuser","password":"wrong"}'
done
# MUST be 200. If this is 429, the key is IP-only and you have built a DoS.
curl -s -o /dev/null -w "real user: %{http_code}\n" -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"you","password":"CORRECT"}'
```

Also confirm the body still reaches the controller — if login returns 400 "username is required"
after adding the filter, the input stream was consumed and not replayed.

Then confirm a mistype does not lock anyone out: two failures, then a success, then another
success. All must pass.

## Honest limits — say these out loud

- **In-memory and per-instance.** Resets on restart, does not coordinate across instances. Fine
  for a single instance; move the counter to Redis if it ever scales out.
- **Stops online guessing, not a distributed attempt** from many addresses. A strong password
  remains the real control — this buys time, it does not replace it.
- **Not a substitute for MFA** on anything genuinely sensitive.

## Related work worth raising at the same time

Rate limiting is usually found while auditing an auth surface. Whenever you add it, check these
too — they were all live holes in the project this skill came from:

- Is `/api/auth/register` **anonymous**? On a single-user app, open registration lets a stranger
  mint an account and inherit whatever the app grants a logged-in user. Require ADMIN or remove it.
- If you use `@PreAuthorize`, is **`@EnableMethodSecurity`** present? Without it the annotation is
  silently ignored and the endpoint looks protected while standing open.
- Does authentication check the **active/deleted flag**? With soft deletes, a "deleted" account
  often still logs in.
- Is any **password hash written to a log**? Debug lines that print bcrypt output leave
  credentials in plaintext on disk.
- Does any endpoint take a **user id from the URL** without comparing it to the caller? That is an
  authorization gap, and rate limiting does nothing for it.
