// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.lua.modular

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.eel.isArm64
import com.intellij.platform.eel.isLinux
import com.intellij.platform.eel.isX86_64
import com.intellij.platform.eel.provider.LocalEelDescriptor
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApiBlocking
import com.intellij.platform.eel.provider.utils.EelPathUtils.TransferTarget
import com.intellij.platform.eel.provider.utils.EelPathUtils.transferLocalContentToRemote
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
      ?: throwMissingLspExecutable(project, configuration.name, "lua.lsp.executable.not.found")
    return GeneralCommandLine(executable.asEelPath().toString()).apply {
      addParameters(configuration.arguments)
      configuration.environmentVariables.configureCommandLine(this)
    }
  }

  override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}

private fun findLuaLanguageServerOnExecutionHost(project: Project): Path? {
  val descriptor = project.getEelDescriptor()

  if (descriptor is LocalEelDescriptor) {
    return findPluginPath("bin/lua-language-server")
      ?: findPluginPath("bin/lua-language-server.exe")
  }

  val platform = descriptor.toEelApiBlocking().platform
  val distributionName = when {
    platform.isLinux && platform.isX86_64 -> "linux-x64"
    platform.isLinux && platform.isArm64 -> "linux-arm64"
    else -> return null
  }

  val localDistribution = findPluginPath("lua-ls/$distributionName")
    ?.takeIf { Files.isDirectory(it) }
    ?: return null

  val remoteDistribution = transferLocalContentToRemote(
    source = localDistribution,
    target = TransferTarget.Temporary(descriptor),
  )

  return remoteDistribution
    .resolve("bin/lua-language-server")
    .takeIf { Files.isRegularFile(it) }
}

private fun findPluginPath(relativePath: String): Path? {
  val path = PluginPathManager.getPluginDistPath(
    LuaLspIntegrationProvider::class.java,
    relativePath,
  ) ?: return null

  return path.takeIf { Files.isRegularFile(it) || Files.isDirectory(it) }
}
