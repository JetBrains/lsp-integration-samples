import { Link, createFileRoute } from "@tanstack/react-router";
import type { SearchSchemaInput } from "@tanstack/react-router";

import { COURSES, matches, recipes } from "../lib/recipes";
import type { Course } from "../lib/recipes";
import { describeCourse } from "../lib/type-demos";

type Filter = Course | "all";

/** What a link may pass in — everything optional. */
interface RecipeSearchInput {
  readonly course?: string;
  readonly q?: string;
}

/** What the component reads back out — everything present and narrowed. */
interface RecipeSearch {
  readonly course: Filter;
  readonly q: string;
}

function asFilter(value: unknown): Filter {
  return typeof value === "string" && (COURSES as readonly string[]).includes(value)
    ? (value as Course)
    : "all";
}

export const Route = createFileRoute("/recipes/")({
  /**
   * Hand-written validator — no schema library. The `SearchSchemaInput` marker on
   * the parameter is what lets the two halves differ: links type-check against
   * the loose input, `Route.useSearch()` gets the strict output.
   */
  validateSearch: (input: RecipeSearchInput & SearchSchemaInput): RecipeSearch => ({
    course: asFilter(input.course),
    q: typeof input.q === "string" ? input.q : "",
  }),
  component: RecipeList,
});

function RecipeList() {
  const { course, q } = Route.useSearch();
  const navigate = Route.useNavigate();

  const visible = recipes.filter(
    (recipe) => (course === "all" || recipe.course === course) && matches(recipe, q),
  );

  return (
    <>
      <section className="border-b border-line pb-5">
        <h1>Recipes</h1>
        <p className="max-w-[68ch] text-muted">
          Nine dishes from nine worlds. Each one needs something the local landscape provides — a
          trench, a vent, a comet, or a room nobody speaks in.
        </p>
      </section>

      <section className="flex flex-col gap-2">
        <input
          type="search"
          value={q}
          placeholder="Filter by dish, world or description…"
          aria-label="Filter recipes"
          className="w-full rounded-lg border border-line bg-sunken px-3 py-2 text-sm text-ink placeholder:text-dim"
          onChange={(event) => {
            const next = event.target.value;
            void navigate({ search: (prev) => ({ ...prev, q: next }), replace: true });
          }}
        />
        <div className="flex flex-wrap gap-2">
          {(["all", ...COURSES] as const).map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={value === course}
              className={
                value === course
                  ? "rounded-full border border-accent bg-accent-wash px-3 py-1 text-xs font-medium text-accent"
                  : "rounded-full border border-line px-3 py-1 text-xs font-medium text-muted hover:border-line-lit hover:text-ink"
              }
              onClick={() => {
                void navigate({ search: (prev) => ({ ...prev, course: value }) });
              }}
            >
              {value}
            </button>
          ))}
        </div>
      </section>

      {visible.length === 0 ? (
        <p className="text-muted">Nothing matches that filter.</p>
      ) : (
        <ul className="grid list-none gap-4 p-0">
          {visible.map((recipe) => (
            <li key={recipe.slug}>
              <Link
                to="/recipes/$slug"
                params={{ slug: recipe.slug }}
                className="block rounded-card border border-line bg-panel p-5 text-ink no-underline transition-colors hover:border-line-lit hover:no-underline"
              >
                <div className="mb-2 flex items-baseline justify-between gap-4">
                  <h2 className="m-0 text-ink">{recipe.title}</h2>
                  <span className="shrink-0 rounded-full border border-line bg-panel-2 px-2 py-0.5 text-[0.72rem] text-muted">
                    {recipe.world}
                  </span>
                </div>
                <p className="text-muted">{recipe.summary}</p>
                <p className="m-0 text-sm text-dim">
                  {recipe.course} — {describeCourse(recipe.course)} · serves {recipe.serves}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
