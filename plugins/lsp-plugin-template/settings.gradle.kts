@file:Suppress("UnstableApiUsage")

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
    id("org.jetbrains.intellij.platform.settings") version "2.20.0-SNAPSHOT"
}

// TODO: Rename the project and the matching template.lsp.frontend module in both XML descriptors.
rootProject.name = "template.lsp"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

include("frontend")
