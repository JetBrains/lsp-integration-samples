# Bazel LSP

This sample integrates [bazel-lsp](https://github.com/cameron-martin/bazel-lsp)
with IntelliJ IDEA using the [LSP API](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html).
The complete integration lives in the `frontend` content module.

The language server handles `BUILD`, `WORKSPACE`, `*.bazel`, `*.bzl`, `*.star`,
and `*.sky` files.

`bazel` must be on the execution host's `PATH` (inside the backend for remote
projects). To use Bazelisk, set `--bazel bazelisk` in Language Servers settings.

## Module layout

- `frontend` contains the LSP provider, server settings, executable lookup,
  remote-host transfer, and frontend registration.
- The root project assembles native plugin ZIPs, each including Linux x86_64 and
  ARM64 servers for remote execution.
- There are no shared or backend modules.

## Build and install

Run `./gradlew buildPluginVariants` from the plugin root and install the ZIP for
your frontend's platform from `build/distributions/`. Windows ARM64 has no bundled
native server.
