---
name: create-lsp-plugin
description: Create an IntelliJ Platform language support plugin from JetBrains' LSP template, reusing its built-in settings support. Covers server setup, plugin metadata, packaging, and IDE verification.
---

# Create an LSP plugin

Create a standalone plugin from `lsp-plugin-template/` in
[JetBrains/lsp-integration-samples](https://github.com/JetBrains/lsp-integration-samples).
Use the current template and examples as source material; this skill does not embed a second copy.
Work in the user's destination, leaving the source template unchanged.

## Defaults: keep the plugin close to the template

- Reuse `LspIntegrationSettingsProvider`, `LspPluginServerConfiguration`, and the platform's LSP
  settings UI. **Do not create a separate settings page unless the user explicitly requests one.**
  This includes `Configurable`/`BoundConfigurable` classes, `projectConfigurable` or
  `applicationConfigurable` registrations, and custom persistence that duplicates platform LSP settings.
  Choosing a user-installed server does not imply a request for a path picker or settings page.
- Implement the selected server setup with the smallest necessary changes. Add helpers only for
  concrete needs; avoid speculative discovery frameworks, setup wizards, custom actions, retry
  systems, and support for additional installation methods or platforms.
- Use a clear error and documented remedy for a missing prerequisite. Add an installer or downloader
  only when it is part of the chosen provisioning strategy. Required runtime support still needs
  to work; keeping the plugin small does not mean leaving required TODOs or broken launch paths.

## 1. Establish the language and server setup

Infer what you can from the request and project. Establish only what is needed to implement the plugin:

- Language, supported extensions and special filenames, and a small project suitable for testing.
- Destination directory, plugin name, unique plugin ID, vendor, description, and initial version.
  Suggest technical names when absent; ask for ownership/vendor details instead of attributing the plugin to JetBrains.
- Server implementation, upstream documentation, version, LSP transport and launch command.
  A compiler or CLI for the language is not necessarily its language server.
- How the **server** is supplied: bundled in the plugin, downloaded on demand, or installed by the user/project.
- Separately, how its **runtime and other tools** are supplied: Node.js, .NET, Java, Python,
  a compiler, SDK, or a tool such as `bazel`. Establish required versions and whether the server
  distribution is self-contained. An npm package may contain a native binary; a DLL may require .NET.
- Target IDE/build, supported OS/CPU combinations, and whether remote development must work.

Use the server the user names. Otherwise start with the language's official or usual server;
ask a focused question if there is a meaningful choice. Check its upstream launch/setup documentation
and the closest relevant repository example. Stop researching once the entry point, required
arguments, runtime prerequisites, and delivery method are known. Do not survey alternatives or
search the user's machine for installations without a concrete need.

Read the relevant parts of [server-setup.md](references/server-setup.md) for the chosen approach.
If setup remains unclear, ask about that specific gap rather than inventing fallback strategies.
Metadata and the clean copy can proceed while awaiting the answer.

Record the decisions, exact command structure, runtime requirements, and supported platforms in the
generated plugin's README. Distinguish what the plugin supplies from what users must install.

## 2. Obtain and copy a clean template

Use an available checkout of the samples repository if it contains the template; do not assume the
installed skill is inside that checkout. Otherwise obtain it from the canonical repository in a
temporary directory. A sparse checkout avoids downloading unrelated sample projects:

```shell
git clone --depth 1 --filter=blob:none --sparse https://github.com/JetBrains/lsp-integration-samples.git <samples-checkout>
git -C <samples-checkout> sparse-checkout set lsp-plugin-template plugins
```

Honor a requested revision and record the source commit used. Read the template README, Gradle
files, both XML descriptors, and both Kotlin files. Read applicable repository instructions too.

Run the bundled helper, resolving its path relative to this `SKILL.md`:

```shell
python3 <skill-directory>/scripts/copy_template.py --source <samples-checkout> --destination <new-plugin-directory>
```

The destination must not already exist. If the user chose an existing project, inspect it and merge
the necessary source files deliberately instead of overwriting it. The helper retains the Gradle
wrapper JAR and scripts while excluding IDE settings, build output, and generated caches at every level.
Never copy `.intellijPlatform`, `.gradle`, `.kotlin`, or `build` from a working template checkout.

## 3. Customize every template component

Choose related but distinct identifiers: plugin ID, Gradle project name, Kotlin package, class prefix,
and LSP server ID. Keep a consistent mapping while editing. For a Gradle project named
`acme-language-lsp`, the content module is `acme-language-lsp.frontend`, and its descriptor is
`frontend/src/main/resources/acme-language-lsp.frontend.xml`.

| File in the copied template | Required changes |
| --- | --- |
| `src/main/resources/META-INF/plugin.xml` | Set a unique plugin ID, display name, real vendor, and useful description, including server/runtime setup. Update the required content module name. |
| `settings.gradle.kts` | Set `rootProject.name`; retain `include("frontend")`. Add only the repositories needed to obtain the chosen server/runtime artifacts. |
| `build.gradle.kts` | Set group and plugin version; choose a compatible IDE dependency and `sinceBuild`; implement server packaging when needed. Preserve frontend module packaging and split-mode configuration for supported targets. |
| `frontend/build.gradle.kts` | Retain the LSP module dependencies; add dependencies required by the actual implementation. |
| Kotlin package directories and both `TemplateLsp*.kt` files | Rename directories, packages, files, provider/settings/descriptor classes, and all references. |
| `frontend/src/main/resources/template.lsp.frontend.xml` | Rename to match the content module; update both implementation class names. Add file type registration only when the target IDE does not recognize the language. |
| `*LspServerSettingsProvider.kt` | Customize the existing provider's server ID, display name, file patterns, arguments, environment defaults, and initialization options as needed. Keep the platform settings integration; no separate settings page by default. |
| `*LspIntegrationProvider.kt` | Implement file matching, client startup, executable/runtime resolution, command construction, and actionable setup errors. |
| `README.md` | Replace template instructions with plugin purpose, supported files/features, setup, platforms, build/install/run instructions, and a concrete verification procedure. |

The template currently uses platform **263 APIs** and the IntelliJ Platform Gradle Plugin snapshot
declared in `settings.gradle.kts`. Check the checked-out versions and API sources before adapting
examples. Do not lower compatibility bounds merely to make installation possible. Prefer a tested,
explicit IDE version for the generated plugin and document any snapshot dependency. Use a JDK
compatible with the chosen IDE and Gradle versions; the current template README specifies JDK 21.

For the integration provider:

1. Construct the renamed descriptor in `fileOpened`; check `descriptor.isSupportedFile(file)` and
   call `clientStarter.ensureClientStarted(descriptor)` only for matching files and ready prerequisites.
   Use the same effective file patterns for startup and document support so user overrides work.
2. Keep `LspIntegrationSettings.getInstanceOrNull(project)?.getPluginConfiguration(SERVER_ID)` and
   the fallback configuration. Preserve `configuration.arguments`,
   `configuration.environmentVariables.configureCommandLine(...)`, and
   `configuration.createInitializationOptions()` when implementing the command.
3. Resolve real server and runtime paths. Pass the executable and individual arguments to
   `GeneralCommandLine`; include required server entry points and LSP mode flags exactly once.
   Set the project working directory if the server needs it. Keep protocol stdout free of log output.
4. Use platform LSP lifecycle management instead of spawning a separate unmanaged server per file.
   If the selected strategy requires downloads/runtime setup, use the appropriate existing platform
   mechanism in the background. Otherwise a clear missing-prerequisite error is sufficient.
5. Keep integration in the frontend content module. In remote development, resolve OS, architecture,
   paths, environment, working directory, and executables for the **project execution host**.
   Follow the deployment guidance in [server-setup.md](references/server-setup.md); a frontend-local
   installation path alone is insufficient.

Inspect existing file types and dependencies before registering new ones. The template's commented
TextMate registration is an example, not proof that TextMate is available in every target IDE.
Do not claim syntax highlighting or other features solely because an LSP server starts.

Search the generated sources, resources, Gradle files, and README for `TODO`, `TemplateLsp`,
`org.jetbrains.lsp.samples.template`, `template.lsp`, `lsp.plugin.template`,
`template-language-server`, `Your Language`, `Your language`, `Your name`, and
`path/to/language-server`. Resolve every template placeholder, including optional decisions;
do not just delete TODO markers while leaving stubs. Preserve third-party copyright/license notices.

## 4. Build, inspect, and exercise the plugin

Follow [verification.md](references/verification.md). It includes Gradle commands, ZIP/JAR inspection,
server/runtime checks, regular and split IDE runs, and an observable language-feature smoke test.
Run the checks relevant to the implemented setup and fix failures within that design. Do not create
extra product features or a broad test framework merely to satisfy optional verification scenarios.
Treat build success, correct packaging, server startup, and working IDE features as separate results.

If network access, a runtime, another OS, or an interactive IDE is unavailable, finish the independent
checks and report exactly which check is blocked or untested. Do not report an interactive smoke test
as passed based on compilation or a server version command. Ask the user to perform only the remaining
interactive steps when you cannot operate the IDE yourself.

## 5. Deliver the generated plugin

Provide the project location and built ZIP(s), plugin ID/name, server/runtime supply strategy,
supported targets, commands actually run and their outcomes, and any remaining verification steps.
Include concise setup and smoke-test instructions with expected behavior in the plugin README;
reuse a small existing example project where practical.
Do not publish to Marketplace or another repository as part of scaffolding unless requested.
