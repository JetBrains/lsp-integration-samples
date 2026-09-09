import { Link, createFileRoute } from "@tanstack/react-router";

import { COURSES, byCourse, recipes } from "../lib/recipes";
import { describeCourse } from "../lib/type-demos";

export const Route = createFileRoute("/")({
  component: Overview,
});

function Overview() {
  const grouped = byCourse();

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Field notes, cycles 6 through 15</p>
        <h1>
          Cooking that needs <span className="accent">a trench, a vent or a comet</span>.
        </h1>
        <p className="lede">
          A collection of dishes from worlds where the kitchen is the landscape. Nothing here can be
          made at a domestic stove: one recipe is thickened by four hundred fathoms of water
          pressure, another is baked by starlight, and one is ruined if anyone in the room speaks.
        </p>
        <div className="row">
          <Link to="/recipes" className="button">
            Browse {recipes.length} recipes
          </Link>
        </div>
      </section>

      <section className="cards">
        <article className="card">
          <h2>How to read a recipe here</h2>
          <ul className="tight">
            <li>
              <strong>Cycle</strong> — the Galactic Standard cycle the recipe was first written
              down, not when the dish was invented.
            </li>
            <li>
              <strong>World</strong> — where it is cooked, which is usually also the only place the
              ingredients keep.
            </li>
            <li>
              <strong>Method</strong> — the short form the local kitchens actually use. The notes
              above it are what goes wrong.
            </li>
          </ul>
          <p className="muted">
            Quantities are given where they matter and left out where they do not. On Ptolem the
            timing is the recipe; on Ashfall nobody has ever measured anything.
          </p>
        </article>

        <article className="card">
          <h2>The collection by course</h2>
          <ul className="tight">
            {COURSES.map((course) => {
              const bucket = grouped[course] ?? [];
              return (
                <li key={course}>
                  <strong>
                    {bucket.length} {course}
                    {bucket.length === 1 ? "" : "s"}
                  </strong>{" "}
                  — {describeCourse(course).toLowerCase()}
                </li>
              );
            })}
          </ul>
          <p className="muted">
            Most are cooked for a table rather than a person. The nine-moon stew serves twenty and
            cannot sensibly be made for fewer.
          </p>
        </article>
      </section>
    </>
  );
}
