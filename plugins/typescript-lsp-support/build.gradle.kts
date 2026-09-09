import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PreparePluginVariantTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}


dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdeaUltimate("263-EAP-SNAPSHOT") {
            useInstaller = false
        }
        testFramework(TestFrameworkType.Platform)
    }
}

val typescriptVersion = "7.0.2"

// GitHub release asset classifiers. Every asset is an npm package tarball that has to be
// expanded before it can be bundled.
val typescriptArchives = listOf(
    "linux-x64",
    "linux-arm64",
    "darwin-x64",
    "darwin-arm64",
    "win32-x64",
    "win32-arm64",
)

// The server is laid out in this directory inside the plugin, because the plugin root's own `lib`
// directory is reserved for the plugin JARs, and the TypeScript package also ships a `lib` directory.
val serverDirectory = "typescript"

// A single archive per configuration, so the target classifier of every resolved file is known.
val typescriptConfigurations = typescriptArchives.associateWith { classifier ->
    val configuration = configurations.create("lspServer-$classifier") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(
        configuration.name,
        "microsoft:typescript:$typescriptVersion:$classifier@tgz",
    )
    configuration
}

val layoutTypeScript = tasks.register<Sync>("layoutTypeScript") {
    description = "lay out TypeScript language server archives as native plugin variant contents"
    into(layout.buildDirectory.dir("typescript-ls"))

    typescriptConfigurations.forEach { (classifier, configuration) ->
        // Local copy: referencing a script-level `val` inside `eachFile` below would capture the
        // build script itself, which the configuration cache cannot serialize.
        val destination = serverDirectory

        // `tarTree` evaluates its argument as per `files(...)`, so handing it a provider keeps
        // dependency resolution at execution time. Building the tree inside a `map {}` instead
        // would capture the build script and break the configuration cache.
        val archive = configuration.elements.map { it.single().asFile }

        from(tarTree(resources.gzip(archive))) {
            // Rewriting the whole path rather than using `into(...)`: inside `eachFile` the
            // relative path already carries the `into(...)` prefix, which makes dropping the
            // npm package's top-level `package` directory by index error-prone.
            eachFile {
                relativePath = RelativePath(
                    true,
                    classifier,
                    destination,
                    *relativePath.segments.drop(1).toTypedArray(),
                )
            }
            includeEmptyDirs = false
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }
    nativeVariants {
        enabled = true

        linux {
            x86_64.from(layout.buildDirectory.dir("typescript-ls/linux-x64"))
            arm64.from(layout.buildDirectory.dir("typescript-ls/linux-arm64"))
        }
        mac {
            x86_64.from(layout.buildDirectory.dir("typescript-ls/darwin-x64"))
            arm64.from(layout.buildDirectory.dir("typescript-ls/darwin-arm64"))
        }
        windows {
            x86_64.from(layout.buildDirectory.dir("typescript-ls/win32-x64"))
            arm64.from(layout.buildDirectory.dir("typescript-ls/win32-arm64"))
        }
    }
}

tasks.withType<PreparePluginVariantTask> {
    dependsOn(layoutTypeScript)
}

// Gradle produces reproducible archives and normalizes entry permissions to 0644,
// which would strip the executable bit from the bundled language server.
tasks.withType<Zip>()
    .matching { it.name.startsWith("buildPluginVariants_") }
    .configureEach {
        eachFile {
            if (name == "tsc" || name == "tsc.exe") {
                permissions { unix("0755") }
            }
        }
    }
