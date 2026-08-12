# CPSS Website Copy Draft

---

## Client-Facing Section

### Crunch Punch Sweet Savory
**A smarter way to build your salad — literally.**

CPSS is a playful nutrition app that lets you build custom salads and food mixtures from real ingredients, then watches the numbers update instantly as you go. Drag in spinach, add some chicken, toss in a handful of walnuts — calories, macros, vitamins, and fiber recalculate live, scaled precisely to how much you actually use.

But the fun part isn't just the numbers. Every ingredient carries a flavor profile — **Crunch, Punch, Sweet, and Savory** — so you can actually see whether your salad is balanced or about to be a soggy, one-note mess before you make it.

- 🥗 **Salad Builder** — pick ingredients, set portions, watch nutrition update in real time
- 🌶️ **Flavor Balance** — crunch/punch/sweet/savory scoring keeps every mix interesting
- 🧪 **Mixture Creator** — save your own custom blends and reuse them anytime
- 📱 **Works everywhere** — fully responsive on mobile and desktop, no app download needed

#### Why I built it

I grew up on Hamburger Helper, white bread, and canned soda. A "salad" meant iceberg lettuce, a couple of tomato wedges, and some diced carrots — mostly just a vehicle for dressing, which, let's be honest, is usually just sugar in disguise.

Then I became a single dad, and I wanted better for my kids than I'd had. Getting them to eat vegetables was a daily battle — *eat your vegetables!* — until I stumbled onto something simple: if you balance a salad across four dimensions — **crunch, punch, sweet, savory** — it stops needing a bottle of dressing to be worth eating. It just works, with or without it.

Most salad books lean on exotic ingredients or elaborate dressings, which is great if you've got the time and a well-stocked pantry — most of us don't. This system is built for real life: simple, balanced, and something you'll actually want to eat, not just something you think you should.

So I built a tool that would let me — and anyone else — build salads that hit that balance instantly, paired with real nutritional tracking, so every combination is both dialed-in and genuinely satisfying.

It's a small tool with a simple hope: make mindful eating a little easier, and a lot more enjoyable.

Try it yourself: **[crunchpunchsweetsavory.com](http://crunchpunchsweetsavory.com/)**

*A small, fun project — built the way I build everything: with a serious engine underneath.*

---

## Technical Deep Dive

### Small app, real architecture

CPSS looks like a fun salad builder. Underneath, it's a properly engineered full-stack application — the same architectural discipline I bring to client work, just applied to something lighter.

**Clean layered architecture**
The backend follows a strict four-layer separation: controllers handle HTTP only, services own business logic, a dedicated database-service layer manages persistence, and repositories talk to the database. Domain models are kept entirely distinct from JPA entities — mapped explicitly at the boundary — so business logic never leaks into the data layer and vice versa. It's the kind of separation that keeps a codebase maintainable long after the "fun weekend project" phase ends.

**A real calculation engine, not a lookup table**
Every food is stored once, standardized per 100g. When you build a salad, the backend scales each ingredient's full nutrition profile to your exact gram weight, aggregates across every ingredient, and derives calories from macros server-side — live, on every change. It's a small but genuine computation layer, not static data.

**Production-grade security**
Stateless JWT authentication, BCrypt password hashing, and a single-use, time-boxed password-reset flow with proper token cleanup and no user-enumeration leakage — the same auth patterns you'd expect from a production client application.

**Modern, responsive stack**
Java 21 and Spring Boot on the backend; React, TypeScript, Vite, and Tailwind on the frontend; versioned database migrations via Liquibase. Fully responsive out of the box — the same experience on a phone in the kitchen as on a desktop planning the week ahead.

**The takeaway:** even a "fun" project gets the full treatment — thoughtful architecture, real security, and code built to last. That's the standard I hold for every build, big or small.
