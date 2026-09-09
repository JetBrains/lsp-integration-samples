declare const brand: unique symbol;

/**
 * A nominal wrapper around a structural type. `Brand<string, "Slug">` is still
 * a string at runtime, but a plain string will not be accepted where one is
 * expected — you have to go through the smart constructor.
 */
export type Brand<T, B extends string> = T & { readonly [brand]: B };

export type Slug = Brand<string, "Slug">;

const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export function isSlug(value: string): value is Slug {
  return SLUG.test(value);
}

export function asSlug(value: string): Slug {
  if (!isSlug(value)) {
    throw new TypeError(`not a slug: ${JSON.stringify(value)}`);
  }
  return value;
}
