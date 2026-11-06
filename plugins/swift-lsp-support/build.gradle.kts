import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("org.jetbrains.intellij.platform")
}

group = "org.jetbrains.lsp.samples.swift"
version = "0.0.1"

dependencies {
    intellijPlatform {
        intellijIdea("263.7031.23") {
            useInstaller = true
        }
        pluginModule(implementation(project(":frontend")))
    }
}

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.FRONTEND

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263"
        }
    }
}
