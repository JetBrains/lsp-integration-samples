// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.bazel

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import java.nio.file.Files
import java.nio.file.Path


class BazelLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        if (isBazelLspFile(file)) {
            clientStarter.ensureClientStarted(BazelLspServerDescriptor(project))
        }
    }

    override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
        return LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language)
    }
}

class BazelLspServerDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "Bazel") {
    override fun isSupportedFile(file: VirtualFile): Boolean = isBazelLspFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = findBundledBazelLanguageServer()
            ?: throwMissingLspExecutable(project, "Bazel", "bazel.lsp.executable.not.found")
        return GeneralCommandLine(executable).apply {
            // bazel-lsp shells out to the `bazel` executable, so run it inside the workspace.
            project.basePath?.let { withWorkingDirectory(Path.of(it)) }
        }
    }
}

private val BAZEL_FILE_EXTENSIONS = setOf("bzl", "bazel", "star", "sky")
private val BAZEL_FILE_NAMES = setOf("BUILD", "WORKSPACE")

private fun isBazelLspFile(file: VirtualFile): Boolean {
    val extension = file.extension
    return (extension != null && extension in BAZEL_FILE_EXTENSIONS) || file.name in BAZEL_FILE_NAMES
}

private fun findBundledBazelLanguageServer(): String? {
    val plugin = PluginManagerCore.getPlugin(
        PluginId.getId("org.jetbrains.bazel-lsp-support")
    ) ?: return null

    val executableName =
        if (SystemInfo.isWindows) "bazel-lsp.exe"
        else "bazel-lsp"

    val executable = plugin.pluginPath
        .resolve("bin")
        .resolve(executableName)

    return executable
        .takeIf { Files.isRegularFile(it) }
        ?.toString()
}
