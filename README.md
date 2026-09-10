# LSP Integration Samples

This repository contains examples for LSP integration in IntelliJ IDEA:

- `samples/` — sample projects for integration and performance testing.
- `plugins/` — IntelliJ Platform plugin examples for LSP servers.

## Samples

Sample projects from existing open-source repositories:

| Language | Project | Source |
| --- | --- | --- |
| Lua | [`samples/lua/inspect.lua`](samples/lua/inspect.lua) | [kikito/inspect.lua](https://github.com/kikito/inspect.lua) |
| Lua | [`samples/lua/luarocks`](samples/lua/luarocks) | [luarocks/luarocks](https://github.com/luarocks/luarocks) |
| Dart | [`samples/dart/equatable`](samples/dart/equatable) | [felangel/equatable](https://github.com/felangel/equatable) |
| Swift | [`samples/swift/swift-tagged`](samples/swift/swift-tagged) | [pointfreeco/swift-tagged](https://github.com/pointfreeco/swift-tagged) |

## Plugins

### Lua LSP support

[`plugins/lua-lsp-support`](plugins/lua-lsp-support) is an IntelliJ Platform
plugin that integrates the [LuaLS language server](https://github.com/LuaLS/lua-language-server)
with IntelliJ IDEA.

### Bazel LSP support

[`plugins/bazel-lsp-support`](plugins/bazel-lsp-support) is an IntelliJ Platform
plugin that integrates the [bazel-lsp language server](https://github.com/cameron-martin/bazel-lsp)
with IntelliJ IDEA. It handles `BUILD`, `WORKSPACE`, `*.bazel`, `*.bzl`,
`*.star`, and `*.sky` files, and requires the `bazel` executable on the `PATH`.

### TypeScript LSP support

[`plugins/typescript-lsp-support`](plugins/typescript-lsp-support) is an IntelliJ Platform
plugin that integrates the native [TypeScript language server](https://github.com/microsoft/TypeScript)
with IntelliJ IDEA.

### Tailwind CSS LSP support

[`plugins/tailwind-lsp-support`](plugins/tailwind-lsp-support) is an IntelliJ Platform
plugin that integrates the [Tailwind CSS language server](https://github.com/tailwindlabs/tailwindcss-intellisense)
with IntelliJ IDEA. It handles `css`, `scss`, `less`, `html`, `js`, `jsx`, `ts`, and `tsx`
files. The `@tailwindcss/language-server` npm package is bundled at build time; it is a
platform-independent JS bundle, started with `npx` from the IDE's managed Node.js runtime,
which the plugin downloads on demand.

### Download a plugin build

Each plugin has a workflow that uploads platform-specific plugin ZIPs as GitHub
Actions artifacts:

- [Build Lua LSP plugin](https://github.com/JetBrains/lsp-integration-samples/actions/workflows/build-lua-plugin.yml)
- [Build Bazel LSP plugin](https://github.com/JetBrains/lsp-integration-samples/actions/workflows/build-bazel-plugin.yml)
- [Build TypeScript LSP plugin](https://github.com/JetBrains/lsp-integration-samples/actions/workflows/build-typescript-plugin.yml)
- [Build Tailwind CSS LSP plugin](https://github.com/JetBrains/lsp-integration-samples/actions/workflows/build-tailwind-plugin.yml)

Open a successful workflow run and download the artifact for your platform:

| Platform | Lua artifact | Bazel artifact | TypeScript artifact |
| --- | --- | --- | --- |
| Linux ARM64 | `lua-lsp-linux-arm64` | `bazel-lsp-linux-arm64` | `typescript-lsp-linux-arm64` |
| Linux x86_64 | `lua-lsp-linux-x86_64` | `bazel-lsp-linux-x86_64` | `typescript-lsp-linux-x86_64` |
| macOS ARM64 | `lua-lsp-mac-arm64` | `bazel-lsp-mac-arm64` | `typescript-lsp-mac-arm64` |
| macOS x86_64 | `lua-lsp-mac-x86_64` | `bazel-lsp-mac-x86_64` | `typescript-lsp-mac-x86_64` |
| Windows ARM64 | — | — | `typescript-lsp-windows-arm64` |
| Windows x86_64 | `lua-lsp-windows-x86_64` | `bazel-lsp-windows-x86_64` | `typescript-lsp-windows-x86_64` |

The Tailwind CSS plugin's server is platform-independent, so its workflow uploads a
single `tailwind-lsp` artifact that works on every platform.

After downloading and extracting the artifact, install the plugin from the
ZIP file using **Settings | Plugins | ⚙ | Install Plugin from Disk** in
IntelliJ IDEA.
