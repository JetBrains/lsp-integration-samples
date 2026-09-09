/**
 * `using` is downlevelled by esbuild into a helper that looks the disposal
 * method up via `Symbol.dispose`, falling back to `Symbol.for("Symbol.dispose")`.
 * Engines that predate the proposal have neither, so define them here — the
 * same registry symbol the helper falls back to, which keeps our object
 * literals and the helper in agreement.
 *
 * Import this before anything that uses `using`.
 */

function defineWellKnown(name: "dispose" | "asyncDispose"): void {
  if (!(name in Symbol)) {
    Object.defineProperty(Symbol, name, { value: Symbol.for(`Symbol.${name}`) });
  }
}

defineWellKnown("dispose");
defineWellKnown("asyncDispose");
