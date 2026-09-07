import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}


dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdeaUltimate("2026.2.1")
        testFramework(TestFrameworkType.Platform)

    }
}

val bazelLspVersion = "0.6.4"

// GitHub release asset classifier -> artifact extension used to address the asset.
// The Linux and macOS assets have no file extension, so `bin` is only a synthetic
// extension for the Gradle artifact coordinates and is not part of the download URL.
val bazelLspBinaries = listOf(
    "linux-amd64" to "bin",
    "linux-arm64" to "bin",
    "osx-amd64" to "bin",
    "osx-arm64" to "bin",
    "windows-amd64" to "exe",
)

// A single binary per configuration, so the target classifier of every resolved file is known.
val bazelLspConfigurations = bazelLspBinaries.associate { (classifier, extension) ->
    val configuration = configurations.create("lspServer-$classifier") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(
        configuration.name,
        "cameron-martin:bazel-lsp-$bazelLspVersion:v$bazelLspVersion:$classifier@$extension",
    )
    classifier to configuration
}

val layoutBazelLsp = tasks.register<Sync>("layoutBazelLsp") {
    description = "lay out bazel-lsp binaries as native plugin variant contents"
    into(layout.buildDirectory.dir("bazel-lsp"))

    bazelLspConfigurations.forEach { (classifier, configuration) ->
        val executableName = if (classifier.startsWith("windows")) "bazel-lsp.exe" else "bazel-lsp"

        from(configuration) {
            into("$classifier/bin")
            rename { executableName }
            filePermissions { unix("0755") }
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }
    nativeVariants {
        enabled = true

        linux {
            x86_64.from(layout.buildDirectory.dir("bazel-lsp/linux-amd64"))
            arm64.from(layout.buildDirectory.dir("bazel-lsp/linux-arm64"))
        }
        mac {
            x86_64.from(layout.buildDirectory.dir("bazel-lsp/osx-amd64"))
            arm64.from(layout.buildDirectory.dir("bazel-lsp/osx-arm64"))
        }
        windows {
            x86_64.from(layout.buildDirectory.dir("bazel-lsp/windows-amd64"))
        }
    }
}

// `prepareSandbox_runIde` bundles the native variant matching the host machine, so `runIde`
// needs the binaries laid out as well.
tasks.matching { it.name.startsWith("buildPluginVariants_") || it.name.startsWith("prepareSandbox") }
    .configureEach {
        dependsOn(layoutBazelLsp)
    }

// Gradle produces reproducible archives and normalizes entry permissions to 0644,
// which would strip the executable bit from the bundled language server.
tasks.withType<Zip>()
    .matching { it.name.startsWith("buildPluginVariants_") }
    .configureEach {
        eachFile {
            if (name == "bazel-lsp" || name == "bazel-lsp.exe") {
                permissions { unix("0755") }
            }
        }
    }
