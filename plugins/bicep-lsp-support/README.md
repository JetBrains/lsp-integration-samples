# Bicep

An IntelliJ Platform plugin that provides [Bicep](https://github.com/Azure/bicep)
language support through the bundled Bicep language server.

The plugin handles `.bicep` files and uses .NET 10 to run the server.
It reuses a compatible .NET installation or downloads one when needed.

[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22913-bicep)

## Build

Open this directory as a Gradle project with JDK 21, then run:

```shell
./gradlew buildPlugin
```

The plugin ZIP is in `build/distributions/`. Use `gradlew.bat` on Windows.

