import org.gradle.kotlin.dsl.ivy
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "lua-lsp-support"

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
        //https://github.com/LuaLS/lua-language-server/releases/download/3.19.1/lua-language-server-3.19.1-win32-x64.zip


        ivy {
            name = "releases"
            setUrl("https://github.com")
            patternLayout {
                artifact("[organization]/lua-language-server/releases/download/[revision]/[artifact]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content {
                includeGroup("LuaLS")
            }
        }
    }
}
