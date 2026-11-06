# Swift LSP

Integrates [SourceKit-LSP](https://github.com/swiftlang/sourcekit-lsp) with IntelliJ
IDEA for Swift files, including `Package.swift`. The integration lives in the
`frontend` module and supports split mode.

## Requirements and setup

- IntelliJ IDEA 2026.3 or newer (platform build 263+).
- A Swift toolchain containing both `swift` and `sourcekit-lsp` on the project
  execution host. The plugin does not install Swift or its SDKs.

Put the toolchain's binary directory on `PATH`, or set `SWIFT_TOOLCHAIN` to an
absolute toolchain root or binary directory in **Settings | Languages & Frameworks |
Language Servers | Swift (SourceKit-LSP) | Environment variables**.
For remote projects, the toolchain must be installed on the remote host.

Try the [sample project](../../samples/swift/swift-tagged) with Swift 6+.
Run `swift build` in the package root first to resolve dependencies.

## Build and install

With `JAVA_HOME` pointing to **JDK 25**, run from this directory:

```shell
./gradlew buildPlugin
```

Install the ZIP from `build/distributions/` through **Settings | Plugins |
Install Plugin from Disk**. Use `gradlew.bat` on Windows.

Run `./gradlew runIde` or `./gradlew runIdeSplitMode` to test in a sandbox IDE.
