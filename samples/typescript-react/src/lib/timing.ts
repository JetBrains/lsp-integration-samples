/**
 * Explicit resource management (TypeScript 5.2+). `using` declarations call
 * `Symbol.dispose` when the enclosing block exits, including on an early
 * `return` or a thrown error.
 */

export interface Span extends Disposable {
  readonly label: string;
  readonly ms: number;
}

export function span(label: string): Span {
  const started = performance.now();
  return {
    label,
    get ms() {
      return performance.now() - started;
    },
    [Symbol.dispose]() {
      console.debug(`[span] ${label} took ${(performance.now() - started).toFixed(2)}ms`);
    },
  };
}

export function timed<T>(label: string, work: () => T): { value: T; ms: number } {
  using scope = span(label);
  const value = work();
  return { value, ms: scope.ms };
}
