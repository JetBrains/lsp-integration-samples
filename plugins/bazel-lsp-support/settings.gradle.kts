import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "bazel-lsp-support"

pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        gradlePluginPortal()
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.3.20"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.19.0-SNAPSHOT"
}

dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
        // https://github.com/cameron-martin/bazel-lsp/releases/download/v0.6.4/bazel-lsp-0.6.4-linux-amd64
        // https://github.com/cameron-martin/bazel-lsp/releases/download/v0.6.4/bazel-lsp-0.6.4-windows-amd64.exe

        ivy {
            name = "releases"
            setUrl("https://github.com")
            patternLayout {
                // The Windows asset is the only one with a file extension (`.exe`);
                // the Linux and macOS assets are extension-less binaries.
                artifact("[organization]/bazel-lsp/releases/download/[revision]/[artifact]-[classifier].[ext]")
                artifact("[organization]/bazel-lsp/releases/download/[revision]/[artifact]-[classifier]")
            }
            metadataSources { artifact() }
            content {
                includeGroup("cameron-martin")
            }
        }
    }
}
