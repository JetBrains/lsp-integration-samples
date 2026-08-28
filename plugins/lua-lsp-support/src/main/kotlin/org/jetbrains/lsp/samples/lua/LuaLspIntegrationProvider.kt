// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.lua

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.Icon

private const val SERVER_DIRECTORY = "server"

class LuaLspIntegrationProvider : LspIntegrationProvider {
  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    clientStarter: LspIntegrationProvider.LspClientStarter,
  ) {
    if (isLuaLspFile(file)) {
      clientStarter.ensureClientStarted(LuaLspServerDescriptor(project))
    }
  }

  override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
    return LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language)
  }
}

class LuaLspServerDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "Lua") {
  override fun isSupportedFile(file: VirtualFile): Boolean = isLuaLspFile(file)

  override fun createCommandLine(): GeneralCommandLine {
    val executable = findBundledLuaLanguageServer()
      ?: throwMissingLspExecutable(project, "Lua", "lua.lsp.executable.not.found")
    return GeneralCommandLine(executable)
  }
}

class LuaLspFileType : FileType {
  override fun getName(): String = "Lua"
  override fun getDescription(): String = LuaLspBundle.message("lua.filetype.description")
  override fun getDefaultExtension(): String = "lua"
  override fun getIcon(): Icon = AllIcons.General.Language
  override fun isBinary(): Boolean = false
}

private fun isLuaLspFile(file: VirtualFile): Boolean = file.extension == "lua"

private fun findBundledLuaLanguageServer(): String? {
    val explicitPath = System.getProperty("lsp.client.playground.lua.language.server.path")
    return explicitPath
}
