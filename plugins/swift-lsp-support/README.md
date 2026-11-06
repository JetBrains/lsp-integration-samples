# Swift LSP Support

Connects IntelliJ IDEA to [SourceKit-LSP](https://github.com/swiftlang/sourcekit-lsp)
from an existing Swift toolchain for `*.swift` files, including `Package.swift`.
The plugin supplies the LSP integration only; it never downloads or installs Swift,
SourceKit-LSP, an SDK, or a runtime. Completion, diagnostics, hover, and navigation
are supplied by SourceKit-LSP. The target IDE's bundled TextMate Swift grammar
provides syntax highlighting independently of this plugin.

Plugin ID: `swift.lsp.support`. Server ID: `swift-sourcekit-lsp`.
Initial version: `0.0.1`.

## Existing toolchain and discovery

Use a complete Swift toolchain that includes both `swift` and `sourcekit-lsp`
(`swift.exe` and `sourcekit-lsp.exe` on Windows) in its binary directory. Use Swift
6.0 or newer for the included sample project. The server version follows the selected
toolchain; this plugin does not pin or update it. Keep the toolchain's libraries,
SDKs, and runtime installed alongside the tools. On Windows, the existing installation
must include the required Visual Studio C++ build tools and Windows SDK; see
[Swift's Windows prerequisites](https://www.swift.org/install/windows/manual/).
Preserve installer-provided environment settings such as `SDKROOT` where required.

Discovery runs on the **project execution host**, in this order:

1. A nonempty `SWIFT_TOOLCHAIN` value in the server's environment settings. It must
   be an absolute toolchain root, `usr` directory, or `bin` directory. An invalid
   override reports an error instead of silently selecting another toolchain.
2. The first directory on the effective `PATH` containing both executables.
3. On Windows only, the usual installation layouts:
   - `%LOCALAPPDATA%\Programs\Swift\Toolchains\<toolchain>\usr\bin`
   - `%ProgramFiles%\Swift\Toolchains\<toolchain>\usr\bin`
   - The `usr\bin` or `bin` directory directly under either Swift installation root.
   - Legacy `%SystemDrive%\Library\Developer\Toolchains\<toolchain>\usr\bin`.

Only immediate toolchain directories are inspected; there is no recursive disk scan.
If several complete Windows toolchains are found and PATH has not selected one, set
`SWIFT_TOOLCHAIN` explicitly. Windows environment names are case insensitive and
quoted PATH entries are accepted. See the upstream
[Windows toolchain layout](https://github.com/swiftlang/swift/blob/main/docs/WindowsToolchain.md).

On macOS and Linux, put the installed toolchain's binary directory on PATH or set the
explicit toolchain path. On macOS an Xcode toolchain root can be used, for example
`/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain`.
Remote projects use paths and environment variables from their execution host.
No local toolchain is copied to a remote machine.

## Select a toolchain

Open **Settings | Languages & Frameworks | Language Servers**, select
**Swift (SourceKit-LSP)**, and edit **Environment variables**:

```text
SWIFT_TOOLCHAIN=C:\Users\you\AppData\Local\Programs\Swift\Toolchains\6.3.3+Asserts
```

Use the actual installed directory, without shell expressions such as `%LOCALAPPDATA%`
or `~`. The default empty value enables automatic discovery. Keep parent environment
inheritance enabled to retain SDK/runtime variables. File patterns, arguments,
environment variables, and initialization options remain configurable in this same
platform UI. Apply changes and restart the server using the LSP status widget.

The command is exactly:

```text
<resolved-bin>/sourcekit-lsp[.exe] <configured arguments...>
```

SourceKit-LSP uses standard input/output by default; no `--stdio` flag is added.
The platform sends the project roots through LSP initialization; the plugin does not
override the process working directory. The platform applies the configured environment
variables and parent environment setting; the plugin does not rewrite PATH.

## Build and install

This project was copied from `lsp-plugin-template/` at repository commit
`a0601a849bd5ddeabe23cc3f5d62015965985f15` and keeps its frontend module layout.
It targets IntelliJ IDEA EAP build **263.7031.23**, with minimum platform build **263**,
and retains the template's IntelliJ Platform Gradle Plugin **2.20.0-SNAPSHOT**.
Use **JDK 25** for this selected IDE (its product metadata requires Java 25).
The build downloads Gradle/IDE dependencies as needed, but no Swift tools.

From this directory, with `JAVA_HOME` pointing to JDK 25:

```shell
./gradlew buildPlugin verifyPluginStructure :frontend:test
./gradlew runIde
./gradlew runIdeSplitMode
```

Use `gradlew.bat` on Windows. Install `build/distributions/swift-lsp-support-0.0.1.zip`
through **Settings | Plugins | Install Plugin from Disk** in a compatible test IDE.
The same ZIP contains no native binaries and is intended for Windows, macOS, and
Linux with a working toolchain. Split mode resolves the server on the project host.

## Verify language support

Open [`../../samples/swift/swift-tagged`](../../samples/swift/swift-tagged) as the
project root in the sandbox IDE. With its existing Swift 6+ toolchain, run
`swift build` there to populate modules and the index. Open
`Sources/Tagged/Tagged.swift`; confirm that **Swift (SourceKit-LSP)** starts in the
LSP widget or IDE logs. Introduce `let lspCheck: Int = "wrong"` inside a function,
check for a diagnostic, then remove it. Check completion after `String().`, hover,
and navigation to a symbol in the package. Open a second Swift file and confirm that
the same project server is reused. Cross-module results can require a fresh build;
see [SourceKit-LSP's indexing guidance](https://github.com/swiftlang/sourcekit-lsp#readme).

Change `SWIFT_TOOLCHAIN` to a nonexistent directory and restart the server: expect
an actionable setup error. Restore it and verify startup. Check a custom file pattern
and one server argument through the platform settings. Repeat the feature checks in
split mode and after installing the distribution ZIP. For a remote project, select
a toolchain that exists on the remote host.

The resolver tests cover override precedence, invalid paths, PATH ordering, executable
permissions, Windows layouts, and ambiguous installations using temporary files.
They do not replace a Windows runtime check or an IDE language-feature smoke test.

Verified on 2026-10-30: `buildPlugin`, `verifyPluginStructure`, and all 14 resolver tests
passed with JDK 25 against IDEA 263.7031.23. The distribution ZIP's metadata,
frontend descriptor, and registered classes were inspected; it contains no Swift
binaries. A direct stdio check with the existing Apple Swift 6.2.3 SourceKit-LSP
passed initialization, type-error diagnostics, clearing diagnostics after a fix,
hover, and shutdown. This checks the server independently of the IDE integration.
Interactive IDE and split-mode checks were blocked by the locked desktop. Native
Windows/Linux execution and remote-host behavior remain unverified.

Verified on 2026-11-02 with the existing Apple Swift 6.2.3 toolchain: launching
SourceKit-LSP from the package root and from an unrelated directory produced the same
successful initialization, local-target dependency resolution, package compiler settings,
type-error diagnostics, corrected diagnostics, hover, and shutdown. Both runs received
the package URI in LSP initialization. This was a direct server check on macOS.
