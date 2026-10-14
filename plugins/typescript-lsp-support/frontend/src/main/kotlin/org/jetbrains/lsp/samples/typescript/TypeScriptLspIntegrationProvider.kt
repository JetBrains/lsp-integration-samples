// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.typescript

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettings
import com.intellij.platform.lsp.api.LspPluginServerConfiguration
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import com.intellij.platform.lsp.impl.createInitializationOptions
import java.nio.file.Files


class TypeScriptLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        val descriptor = TypeScriptLspServerDescriptor(project)
        if (descriptor.isSupportedFile(file)) {
            clientStarter.ensureClientStarted(descriptor)
        }
    }

    override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
        return LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language)
    }
}

class TypeScriptLspServerDescriptor(
    project: Project,
    private val configuration: LspPluginServerConfiguration =
        LspIntegrationSettings.getInstanceOrNull(project)
            ?.getPluginConfiguration(TypeScriptLspServerSettingsProvider.SERVER_ID)
            ?: DEFAULT_CONFIGURATION,
) : ProjectWideLspClientDescriptor(project, configuration.name) {
    override fun isSupportedFile(file: VirtualFile): Boolean = configuration.isSupportedFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = findBundledTypeScriptLanguageServer()
            ?: throw ExecutionException("The bundled TypeScript language server executable was not found in the plugin installation.")
        return GeneralCommandLine(executable).apply {
            addParameters(configuration.arguments)
            configuration.environmentVariables.configureCommandLine(this)
        }
    }

    override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}

private fun findBundledTypeScriptLanguageServer(): String? {
    val plugin = PluginManagerCore.getPlugin(
        PluginId.getId("org.jetbrains.typescript-lsp-support")
    ) ?: return null

    val executableName =
        if (SystemInfo.isWindows) "tsc.exe"
        else "tsc"

    val executable = plugin.pluginPath
        .resolve("package")
        .resolve("lib")
        .resolve(executableName)

    return executable
        .takeIf { Files.isRegularFile(it) }
        ?.toString()
}
