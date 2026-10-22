import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("org.jetbrains.intellij.platform")
}

// TODO: Set your plugin's group and version.
group = "org.jetbrains.lsp.samples.template"
version = "0.0.1"

dependencies {
    intellijPlatform {
        intellijIdea("latest") {
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
