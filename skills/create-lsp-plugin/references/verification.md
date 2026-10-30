# Verify the plugin without expanding its scope

## Review the changes

Before building, check that all template identities/TODOs are resolved, the frontend descriptor and
class names agree, and the platform settings provider remains registered. Unless the user explicitly
requested a separate settings page, there should be no new `Configurable`, `projectConfigurable`,
or `applicationConfigurable` implementation/registration or duplicate settings persistence.

Check that server lookup follows the selected method. Remove speculative installation searches,
fallbacks, and unrelated helpers. Do not add features just to create more test scenarios.

## Build and inspect

From the generated project, use a compatible JDK and the included wrapper. On Windows use
`gradlew.bat`.

```shell
./gradlew buildPlugin
./gradlew verifyPluginStructure
```

Inspect the ZIP(s) in `build/distributions/` with `unzip -l` or Python's `zipfile`, and inspect
nested JARs as needed. Confirm the plugin ID, name, vendor, description, version, compatibility
bounds, frontend module, and registered classes.

For a bundled server/runtime, check the exact entry point and required supporting files against
the launch code, including executable permissions and the correct OS/CPU artifact. Check the
`runIde` sandbox layout too: native distribution ZIPs and the sandbox can differ. Use a temporary
extraction to check installed paths, rather than relying on files in the source checkout.

For an installed or downloaded server, verify the agreed lookup/setup path and runtime prerequisites.
An upstream-documented version command can help; do not invent flags or treat a process waiting
on stdio as a successful LSP initialization.

Run relevant existing tests. Add focused tests only for substantial custom logic actually needed
by this plugin. Run `verifyPlugin` for additional IDE compatibility when that is in scope.
Do not introduce a broad testing framework or platform matrix for a straightforward integration.

## IDE smoke test

Use a small existing example project when practical, with any manifest/dependencies the server needs.
Document the supported file to open and the expected result.

```shell
./gradlew runIde
```

1. Confirm the plugin is enabled and the language file is recognized. Check server initialization
   through the available LSP status UI or IDE logs (Help → Show Log in Explorer/Finder).
2. Introduce and fix an error to check diagnostics. Try completion and navigation/hover where
   supported. Confirm these come from the new integration rather than another installed plugin.
3. Check file matching and one relevant override through the **existing platform LSP settings**.
   Open another supported file to confirm client reuse.
4. Check the selected setup's missing-prerequisite behavior. A clear error with a remedy is enough
   for user-installed tools; a custom setup UI is not required.

For the intended split-mode target, repeat the key checks with:

```shell
./gradlew runIdeSplitMode
```

Test a different execution-host OS/CPU only when that support is claimed and an environment is available.
Do not claim remote compatibility based solely on a local split-mode run.

Install the built plugin ZIP in a compatible test IDE/profile via Settings → Plugins → Install Plugin
from Disk and repeat the key smoke test to catch packaging differences. Preserve the user's working
IDE configuration.

## Handoff

Keep the plugin README concise: prerequisites, build/install/run commands, the sample project, and
expected behavior. Report build, package inspection, and IDE checks separately. When an interactive
IDE or target host is unavailable, label those checks unverified and give the exact remaining steps.
Build success alone does not establish that language features work.
