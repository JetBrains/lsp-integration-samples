export type Course = "starter" | "main" | "dessert";

export const COURSES = ["starter", "main", "dessert"] as const satisfies readonly Course[];

export interface Recipe {
  readonly slug: string;
  readonly title: string;
  readonly course: Course;
  /** Galactic Standard cycle the recipe was first written down. */
  readonly cycle: `${number}.${number}`;
  readonly world: string;
  readonly serves: number;
  readonly summary: string;
  readonly notes: readonly string[];
  readonly method?: string;
}

/**
 * `as const` keeps every literal, `satisfies` checks the shape without widening
 * it. That combination is what makes `KnownSlug` below a union of the actual
 * slugs rather than plain `string`.
 */
export const recipes = [
  {
    slug: "nebula-broth",
    title: "Nebula broth",
    course: "starter",
    cycle: "11.2",
    world: "Vess Deep",
    serves: 6,
    summary:
      "A clear broth that is thickened by pressure instead of flour. Sealed in a stone flask and dropped to the trench floor, it comes back up cloudy and thick and stays that way as long as it is warm.",
    notes: [
      "The trench cooks do the drop at four hundred fathoms; shallower and the broth never sets, deeper and it turns to jelly you have to cut.",
      "It loses its body the moment it cools, so it is ladled at the table and never reheated.",
      "Vess kitchens keep the same flask for a lifetime. A new flask gives a thin first batch that nobody serves to guests.",
    ],
    method: `Fill the flask with sunfish stock and three pinches of grey salt.
Seal it, weight it, and lower it to four hundred fathoms.
Leave it for one tide.
Raise it slowly — a fast raise beats the air back in.
Pour at the table, still hot.`,
  },
  {
    slug: "salt-comet-caviar",
    title: "Salt-comet caviar",
    course: "starter",
    cycle: "14.0",
    world: "The Long Tail",
    serves: 2,
    summary:
      "Beads of frozen brine skimmed off a comet's trailing edge, served on a slate chilled to the same temperature so they hold their shape long enough to reach the mouth.",
    notes: [
      "Only the outer third of the tail is worth taking. Closer in, the beads have already been through one thaw and taste flat.",
      "Serve on slate, never on metal — metal pulls the cold out from underneath and the beads slump.",
      "There is no seasoning. The brine is the seasoning, and it changes with every pass the comet makes.",
    ],
  },
  {
    slug: "singing-kelp-rolls",
    title: "Singing kelp rolls",
    course: "starter",
    cycle: "9.6",
    world: "Hollow Tide",
    serves: 4,
    summary:
      "Kelp with hollow stems that hum when steam moves through them. The pitch falls as the leaf softens, and the cook listens rather than looks to know when the rolls are done.",
    notes: [
      "Ready is a low, flat note. A note that wavers means one roll in the basket is still raw.",
      "Roll them loosely. A tight roll cannot breathe and stays silent all the way through cooking.",
      "Hollow Tide apprentices spend their first season doing nothing but listening to a basket.",
    ],
    method: `Cut six stems a hand long, split, and flatten.
Fill with smoked roe and shredded stalk.
Roll loose, seam down, in a woven basket.
Steam over boiling seawater.
When the hum drops to a low flat note, take the basket off.`,
  },
  {
    slug: "gravity-well-dumplings",
    title: "Gravity-well dumplings",
    course: "main",
    cycle: "12.4",
    world: "Kerrin Shelf",
    serves: 8,
    summary:
      "Dumplings folded around a mixed filling and spun in a centrifuge before they are cooked, so the filling settles into bands. Each bite through the middle crosses all of them.",
    notes: [
      "Spin at a steady rate. Any wobble smears two bands into each other and the bite tastes like one thing.",
      "The order of the bands is the recipe. Fat on the outside, meat in the middle, sour at the centre.",
      "Kerrin cooks argue about spin time the way other worlds argue about salt.",
    ],
    method: `Mix the filling loosely — do not knead it, the bands need room to move.
Fold twenty dumplings, pleat up.
Load the centrifuge, pleats facing out.
Spin at a steady rate for a quarter shift.
Steam upright, and do not let them touch.`,
  },
  {
    slug: "nine-moon-stew",
    title: "Nine-moon stew",
    course: "main",
    cycle: "6.1",
    world: "Ptolem",
    serves: 20,
    summary:
      "The oldest dish on Ptolem and the slowest. One ingredient goes in at each moonrise, in a fixed order, over a single long night. Nothing is stirred until the ninth moon is up.",
    notes: [
      "Miss a moonrise and the batch is finished — you serve what you have and start again the next night.",
      "The order is not about flavour, it is about how long each thing needs. Root first, leaf last.",
      "It is cooked for gatherings, never for one household, because nobody wants to sit up nine risings alone.",
    ],
    method: `First moon: black root, whole.
Second: bone and marrow.
Third: hard grain.
Fourth: the fat.
Fifth: sour paste.
Sixth: dried fruit.
Seventh: peppers, split.
Eighth: soft greens.
Ninth: salt, then stir once and serve.`,
  },
  {
    slug: "ember-lichen-flatbread",
    title: "Ember-lichen flatbread",
    course: "main",
    cycle: "13.7",
    world: "Ashfall Terraces",
    serves: 4,
    summary:
      "Lichen scraped off vent rock, worked into a stiff dough, and baked by slapping it straight onto the vent it was scraped from. It cooks in under a minute and has to be eaten inside five.",
    notes: [
      "Scrape from a vent that is warm to the hand but not hot. A cold vent grows lichen with no smoke in it.",
      "The dough should tear rather than stretch. If it stretches, there is too much water and it will slide off the rock.",
      "It goes stale faster than anything else on the terraces, which is why it is never sold, only shared.",
    ],
    method: `Scrape two handfuls of lichen; discard the grey underside.
Work into coarse flour with just enough water to bind.
Rest the dough while you find a flat piece of vent rock.
Slap the round on hard so it sticks.
Peel it off when the edges lift, and eat it standing up.`,
  },
  {
    slug: "photon-glazed-thistle",
    title: "Photon-glazed thistle",
    course: "main",
    cycle: "15.1",
    world: "Orbital Halo Nine",
    serves: 3,
    summary:
      "Thistle hearts lacquered by focused starlight rather than heat. The sugars on the surface set into a thin glass shell while the inside stays raw and cold.",
    notes: [
      "The whole point is the difference between the shell and the middle. Anything that warms the middle has gone wrong.",
      "Halo kitchens work in short passes with the mirror, letting the heart rest between them.",
      "Done properly it cracks audibly under a spoon. If it dents instead, the glaze was too thick.",
    ],
    method: `Trim nine thistle hearts and brush with cane syrup.
Set them on a cold plate under the mirror.
Give them three short passes, resting between each.
Stop while the centre is still cold.
Serve within the quarter hour; the shell draws water and softens.`,
  },
  {
    slug: "methane-frost-sorbet",
    title: "Methane-frost sorbet",
    course: "dessert",
    cycle: "10.3",
    world: "Tiun",
    serves: 6,
    summary:
      "A sorbet whisked in open air on a world cold enough to keep it solid, then carried to the table under a cover. It has to be served below its own freezing point or it collapses into syrup.",
    notes: [
      "Whisk outdoors and keep whisking. A still bowl grows crystals you can feel on the tongue.",
      "The cover is not for warmth, it is to stop frost from the air settling on top and dulling the flavour.",
      "Tiun households judge a host by how fast the bowls get from the bowl to the table.",
    ],
    method: `Take pressed berry juice outside and whisk it in the open.
Keep the bowl moving until it holds a peak.
Scoop into chilled cups and cover each one.
Carry them out together.
Uncover at the table, all at once.`,
  },
  {
    slug: "hush-fruit-tart",
    title: "Hush-fruit tart",
    course: "dessert",
    cycle: "8.9",
    world: "Mirren Quiet",
    serves: 8,
    summary:
      "Hush fruit gives up its flavour to noise, so the tart is assembled and baked in a sealed room and the cooks work without speaking. Loud kitchens produce a pale, sweet nothing.",
    notes: [
      "The fruit is picked before dawn, when the wind is down. Afternoon fruit is already half spent.",
      "Sealed room, soft shoes, no talking. The old kitchens were built underground for exactly this.",
      "It is eaten in silence too, which visitors find harder than the cooking.",
    ],
    method: `Line the tin and blind-bake it before the fruit comes in.
Seal the room. Stop talking.
Halve the fruit, cut side up, and pack it tight.
Pour over thin cream and dust with dark sugar.
Bake until the cream barely holds, then carry it out still sealed.`,
  },
] as const satisfies readonly Recipe[];

export type KnownSlug = (typeof recipes)[number]["slug"];

/** Mapped type with an `as` clause: rekeys the tuple by slug. */
export type RecipeBySlug = {
  readonly [R in (typeof recipes)[number] as R["slug"]]: R;
};

export function findRecipe(slug: string): Recipe | undefined {
  return recipes.find((recipe) => recipe.slug === slug);
}

export function byCourse(): Partial<Record<Course, readonly Recipe[]>> {
  return Object.groupBy(recipes, (recipe) => recipe.course);
}

export function matches(recipe: Recipe, query: string): boolean {
  if (query === "") return true;
  const needle = query.toLowerCase();
  return (
    recipe.title.toLowerCase().includes(needle) ||
    recipe.summary.toLowerCase().includes(needle) ||
    recipe.world.toLowerCase().includes(needle) ||
    recipe.slug.includes(needle)
  );
}
