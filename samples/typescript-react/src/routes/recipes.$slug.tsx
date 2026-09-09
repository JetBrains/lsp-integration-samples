import { useReducer } from "react";
import { Link, createFileRoute, notFound } from "@tanstack/react-router";

import { isSlug } from "../lib/brand";
import { findRecipe } from "../lib/recipes";
import type { Recipe } from "../lib/recipes";
import { timed } from "../lib/timing";
import { assertNever, describeCourse } from "../lib/type-demos";

export const Route = createFileRoute("/recipes/$slug")({
  loader: ({ params }) => {
    // A slug has to look like one before it is worth a lookup. `isSlug` narrows
    // the plain string to the branded `Slug` that `findRecipe` expects.
    if (!isSlug(params.slug)) {
      throw notFound();
    }
    // `timed` uses a `using` declaration internally; the span is disposed before
    // this call returns, and it logs to the console on the way out.
    const { value: recipe } = timed(`lookup ${params.slug}`, () => findRecipe(params.slug));
    if (!recipe) {
      throw notFound();
    }
    return { recipe };
  },
  component: RecipeDetail,
  notFoundComponent: () => (
    <section className="rounded-card border border-line bg-panel p-5">
      <h1>Unknown dish</h1>
      <p className="text-muted">No recipe has that slug.</p>
      <Link to="/recipes" className="button">
        Back to the list
      </Link>
    </section>
  ),
});

interface ServingsState {
  readonly serves: number;
  readonly base: number;
}

type ServingsAction =
  | { readonly type: "step"; readonly by: number }
  | { readonly type: "set"; readonly serves: number }
  | { readonly type: "reset" };

function reduce(state: ServingsState, action: ServingsAction): ServingsState {
  switch (action.type) {
    case "step":
      return { ...state, serves: Math.max(1, state.serves + action.by) };
    case "set":
      return { ...state, serves: Math.max(1, action.serves) };
    case "reset":
      return { ...state, serves: state.base };
    default:
      // Add a variant to `ServingsAction` without handling it and this stops compiling.
      return assertNever(action);
  }
}

function initialServings(recipe: Recipe): ServingsState {
  return { serves: recipe.serves, base: recipe.serves };
}

function RecipeDetail() {
  const { recipe } = Route.useLoaderData();
  const [servings, dispatch] = useReducer(reduce, recipe, initialServings);

  const factor = servings.serves / servings.base;

  return (
    <article className="flex flex-col gap-6">
      <Link to="/recipes" className="text-sm text-muted">
        ← All recipes
      </Link>

      <header className="border-b border-line pb-5">
        <div className="mb-2 flex items-baseline justify-between gap-4">
          <h1 className="m-0">{recipe.title}</h1>
          <span className="shrink-0 rounded-full border border-line bg-panel-2 px-2 py-0.5 text-[0.72rem] text-muted">
            cycle {recipe.cycle}
          </span>
        </div>
        <p className="text-sm text-dim">
          {recipe.world} · {recipe.course} — {describeCourse(recipe.course)}
        </p>
        <p className="m-0 max-w-[68ch] text-muted">{recipe.summary}</p>
      </header>

      <section className="rounded-card border border-line bg-panel p-5">
        <h2>Notes</h2>
        <ul className="tight m-0">
          {recipe.notes.map((note) => (
            <li key={note}>{note}</li>
          ))}
        </ul>
      </section>

      {recipe.method !== undefined && (
        <section className="rounded-card border border-line bg-panel p-5">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <h2 className="m-0 mr-auto">Method</h2>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                aria-label="Fewer servings"
                className="size-7 rounded-lg border border-line text-muted hover:border-line-lit hover:text-ink"
                onClick={() => dispatch({ type: "step", by: -1 })}
              >
                −
              </button>
              <span className="min-w-24 text-center font-mono text-sm tabular-nums text-ink">
                serves {servings.serves}
              </span>
              <button
                type="button"
                aria-label="More servings"
                className="size-7 rounded-lg border border-line text-muted hover:border-line-lit hover:text-ink"
                onClick={() => dispatch({ type: "step", by: 1 })}
              >
                +
              </button>
            </div>
            {factor === 1 ? (
              <span className="text-xs text-dim">as written</span>
            ) : (
              <button
                type="button"
                className="rounded-full border border-accent bg-accent-wash px-3 py-1 text-xs font-medium text-accent"
                onClick={() => dispatch({ type: "reset" })}
              >
                ×{factor.toFixed(2)} — back to {servings.base}
              </button>
            )}
          </div>
          <pre>
            <code>{recipe.method}</code>
          </pre>
          {factor !== 1 && (
            <p className="mt-3 mb-0 text-sm text-dim">
              Quantities above are written for {servings.base}. Scale anything countable by{" "}
              {factor.toFixed(2)}; timings and depths stay as they are.
            </p>
          )}
        </section>
      )}
    </article>
  );
}
