export interface NavItem {
  readonly to: string;
  readonly label: string;
}

/**
 * The `const` modifier on `T` is what keeps the `to` values as literals at the
 * call site. Drop it and they widen to `string`, and the router can no longer
 * tell whether a link points anywhere real.
 */
export function defineNav<const T extends readonly NavItem[]>(items: T): T {
  return items;
}
