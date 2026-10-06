// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.lua

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.eel.isArm64
import com.intellij.platform.eel.isLinux
import com.intellij.platform.eel.isMac
import com.intellij.platform.eel.isWindows
import com.intellij.platform.eel.isX86_64
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApiBlocking
import com.intellij.platform.eel.provider.utils.EelServerDeploymentUtils.findOrDeployServer
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettings
import com.intellij.platform.lsp.api.LspPluginServerConfiguration
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import com.intellij.platform.lsp.impl.createInitializationOptions
import java.nio.file.Files
import java.nio.file.Path

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
    private val configuration: LspPluginServerConfiguration =
        LspIntegrationSettings.getInstanceOrNull(project)
            ?.getPluginConfiguration(LuaLspServerSettingsProvider.SERVER_ID)
            ?: DEFAULT_CONFIGURATION,
) : ProjectWideLspClientDescriptor(project, configuration.name) {
    override fun isSupportedFile(file: VirtualFile): Boolean = configuration.isSupportedFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = findLuaLanguageServerOnExecutionHost(project)
            ?: throw ExecutionException("The bundled Lua language server executable was not found in the plugin installation.")
        return GeneralCommandLine(executable.asEelPath().toString()).apply {
            addParameters(configuration.arguments)
            configuration.environmentVariables.configureCommandLine(this)
        }
    }

    override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}

private fun findLuaLanguageServerOnExecutionHost(project: Project): Path? {
    val descriptor = project.getEelDescriptor()
    val platform = descriptor.toEelApiBlocking().platform
    val distributionPath = when {
        platform.isLinux && platform.isX86_64 -> "linux-x64"
        platform.isLinux && platform.isArm64 -> "linux-arm64"
        platform.isMac || platform.isWindows -> ""
        else -> return null
    }

    val pathToCopy = findPluginPath(distributionPath) ?: return null
    val serverPath = findOrDeployServer(descriptor, pathToCopy)
    return serverPath.resolve("bin/lua-language-server" + if (platform.isWindows) ".exe" else "")
        .takeIf { Files.isRegularFile(it) }
}

private fun findPluginPath(relativePath: String): Path? = PluginPathManager.getPluginDistPath(
    LuaLspIntegrationProvider::class.java,
    relativePath,
)
