# Bazel LSP

This sample integrates [bazel-lsp](https://github.com/cameron-martin/bazel-lsp)
with IntelliJ IDEA through the
[LSP API](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html).
The complete integration lives in the `frontend` content module.

The language server handles `BUILD`, `WORKSPACE`, `*.bazel`, `*.bzl`, `*.star`,
and `*.sky` files. It invokes the `bazel` executable to resolve labels and external
repositories, so `bazel` (or `bazelisk` aliased as `bazel`) must be available on the
execution host's `PATH`.

## Module layout

- `frontend` contains the LSP provider, executable lookup, remote-host transfer,
  and frontend registration.
- The root project contains the plugin metadata and icon and assembles the native
  plugin distributions. Each ZIP also includes Linux x86_64 and ARM64 servers for
  execution on a remote Linux host.
- There are no shared or backend modules.

```text
.
├── frontend/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/                 LSP integration
│       └── resources/
│           └── bazel-lsp-support.frontend.xml
├── src/main/resources/META-INF/
│   ├── plugin.xml                  Plugin metadata and content module declaration
│   └── pluginIcon.svg
├── build.gradle.kts                Plugin assembly and native variants
└── settings.gradle.kts             Repositories and frontend module inclusion
```

## Build and run

Run `./gradlew buildPluginVariants` from the plugin root to build the native ZIPs
in `build/distributions/`. The build downloads the server binaries from GitHub
releases using the repository declared in [settings.gradle.kts](settings.gradle.kts).
The build renames each downloaded executable to `bazel-lsp` (`bazel-lsp.exe` on
Windows) for both native ZIPs and the development sandbox.

Install the ZIP for your frontend's platform. Each supported native ZIP includes
its server at the plugin root, plus Linux servers in `linux-x64/` and `linux-arm64/`.
The frontend selects the server for
the project's execution host and transfers that executable through Eel when needed.
The generic ZIP includes only the Linux servers.

Upstream publishes no Windows ARM64 binary. Gradle emits a Windows ARM64 ZIP
without a local Windows ARM64 server, and CI does not publish it.

Run `./gradlew runIdeSplitMode` from the plugin root to start the split-mode IDE pair.
The plugin is installed on the frontend, matching the Lua sample's module setup.
