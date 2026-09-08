// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.typescript

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


class TypeScriptLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        if (isTypeScriptLspFile(file)) {
            clientStarter.ensureClientStarted(TypeScriptLspServerDescriptor(project))
        }
    }

    override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
        return LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language)
    }
}

class TypeScriptLspServerDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "TypeScript") {
    override fun isSupportedFile(file: VirtualFile): Boolean = isTypeScriptLspFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = findBundledTypeScriptLanguageServer()
            ?: throwMissingLspExecutable(project, "TypeScript", "typescript.lsp.executable.not.found")
        return GeneralCommandLine(executable, "--lsp", "--stdio")
    }
}

private val SUPPORTED_EXTENSIONS = setOf("ts", "tsx", "mts", "cts")

private fun isTypeScriptLspFile(file: VirtualFile): Boolean = file.extension in SUPPORTED_EXTENSIONS

private fun findBundledTypeScriptLanguageServer(): String? {
    val plugin = PluginManagerCore.getPlugin(
        PluginId.getId("org.jetbrains.typescript-lsp-support")
    ) ?: return null

    val executableName =
        if (SystemInfo.isWindows) "tsc.exe"
        else "tsc"

    val executable = plugin.pluginPath
        .resolve("typescript")
        .resolve("lib")
        .resolve(executableName)

    return executable
        .takeIf { Files.isRegularFile(it) }
        ?.toString()
}
