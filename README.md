# LSP Integration Samples

This repository provides a [plugin template](lsp-plugin-template) and a
[`create-lsp-plugin` Codex skill](skills/create-lsp-plugin/SKILL.md) for creating
IntelliJ Platform plugins based on the Language Server Protocol (LSP).
It also contains [plugin implementations](#plugin-examples) to use as examples
and [sample projects](samples) for testing.

## Use the template

The template includes LSP integration, the platform's settings UI, and support for
regular IDEs and split mode (remote development).

1. Copy [`lsp-plugin-template/`](lsp-plugin-template) into your own project directory
   and open it as a Gradle project with JDK 21.
2. Follow the [customization instructions](lsp-plugin-template/README.md#customize)
   to set your plugin metadata, supply a language server, and complete the TODOs.
3. Run `./gradlew buildPlugin` to build the plugin or `./gradlew runIde` to try it.

See the [template README](lsp-plugin-template/README.md) for IDE compatibility and
split-mode run instructions.

## Use the skill with Codex

The skill guides Codex through creating a plugin from the template, setting up the
server and its runtime, building the plugin, and verifying it in an IDE.

Ask Codex to install it:

```text
Use $skill-installer to install the skill at
https://github.com/JetBrains/lsp-integration-samples/tree/main/skills/create-lsp-plugin
```

Then ask Codex in your destination workspace:

```text
Use $create-lsp-plugin to create an IntelliJ plugin for <language> in <directory>.
Use <language server>. Help me choose how to supply the server and its runtime.
```

Your plugin project can live outside this repository; the skill obtains the
template and examples as needed.

## Plugin examples

Use these implementations as references for connecting a language server,
supplying its runtime, and configuring a plugin:

| Plugin | Language server |
| --- | --- |
| [Lua](plugins/lua-lsp-support) | [LuaLS](https://github.com/LuaLS/lua-language-server) |
| [Bazel](plugins/bazel-lsp-support) | [bazel-lsp](https://github.com/cameron-martin/bazel-lsp) |
| [Bicep](plugins/bicep-lsp-support) | [Bicep](https://github.com/Azure/bicep) |
| [TypeScript](plugins/typescript-lsp-support) | [Native TypeScript](https://github.com/microsoft/TypeScript) |
| [Tailwind CSS](plugins/tailwind-lsp-support) | [Tailwind CSS IntelliSense](https://github.com/tailwindlabs/tailwindcss-intellisense) |
| [Swift](plugins/swift-lsp-support) | [SourceKit-LSP](https://github.com/swiftlang/sourcekit-lsp) |

## Sample projects

[`samples/`](samples) contains projects for trying the plugins and testing LSP
integration and performance.
