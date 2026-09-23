@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.ivy
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        mavenCentral()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.3.20"
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.19.0-SNAPSHOT"
}

rootProject.name = "lua.lsp.modular"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }

        ivy {
            name = "LuaLS releases"
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

include("frontend")
