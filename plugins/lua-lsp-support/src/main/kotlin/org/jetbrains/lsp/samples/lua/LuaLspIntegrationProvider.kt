// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.lua

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
import com.intellij.platform.lsp.impl.LspPluginServerConfiguration
import java.nio.file.Files


class LuaLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        val descriptor = LuaLspServerDescriptor(project)
        if (descriptor.isSupportedFile(file)) {
            clientStarter.ensureClientStarted(descriptor)
        }
    }

    override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
        return LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language)
    }
}

class LuaLspServerDescriptor(
    project: Project,
    private val configuration: LspPluginServerConfiguration = LuaLspServerSettingsProvider.DEFAULT_CONFIGURATION,
) : ProjectWideLspClientDescriptor(project, configuration.name) {
    override fun isSupportedFile(file: VirtualFile): Boolean = configuration.isSupportedFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = findBundledLuaLanguageServer()
            ?: throwMissingLspExecutable(project, configuration.name, "lua.lsp.executable.not.found")
        return GeneralCommandLine(executable).apply {
            addParameters(configuration.arguments)
            configuration.environmentVariables.configureCommandLine(this, true)
        }
    }

    override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}

private fun findBundledLuaLanguageServer(): String? {
    val plugin = PluginManagerCore.getPlugin(
        PluginId.getId("org.jetbrains.lua-lsp-support")
    ) ?: return null

    val executableName =
        if (SystemInfo.isWindows) "lua-language-server.exe"
        else "lua-language-server"

    val executable = plugin.pluginPath
        .resolve("bin")
        .resolve(executableName)

    return executable
        .takeIf { Files.isRegularFile(it) }
        ?.toString()
}
