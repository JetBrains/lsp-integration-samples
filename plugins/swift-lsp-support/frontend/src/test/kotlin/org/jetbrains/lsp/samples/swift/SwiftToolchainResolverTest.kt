// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.swift

import com.intellij.execution.ExecutionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class SwiftToolchainResolverTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `explicit root wins over PATH`() {
        val chosen = toolchain("chosen")
        val other = toolchain("other")
        assertEquals(chosen, resolve(mapOf("SWIFT_TOOLCHAIN" to root(chosen).toString(), "PATH" to other.parent.toString())))
    }

    @Test
    fun `explicit usr and bin directories are accepted including spaces`() {
        val chosen = toolchain("Swift With Spaces")
        for (path in listOf(chosen.parent, chosen.parent.parent)) {
            assertEquals(chosen, resolve(mapOf("SWIFT_TOOLCHAIN" to "\"$path\"")))
        }
    }

    @Test
    fun `invalid explicit path never falls back`() {
        val other = toolchain("other")
        val error = assertThrows(ExecutionException::class.java) {
            resolve(mapOf("SWIFT_TOOLCHAIN" to temporary.root.resolve("missing").path, "PATH" to other.parent.toString()))
        }
        assertTrue(error.message!!.contains("SWIFT_TOOLCHAIN"))
    }

    @Test
    fun `blank override uses first complete toolchain on PATH`() {
        val first = toolchain("first")
        val second = toolchain("second")
        assertEquals(first, resolve(mapOf("SWIFT_TOOLCHAIN" to " ", "PATH" to "${first.parent}:${second.parent}")))
    }

    @Test
    fun `PATH skips incomplete toolchains and relative entries`() {
        val incomplete = toolchain("incomplete")
        Files.delete(incomplete.parent.resolve("swift"))
        val complete = toolchain("complete")
        assertEquals(complete, resolve(mapOf("PATH" to ":.:relative:${incomplete.parent}:${complete.parent}")))
    }

    @Test
    fun `POSIX requires executable permission`() {
        val server = toolchain("not-executable")
        assertTrue(server.toFile().setExecutable(false, false))
        assertThrows(ExecutionException::class.java) { resolve(mapOf("PATH" to server.parent.toString())) }
    }

    @Test
    fun `Windows PATH is case insensitive and handles quoted directories`() {
        val server = toolchain("Swift With Spaces", windows = true)
        assertEquals(server, resolve(mapOf("Path" to ";relative;\"${server.parent}\""), windows = true))
    }

    @Test
    fun `Windows PATH override replaces differently cased parent variable`() {
        val parent = toolchain("parent", windows = true)
        val chosen = toolchain("chosen", windows = true)
        val environment = mapOf("Path" to parent.parent.toString()) + mapOf("PATH" to chosen.parent.toString())
        assertEquals(chosen, resolve(environment, windows = true))
    }

    @Test
    fun `Windows PATH expands environment variables`() {
        val chosen = toolchain("Swift With Spaces", windows = true)
        val environment = mapOf("Path" to "%TOOLCHAIN_BIN%", "Toolchain_Bin" to chosen.parent.toString())
        assertEquals(chosen, resolve(environment, windows = true))
    }

    @Test
    fun `Windows per-user install is found without PATH`() {
        val server = toolchain("Local/Programs/Swift/Toolchains/6.3.3+Asserts", windows = true)
        assertEquals(server, resolve(mapOf("LocalAppData" to temporary.root.resolve("Local").path), windows = true))
    }

    @Test
    fun `Windows system-wide install is found without PATH`() {
        val server = toolchain("Program Files/Swift/Toolchains/6.3.3+Asserts", windows = true)
        assertEquals(server, resolve(mapOf("PROGRAMFILES" to temporary.root.resolve("Program Files").path), windows = true))
    }

    @Test
    fun `Windows flat installation is found`() {
        val server = toolchain("Program Files/Swift", windows = true)
        assertEquals(server, resolve(mapOf("ProgramFiles" to temporary.root.resolve("Program Files").path), windows = true))
    }

    @Test
    fun `Windows legacy installation uses execution host path mapping`() {
        val server = toolchain("legacy/swift-5.10-RELEASE.xctoolchain", windows = true)
        val legacy = temporary.root.resolve("legacy").toPath()
        val resolver = SwiftToolchainResolver(true, mapOf("SystemDrive" to "D:")) {
            if (it == "D:\\Library\\Developer\\Toolchains") legacy else Path.of(it)
        }
        assertEquals(server, resolver.resolve())
    }

    @Test
    fun `PATH wins over Windows installation locations`() {
        val chosen = toolchain("chosen", windows = true)
        toolchain("Local/Programs/Swift/Toolchains/other", windows = true)
        assertEquals(chosen, resolve(mapOf("PATH" to chosen.parent.toString(), "LOCALAPPDATA" to temporary.root.resolve("Local").path), true))
    }

    @Test
    fun `multiple Windows installations require explicit selection`() {
        toolchain("Local/Programs/Swift/Toolchains/6.2.3+Asserts", windows = true)
        val chosen = toolchain("Local/Programs/Swift/Toolchains/6.3.3+Asserts", windows = true)
        val env = mapOf("LOCALAPPDATA" to temporary.root.resolve("Local").path)
        val error = assertThrows(ExecutionException::class.java) { resolve(env, true) }
        assertTrue(error.message!!.contains("Several Swift toolchains"))
        assertEquals(chosen, resolve(env + ("SWIFT_TOOLCHAIN" to root(chosen).toString()), true))
    }

    @Test
    fun `missing server provides actionable setup error`() {
        val error = assertThrows(ExecutionException::class.java) { resolve(emptyMap()) }
        assertTrue(error.message!!.contains("Language Servers"))
        assertTrue(error.message!!.contains("does not install"))
    }

    private fun resolve(environment: Map<String, String>, windows: Boolean = false): Path =
        SwiftToolchainResolver(windows, environment) { Path.of(it) }.resolve()

    private fun root(server: Path): Path = server.parent.parent.parent

    private fun toolchain(name: String, windows: Boolean = false): Path {
        val bin = temporary.root.toPath().resolve(name).resolve("usr/bin")
        Files.createDirectories(bin)
        val suffix = if (windows) ".exe" else ""
        for (name in listOf("swift", "sourcekit-lsp")) {
            val file = Files.createFile(bin.resolve(name + suffix))
            assertTrue(file.toFile().setExecutable(true))
        }
        return bin.resolve("sourcekit-lsp$suffix")
    }
}
