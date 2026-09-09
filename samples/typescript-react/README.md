# tsgo-playground

A small Vite + React + Tailwind + TanStack Router app whose point is the compiler: `typescript@7`,
the native Go port, doing the type-checking.

```bash
npm install
npm run dev          # http://localhost:5173
```

## Pages

The sample content is a made-up collection of extraterrestrial recipes — nine dishes from nine
worlds, none of them cookable at a domestic stove. It is there to give the type-level code something
to be about.

- **Overview** (`/`) — how to read a recipe, plus a count by course from `Object.groupBy`.
- **Recipes** (`/recipes`) — search params validated on the way in. `validateSearch` takes a loose
  input type marked with `SearchSchemaInput` and returns a strict one, so `<Link to="/recipes">`
  needs no `search` prop while `Route.useSearch()` still reads `{ course: Course | 'all'; q: string }`.
- **Recipe detail** (`/recipes/$slug`) — a loader that narrows the URL param to a branded `Slug`
  before lookup, throws `notFound()` if it misses, and times itself with a `using` declaration. The
  servings control is a reducer over a discriminated union ending in `assertNever`: add a variant to
  `ServingsAction` without handling it and the default branch stops compiling.

