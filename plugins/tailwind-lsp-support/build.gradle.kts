import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

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

val tailwindServerVersion = "0.16.0"

// The npm package is a dependency-free esbuild bundle that ships the prebuilt `@parcel/watcher`
// binaries for every platform, so a single archive covers all plugin distributions and there is
// no need for native plugin variants.
val lspServer = configurations.create("lspServer") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies.add(lspServer.name, "@tailwindcss:language-server:$tailwindServerVersion@tgz")

// The server is laid out in this directory inside the plugin, because the plugin root's own `lib`
// directory is reserved for the plugin JARs.
val serverDirectory = "tailwindcss-language-server"

val layoutTailwindServer = tasks.register<Sync>("layoutTailwindServer") {
    description = "unpack the Tailwind CSS language server npm package as plugin content"
    into(layout.buildDirectory.dir("tailwind-ls"))

    // `tarTree` evaluates its argument as per `files(...)`, so handing it a provider keeps
    // dependency resolution at execution time. Building the tree inside a `map {}` instead
    // would capture the build script and break the configuration cache.
    val archive = lspServer.elements.map { it.single().asFile }

    from(tarTree(resources.gzip(archive))) {
        // Drop the npm package's top-level `package` directory.
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263"
        }
    }
}

// Puts the unpacked server next to the plugin JARs, both in the `runIde` sandbox and in the
// distribution ZIP that `buildPlugin` archives from that sandbox.
tasks.withType<PrepareSandboxTask> {
    dependsOn(layoutTailwindServer)

    from(layout.buildDirectory.dir("tailwind-ls")) {
        into(pluginName.map { "$it/$serverDirectory" })
    }
}
