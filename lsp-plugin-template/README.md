# LSP plugin template

## Overview

This template is a starting point for adding language support to IntelliJ-based IDEs through the
[Language Server Protocol (LSP)](https://microsoft.github.io/language-server-protocol/).

Supply a language server and complete the TODOs to connect it to the IDE and configure its settings.
The modular structure supports both **split mode** and the regular IDE.

See the [existing plugins in this repository](https://github.com/JetBrains/lsp-integration-samples/tree/main/plugins)
for examples.

## Module layout

In split mode, the IDE runs as separate frontend and backend processes.
The plugin's content modules determine where its code is loaded.

- The **root project** configures the IntelliJ Platform dependency, enables split mode,
  and packages the `frontend` content module into a single plugin.
- The **`frontend` module** contains the LSP integration and settings provider.
  In split mode, it is installed and loaded in the frontend IDE process.
  In the monolithic IDE, the same module runs locally.

See the [split mode documentation](https://plugins.jetbrains.com/docs/intellij/split-mode-and-remote-development.html) for details.

## Customize

1. Copy this directory and open the copy as a Gradle project with JDK 21.
2. Search for `TODO` and replace the example names:
   - Set the plugin ID, name, vendor, and description in `src/main/resources/META-INF/plugin.xml`.
   - Rename `org.jetbrains.lsp.samples.template` and the `TemplateLsp*` classes. Update the registrations in
     `frontend/src/main/resources/template.lsp.frontend.xml`.
   - Rename `template.lsp` in `settings.gradle.kts`. Set the content module name in
     `plugin.xml` to `<project-name>.frontend` and rename its XML file to `<project-name>.frontend.xml`.
   - Set the group and version in `build.gradle.kts`.
3. Supply the language server: bundle it with the plugin, download it when needed, or use an existing installation.
   For remote development, make the server executable available on the project execution host.
4. Complete `TemplateLspServerSettingsProvider`: set a stable ID unique among the project's
   LSP servers, a display name, and file patterns. The server ID is independent of the plugin ID.
   Add server arguments or initialization options if needed.
5. Implement `TemplateLspIntegrationProvider.fileOpened()` to start the LSP client for supported files.
6. Replace the executable placeholder in `TemplateLspServerDescriptor.createCommandLine()`.
   For remote development, resolve the executable on the project execution host.
7. Register file types in the frontend XML if the IDE does not recognize your language.

File patterns are empty and `fileOpened` is a stub, so the template does not start a server until customized.
The provider uses the platform's settings UI and applies project overrides for file patterns,
arguments, environment variables, and initialization options.

## Build and run

This template uses the IntelliJ Platform 263 APIs and the
IntelliJ Platform Gradle Plugin snapshot configured in `settings.gradle.kts`.
Choose a compatible IDE version in `build.gradle.kts` when adapting it.

```shell
./gradlew buildPlugin
./gradlew runIde
./gradlew runIdeSplitMode
```

The plugin ZIP is in `build/distributions/`. Use `gradlew.bat` on Windows.

For general template conventions, see the JetBrains
[plugin template](https://github.com/JetBrains/intellij-platform-plugin-template)
and [modular plugin template](https://github.com/JetBrains/intellij-platform-modular-plugin-template).
