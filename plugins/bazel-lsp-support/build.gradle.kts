import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

version = "1.0.1"

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdea("latest") {
            useInstaller = true
        }
        pluginModule(implementation(project(":frontend")))
        testFramework(TestFrameworkType.Platform)
    }
}

val bazelLspVersion = "0.6.4"

// `bin` is a synthetic extension for the extension-less Linux and macOS release assets.
fun bazelLspBinary(classifier: String, extension: String = "bin"): TaskProvider<Sync> {
    val declared = configurations.dependencyScope("lspServer-$classifier")
    dependencies.add(
        declared.name,
        "cameron-martin:bazel-lsp-$bazelLspVersion:v$bazelLspVersion:$classifier@$extension",
    )

    val binary = configurations.resolvable("lspServer-${classifier}Binary") {
        extendsFrom(declared.get())
    }.map { it.singleFile }

    return tasks.register<Sync>("prepareBazelLsp-$classifier") {
        from(binary)
        into(layout.buildDirectory.dir("bazel-lsp/$classifier"))
        rename { if (extension == "exe") "bazel-lsp.exe" else "bazel-lsp" }
        filePermissions { unix("0755") }
    }
}

val linuxX64BazelLsp = bazelLspBinary("linux-amd64")
val linuxArm64BazelLsp = bazelLspBinary("linux-arm64")

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.FRONTEND

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263"
        }
    }
    nativeVariants {
        enabled = true

        linux {
            x86_64.from(linuxX64BazelLsp)
            arm64.from(linuxArm64BazelLsp)
        }
        mac {
            x86_64.from(bazelLspBinary("osx-amd64"))
            arm64.from(bazelLspBinary("osx-arm64"))
        }
        windows {
            x86_64.from(bazelLspBinary("windows-amd64", "exe"))
        }
    }
}

tasks.withType<BuildPluginTask>().configureEach {
    // Include both Linux distributions so the frontend can transfer the matching
    // server to a remote execution host.
    from(linuxX64BazelLsp) {
        into("linux-x64")
    }
    from(linuxArm64BazelLsp) {
        into("linux-arm64")
    }

    eachFile {
        if (name == "bazel-lsp" || name == "bazel-lsp.exe") {
            permissions { unix("0755") }
        }
    }
}
