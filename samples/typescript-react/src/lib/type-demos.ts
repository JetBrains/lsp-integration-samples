import type { Equal, Expect } from "./assert-type";
import type { Brand, Slug } from "./brand";
import type { Course, KnownSlug, RecipeBySlug } from "./recipes";

/* ------------------------------------------------------------------ *
 * Recursive template literal types: parsing a route path
 * ------------------------------------------------------------------ */

export type Segments<S extends string> = S extends `${infer Head}/${infer Rest}`
  ? Head extends ""
    ? Segments<Rest>
    : [Head, ...Segments<Rest>]
  : S extends ""
    ? []
    : [S];

type ParamName<S extends string> = S extends `$${infer Name}` ? Name : never;

/** `"/recipes/$slug"` → `{ slug: string }`, computed by the checker alone. */
export type RouteParams<S extends string> = {
  [K in ParamName<Segments<S>[number]>]: string;
};

export type Check_Segments = Expect<
  Equal<Segments<"/recipes/$slug/method">, ["recipes", "$slug", "method"]>
>;
export type Check_RouteParams = Expect<Equal<RouteParams<"/recipes/$slug">, { slug: string }>>;
export type Check_NoParams = Expect<Equal<RouteParams<"/recipes">, {}>>;

/* ------------------------------------------------------------------ *
 * `as const satisfies` keeps the literals reachable
 * ------------------------------------------------------------------ */

export type Check_KnownSlug = Expect<
  Extract<KnownSlug, "nine-moon-stew"> extends never ? false : true
>;
export type Check_SlugIsNotString = Expect<Equal<Equal<KnownSlug, string>, false>>;
export type Check_MappedRekey = Expect<Equal<RecipeBySlug["nine-moon-stew"]["cycle"], "6.1">>;

/* ------------------------------------------------------------------ *
 * Branding is not structural
 * ------------------------------------------------------------------ */

export type Check_SlugIsAString = Expect<Slug extends string ? true : false>;
export type Check_StringIsNotASlug = Expect<Equal<string extends Slug ? true : false, false>>;
export type Check_BrandsDiffer = Expect<
  Equal<Equal<Brand<string, "A">, Brand<string, "B">>, false>
>;

/* ------------------------------------------------------------------ *
 * Exhaustiveness
 * ------------------------------------------------------------------ */

export function assertNever(value: never): never {
  throw new Error(`unhandled case: ${JSON.stringify(value)}`);
}

export function describeCourse(course: Course): string {
  switch (course) {
    case "starter":
      return "Served while the table is still cold";
    case "main":
      return "The dish the meal is named after";
    case "dessert":
      return "Eaten last, usually in the dark";
    default:
      // Add a member to `Course` and this line stops compiling.
      return assertNever(course);
  }
}
