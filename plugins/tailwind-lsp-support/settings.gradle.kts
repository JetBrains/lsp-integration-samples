import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "tailwind-lsp-support"

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

        // https://registry.npmjs.org/@tailwindcss/language-server/-/language-server-0.16.0.tgz
        ivy {
            name = "npm"
            setUrl("https://registry.npmjs.org")
            patternLayout {
                artifact("[organization]/[module]/-/[module]-[revision].[ext]")
            }
            metadataSources { artifact() }
            content {
                includeGroup("@tailwindcss")
            }
        }
    }
}
