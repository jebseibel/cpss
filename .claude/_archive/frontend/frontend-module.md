# Frontend

React + TypeScript + Vite single-page app in `frontend/`, served standalone in dev and bundled
into the Spring Boot jar for deployment.

> **Verified 2026-08-12** against `frontend/src`, `src/App.tsx`, `src/services/api.ts`, and
> `package.json`.

## Technology Stack

React 19.1, TypeScript 5.9, Vite 7.1, React Router DOM 7.9, Tailwind CSS 4.1,
TanStack React Query 5.90, Axios 1.13, Lucide icons.

Also declared but **not currently used** by any page: React Hook Form + `@hookform/resolvers`,
Zod, Recharts. Forms are hand-rolled `useState` — worth knowing before assuming a form library
is in play. Recharts is the interesting one: it is the obvious tool for the flavor-quadrant
visualization and is already a dependency, but nothing imports it yet.

## Structure

```
frontend/src/
├── App.tsx                      routes
├── main.tsx
├── components/
│   ├── Layout.tsx               nav bar + <Outlet/>, logout button
│   └── ProtectedRoute.tsx       redirects to /login when no token
├── pages/
│   ├── Login.tsx
│   ├── ForgotUsername.tsx       username reminder request
│   ├── ForgotPassword.tsx       reset-link request
│   ├── ResetPassword.tsx        consumes the emailed token
│   ├── Dashboard.tsx
│   ├── BeginHere.tsx            onboarding / how the system works
│   ├── MyStory.tsx              narrative page
│   ├── Salads.tsx               the user's saved salads
│   ├── SaladBuilder.tsx         build/edit a salad — the core surface
│   ├── Mixtures.tsx             saved dry mixtures
│   ├── MakeMixture.tsx          build/edit a mixture
│   ├── MixtureShop.tsx          shopping view for one mixture
│   ├── Foods.tsx                food catalog browser
│   └── Nutrition.tsx            nutrition records
├── lib/
│   └── utils.ts
├── services/api.ts              axios client + all endpoint groups
└── types/api.ts                 response/request interfaces
```

There is **no modal component**, and that is deliberate — see `DESIGN_DECISIONS.md` §10. The
project rule is that the frontend uses no modal dialogs, so create/edit/confirm flows are
full routes instead. `MakeMixture` and `SaladBuilder` are pages for this reason.

## Routes

| Route | Page | Access |
| --- | --- | --- |
| `/login` | Login | public |
| `/forgot-username` | Forgot Username | public |
| `/forgot-password` | Forgot Password | public |
| `/reset-password` | Reset Password | public (token in query string) |
| `/` | Dashboard | protected |
| `/begin-here` | Begin Here | protected |
| `/my-story` | My Story | protected |
| `/salads` | Salads | protected |
| `/salad-builder` | Salad Builder (new) | protected |
| `/salad-builder/:extid` | Salad Builder (edit) | protected |
| `/mixtures` | Mixtures | protected |
| `/mixtures/new` | Make Mixture | protected |
| `/mixtures/edit/:extid` | Make Mixture (edit) | protected |
| `/mixtures/shop/:extid` | Mixture Shop | protected |
| `/foods` | Foods | protected |
| `/nutrition` | Nutrition | protected |
| `/profiles` | placeholder — "coming soon" | protected |
| `/companies` | placeholder — "coming soon" | protected |

The four auth routes are public; everything else is wrapped in `ProtectedRoute` inside a
`Layout`. Note `/profiles` and `/companies` are inline placeholder `<div>`s in `App.tsx`, not
real pages — the `Company` REST resource exists on the backend with no UI in front of it.

## API layer

`services/api.ts` exports one object per resource group: `foodApi`, `nutritionApi`,
`companyApi`, `saladApi`, `mixtureApi`, `authApi`, plus `authHelpers`.

- Base URL from `VITE_API_URL`, defaulting to `/api`.
- Request interceptor attaches `Authorization: Bearer <token>` from `localStorage`.
- Response interceptor clears the token and redirects to `/login` on 401/403 — **except** on
  `/auth/` endpoints, so the login form can show its own error.

**Everything on the wire is an `extid`.** No numeric id ever crosses the API boundary, including
FK-like references (`foodExtid`, `userExtid`). Controllers resolve extid → internal id. See
`DESIGN_DECISIONS.md` §2.

## Two things that surprise people

**Nutrition and flavor totals come from the server, not the client.** `SaladBuilder` shows
live totals as ingredients change, but the aggregation is `service/NutritionCalculator` on the
backend — the numbers in a salad response are already computed. The frontend does not
reimplement the `(per100g × grams) / 100` scaling, and it should not: duplicating that formula
is how the two copies drift apart.

**Integer truncation is visible in the UI.** Because nutrition is integer arithmetic end to
end, a small portion of a low-value food contributes `0` to the totals — 10g of a food with 5g
carbohydrate per 100g reads as zero carbs, not 0.5g. Users notice this before developers do.
It is a known limitation, not a display bug; see `DESIGN_DECISIONS.md` outstanding item 3.

## Commands

- `npm run dev` — Vite dev server (only start this if explicitly asked)
- `npm run build` — `tsc -b && vite build`
- `npm run lint` — ESLint
- `./gradlew buildDeployment` (repo root) — builds the frontend and bundles it into the jar

## Related

- `../../../DESIGN_DECISIONS.md` — the no-modals rule (§10) and the extid boundary (§2)
- `../../ARCHITECTURE.md` — the endpoints these API groups call
- `../database/DOMAIN_MODEL.md` — what a Food, Salad, and Mixture actually are
