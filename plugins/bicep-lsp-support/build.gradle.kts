import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

version = "263.7307.102"

dependencies {
    intellijPlatform {
        intellijIdea("2026.3") {
            useInstaller = true
        }
        bundledModule("intellij.platform.lsp")
        bundledModule("intellij.platform.lsp.impl")
        pluginVerifier()
    }
}

val bicepLspVersion = "0.48.1"

val lspServer = configurations.create("lspServer") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies.add(lspServer.name, "Azure:bicep-langserver:$bicepLspVersion@zip")

tasks.withType<PrepareSandboxTask>().configureEach {
    val archive = lspServer.elements.map { it.single().asFile }
    from(zipTree(archive)) {
        into(pluginName.map { "$it/bicep-langserver" })
    }
}

intellijPlatform {
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
