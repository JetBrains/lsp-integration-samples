// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.swift

import com.intellij.execution.ExecutionException
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.EelOsFamily
import com.intellij.platform.eel.environmentVariables
import com.intellij.platform.eel.expandPathEnvVar
import com.intellij.platform.eel.isWindows
import com.intellij.platform.eel.path.EelPath
import com.intellij.platform.eel.provider.asNioPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApiBlocking
import com.intellij.platform.lsp.api.LspServerEnvironmentData
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap

/** All paths and environment values belong to the project execution host. */
internal class SwiftToolchainResolver(
    private val windows: Boolean,
    environment: Map<String, String>,
    private val toHostPath: (String) -> Path,
) {
    private val environment = if (windows) environment.toMap(TreeMap(String.CASE_INSENSITIVE_ORDER)) else environment
    private val serverName = if (windows) "sourcekit-lsp.exe" else "sourcekit-lsp"
    private val swiftName = if (windows) "swift.exe" else "swift"

    fun resolve(): Path {
        val configured = environment[TOOLCHAIN_VARIABLE]?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            return absolutePath(configured)?.let(::inToolchain) ?: throw ExecutionException(
                "$TOOLCHAIN_VARIABLE is not a usable Swift toolchain: $configured. " +
                    "It must contain both $swiftName and $serverName. $TOOLCHAIN_HELP",
            )
        }

        val path = if (windows) EelOsFamily.Windows.expandPathEnvVar(environment) else environment["PATH"]
        for (entry in path.orEmpty().split(if (windows) ';' else ':')) {
            absolutePath(entry)?.let(::inBin)?.let { return it }
        }

        if (windows) {
            val candidates = windowsToolchains().mapNotNull(::inToolchain).distinct()
            if (candidates.size == 1) return candidates.single()
            if (candidates.size > 1) {
                throw ExecutionException("Several Swift toolchains contain SourceKit-LSP. $TOOLCHAIN_HELP")
            }
        }

        throw ExecutionException(
            "SourceKit-LSP was not found. Add a Swift toolchain's bin directory containing both $swiftName and $serverName to PATH. " +
                "$TOOLCHAIN_HELP " +
                "This plugin does not install Swift or SourceKit-LSP.",
        )
    }

    private fun inToolchain(root: Path): Path? =
        sequenceOf(root.resolve("usr").resolve("bin"), root.resolve("bin"), root).firstNotNullOfOrNull(::inBin)

    private fun inBin(bin: Path): Path? {
        val server = bin.resolve(serverName)
        return server.takeIf { executable(it) && executable(bin.resolve(swiftName)) }
    }

    private fun executable(path: Path): Boolean = Files.isRegularFile(path) && (windows || Files.isExecutable(path))

    private fun windowsToolchains(): List<Path> {
        val swiftRoots = listOfNotNull(
            environment["LOCALAPPDATA"]?.let(::absolutePath)?.resolve("Programs")?.resolve("Swift"),
            environment["ProgramFiles"]?.let(::absolutePath)?.resolve("Swift"),
        ).distinct()
        val toolchainDirectories = swiftRoots.map { it.resolve("Toolchains") } + listOfNotNull(
            // Older swift.org installers used this directory.
            absolutePath((environment["SystemDrive"] ?: "C:") + "\\Library\\Developer\\Toolchains"),
        )
        return swiftRoots + toolchainDirectories.flatMap(::childDirectories)
    }

    private fun childDirectories(directory: Path): List<Path> = try {
        Files.newDirectoryStream(directory).use { entries -> entries.filter { Files.isDirectory(it) } }
    }
    catch (_: IOException) {
        emptyList()
    }

    private fun absolutePath(value: String): Path? {
        val unquoted = value.trim().removeSurrounding("\"")
        if (unquoted.isEmpty()) return null
        return try {
            toHostPath(unquoted).takeIf { it.isAbsolute }
        }
        catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        const val TOOLCHAIN_VARIABLE: String = "SWIFT_TOOLCHAIN"
        private const val TOOLCHAIN_HELP =
            "Set $TOOLCHAIN_VARIABLE to an absolute toolchain root, usr, or bin directory on the project execution host " +
                "in Language Servers > Swift (SourceKit-LSP) > Environment variables."

        fun resolve(project: Project, settings: LspServerEnvironmentData): Path {
            val host = project.getEelDescriptor()
            val eel = host.toEelApiBlocking()
            val parentEnvironment = if (settings.passParentEnvironment) {
                runBlockingMaybeCancellable { eel.exec.environmentVariables().eelIt().await() }
            }
            else emptyMap()
            return SwiftToolchainResolver(eel.platform.isWindows, parentEnvironment + settings.variables) {
                EelPath.parse(it, host).asNioPath()
            }.resolve()
        }
    }
}
