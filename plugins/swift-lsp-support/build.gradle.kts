import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("org.jetbrains.intellij.platform")
}

group = "org.jetbrains.lsp.samples.swift"
version = "1.0.0"

dependencies {
    intellijPlatform {
        intellijIdea("2026.3") {
            useInstaller = true
        }
        pluginModule(implementation(project(":frontend")))
        pluginVerifier()
    }
}

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.FRONTEND

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263.7307"
        }
    }

    pluginVerification {
        ides {
            current()
        }
    }
}
