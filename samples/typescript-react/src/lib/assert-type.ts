/**
 * Compile-time assertions. These produce no JavaScript at all — they exist so
 * that `tsc --noEmit` has real work to do, and so a regression in the type-level
 * code below shows up as an error instead of silently passing.
 */

export type Equal<A, B> =
  (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;

export type Expect<T extends true> = T;

export type Extends<A, B> = A extends B ? true : false;
