import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("263-EAP-SNAPSHOT") {
            useInstaller = false
        }
        pluginModule(implementation(project(":frontend")))
        testFramework(TestFrameworkType.Platform)
    }
}

val typescriptVersion = "7.0.2"

fun typescriptArchive(classifier: String): FileTree {
    val declared = configurations.dependencyScope("lspServer-$classifier")
    dependencies.add(
        declared.name,
        "microsoft:typescript:$typescriptVersion:$classifier@tgz",
    )

    val archiveFile = configurations.resolvable("lspServer-${classifier}Archive") {
        extendsFrom(declared.get())
    }.map { it.singleFile }

    return tarTree(resources.gzip(archiveFile))
}

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
            x86_64.from(typescriptArchive("linux-x64"))
            arm64.from(typescriptArchive("linux-arm64"))
        }
        mac {
            x86_64.from(typescriptArchive("darwin-x64"))
            arm64.from(typescriptArchive("darwin-arm64"))
        }
        windows {
            x86_64.from(typescriptArchive("win32-x64"))
            arm64.from(typescriptArchive("win32-arm64"))
        }
    }
}

tasks.withType<BuildPluginTask>().configureEach {
    includeEmptyDirs = false

    eachFile {
        // Preserve executable permissions in Gradle's reproducible archives.
        if (name == "tsc" || name == "tsc.exe") {
            permissions { unix("0755") }
        }
    }
}
