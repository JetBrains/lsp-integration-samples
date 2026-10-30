# Set up the chosen server

## Keep discovery small

Implement one documented lookup method for the chosen delivery strategy:

- **Bundled:** Resolve the known entry point relative to the installed plugin.
- **Downloaded:** Resolve the known entry point in the chosen download cache.
- **User/project installed:** Use the specified location, the toolchain's documented locator, or
  execution-host PATH, according to the agreed setup. Prefer existing platform APIs over writing
  a custom locator.

Do not add scans of common installation directories, recursive disk searches, version ranking,
symlink/toolchain inference, or chains of fallback locations by default. A justified requirement
can call for additional resolution logic, but failure of the chosen lookup should normally produce
a clear error with installation/PATH instructions. Ask about an unresolved setup choice instead
of building a discovery framework.

Use the template's platform LSP settings for file patterns, arguments, environment variables,
and initialization options. A need to locate an executable does not authorize a separate settings
page. If the user needs customization the platform settings cannot express, explain the specific
gap and ask how they want to handle it; do not silently add a new UI or persistence layer.

## Implement only the selected delivery method

| Method | Needed implementation |
| --- | --- |
| Bundle at build time | Obtain a versioned artifact through Gradle, unpack the required distribution into a server-specific plugin directory, and include it in both the sandbox and distribution ZIP. Keep layout and runtime lookup paths consistent. |
| Download on demand | Prefer a suitable existing platform facility. Download the chosen version/platform into a writable cache, validate the result, and start the client after setup. Keep custom download code limited to what the selected server needs. |
| User/project installation | Document the required installation and lookup rule. Resolve it on the project execution host and report missing prerequisites. Do not add an installer or settings page as a convenience. |

For bundled/downloaded artifacts, inspect the actual package and include the files needed to run it.
An npm tarball may omit dependencies; a .NET DLL may need adjacent libraries and runtime configuration.
Keep server content separate from the plugin's own `lib/`, preserve executable permissions, and retain
required upstream notices. Build native variants only for the intended targets.

## Check the runtime, separately from the server

- **Node.js:** Check whether the package contains JavaScript or a native executable. If Node is
  needed, use the chosen managed or installed runtime. The Tailwind example shows
  `withNodeRuntimeEnsured` and `LspNodeRuntimeManager`; verify compatibility with the target IDE.
  Avoid `npx` silently downloading or selecting a different server from the project.
- **.NET:** Check whether the release is self-contained or requires a runtime/SDK and which version.
  Keep companion files with the server. The Bicep example shows runtime detection and provisioning;
  reuse only the parts needed by the chosen strategy.
- **Other tools:** Check concrete upstream requirements, such as `bazel` for bazel-lsp. Document
  prerequisites; do not build a general dependency manager.

The launch command must use the documented LSP mode/transport, runtime and entry point, and working
directory when needed. Do not guess flags. Preserve the template's arguments, environment, and
initialization-option overrides. Keep downloads and blocking setup off the UI thread.

## Choose one relevant example

Use the same checkout as the template, or obtain the relevant files from
[the samples repository](https://github.com/JetBrains/lsp-integration-samples/tree/main/plugins).

| Directory | Useful pattern |
| --- | --- |
| `plugins/lua-lsp-support` | Bundled native distribution, settings provider, and execution-host deployment. |
| `plugins/bazel-lsp-support` | Bundled executable, external tool prerequisite, and project working directory. |
| `plugins/typescript-lsp-support` | Native server distributed through npm artifacts; npm does not imply Node is needed. |
| `plugins/tailwind-lsp-support` | Bundled npm server and managed Node runtime. |
| `plugins/bicep-lsp-support` | Bundled .NET server and runtime setup. |

Read the relevant Gradle configuration and provider/runtime code. Do not copy a sample's entire
infrastructure into the new plugin. Tailwind and Bicep currently have a different module layout;
retain the template's frontend module and built-in settings integration.

## Execution host and remote support

Keep the template's frontend integration. For remote support, the server, runtime, working directory,
and environment must be on the project execution host. Use compatible project EEL APIs and the
deployment pattern in a native example, such as `getEelDescriptor()`, `findOrDeployServer`, and
`asEelPath()`. Deploy the required distribution, not just a launcher.

Implement the intended host combinations without adding speculative platform fallbacks. Local split
mode checks module placement; it does not prove cross-OS deployment. State any untested limitations.
