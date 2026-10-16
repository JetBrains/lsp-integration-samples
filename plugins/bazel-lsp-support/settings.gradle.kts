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
    id("org.jetbrains.intellij.platform.settings") version "2.20.0-SNAPSHOT"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }

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

include("frontend")
