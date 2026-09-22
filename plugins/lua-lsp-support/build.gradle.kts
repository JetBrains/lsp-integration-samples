import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdeaUltimate("263-EAP-SNAPSHOT") {
            useInstaller = false
        }

        testFramework(TestFrameworkType.Platform)
    }
}

val luaLsVersion = "3.19.1"

/**
 * Resolves the LuaLS archive for the given platform and exposes its content as a file tree,
 * so that native variant archives unpack it directly, with no intermediate directory involved.
 */
fun luaLsArchive(classifier: String, extension: String): FileTree {
    val declared = configurations.dependencyScope("lspServer-$classifier")
    dependencies.add(declared.name, "LuaLS:lua-language-server-$luaLsVersion:$luaLsVersion:$classifier@$extension")

    val archiveFile = configurations.resolvable("lspServer-${classifier}Archive") {
        extendsFrom(declared.get())
    }.map { it.singleFile }

    return when (extension) {
        "zip" -> zipTree(archiveFile)
        "tar.gz" -> tarTree(resources.gzip(archiveFile))
        else -> throw GradleException("Unsupported LuaLS archive extension: $extension")
    }
}

val linuxX64LuaLs = luaLsArchive("linux-x64", "tar.gz")
val linuxArm64LuaLs = luaLsArchive("linux-arm64", "tar.gz")

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263"
        }
    }
    nativeVariants {
        enabled = true

        linux {
            x86_64.from(linuxX64LuaLs)
            arm64.from(linuxArm64LuaLs)
        }
        mac {
            x86_64.from(luaLsArchive("darwin-x64", "tar.gz"))
            arm64.from(luaLsArchive("darwin-arm64", "tar.gz"))
        }
        windows {
            x86_64.from(luaLsArchive("win32-x64", "zip"))
        }
    }
}

tasks.withType<BuildPluginTask>().configureEach {
    // Include both Linux distributions in every plugin archive. They are
    // needed when the IDE frontend runs on one OS and the backend runs on Linux.
    from(linuxX64LuaLs) {
        into("lua-ls/linux-x64")
    }
    from(linuxArm64LuaLs) {
        into("lua-ls/linux-arm64")
    }

    eachFile {
        if (name == "lua-language-server" || name == "lua-language-server.exe") {
            permissions { unix("0755") }
        }
    }
}
