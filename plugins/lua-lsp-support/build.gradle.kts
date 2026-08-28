import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}


dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdeaUltimate("2026.1.4")
        testFramework(TestFrameworkType.Platform)

    }
}

val luaLsVersion = "3.19.1"
val luaLsArchives = listOf(
    "win32-x64" to "zip",
    "darwin-x64" to "tar.gz",
    "darwin-arm64" to "tar.gz",
    "linux-x64" to "tar.gz",
    "linux-arm64" to "tar.gz",
)

val lspServer = configurations.create("lspServer")

dependencies {
    luaLsArchives.forEach { (classifier, extension) ->
        lspServer("LuaLS:lua-language-server-$luaLsVersion:$luaLsVersion:$classifier@$extension")
    }
}

val unpackLuaLs = tasks.register("unpackLuaLs") {
    val outputDirectory = layout.buildDirectory.dir("lua-ls")
    notCompatibleWithConfigurationCache("The unpack task uses Gradle archive file trees during execution")
    inputs.files(lspServer)
    outputs.dir(outputDirectory)

    doLast {
        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()

        lspServer.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val classifier = artifact.classifier
                ?: throw GradleException("LuaLS archive has no classifier: ${artifact.name}")
            val archiveFile = artifact.file
            val archiveContents = when {
                archiveFile.name.endsWith(".zip") -> zipTree(archiveFile)
                archiveFile.name.endsWith(".tar.gz") -> tarTree(resources.gzip(archiveFile))
                else -> throw GradleException("Unsupported LuaLS archive: ${archiveFile.name}")
            }

            project.copy {
                from(archiveContents)
                into(File(outputDir, classifier))
            }
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }
    nativeVariants {
        enabled = true

        linux {
            x86_64.from(layout.buildDirectory.dir("lua-ls/linux-x64"))
            arm64.from(layout.buildDirectory.dir("lua-ls/linux-arm64"))
        }
        mac {
            x86_64.from(layout.buildDirectory.dir("lua-ls/darwin-x64"))
            arm64.from(layout.buildDirectory.dir("lua-ls/darwin-arm64"))
        }
        windows {
            x86_64.from(layout.buildDirectory.dir("lua-ls/win32-x64"))
        }
    }
}

tasks.matching { it.name.startsWith("buildPluginVariants_") }.configureEach {
    dependsOn(unpackLuaLs)
}
