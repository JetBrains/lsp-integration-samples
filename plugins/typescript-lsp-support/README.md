# TypeScript LSP

This sample integrates the native [TypeScript 7 language server](https://github.com/microsoft/TypeScript/releases/tag/v7.0.2)
with IntelliJ IDEA using the [LSP API](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html).
It runs `tsc --lsp --stdio` for `.ts`, `.tsx`, `.mts`, `.cts`, `.js`, `.jsx`, `.mjs`, and `.cjs` files.

Arguments, file patterns, environment variables, and initialization options can be changed in Language Servers settings.

## Module layout

- `frontend` contains the LSP provider, server settings, executable lookup, and remote-host deployment through Eel.
- The root project packages the server into native plugin ZIPs for Linux, macOS, and Windows, on x64 and ARM64.
- Every ZIP also includes both Linux distributions for remote execution, with their TypeScript declaration files.

## Build and install

Run from this directory:

```shell
./gradlew buildPluginVariants
```

Install the ZIP for your frontend's OS and architecture from `build/distributions/`.
The server is bundled; no runtime download is needed.

To update the server version, change `typescriptVersion` in [build.gradle.kts](build.gradle.kts).
