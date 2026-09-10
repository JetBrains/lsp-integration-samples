// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.tailwind

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import com.intellij.platform.lsp.impl.nodeRuntime.withNodeRuntimeEnsured


class CssLspIntegrationProvider : LspIntegrationProvider {
  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    clientStarter: LspIntegrationProvider.LspClientStarter,
  ) {
    if (isCssLspFile(file)) {
      withNodeRuntimeEnsured(project) {
        clientStarter.ensureClientStarted(CssLspServerDescriptor(project))
      }
    }
  }

  override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
    return LspClientWidgetItem(lspClient, currentFile, AllIcons.FileTypes.Css)
  }
}

class CssLspServerDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "CSS") {
  override fun isSupportedFile(file: VirtualFile): Boolean = isCssLspFile(file)

  override fun createCommandLine(): GeneralCommandLine =
    createBundledServerCommandLine(project, "css-language-server", presentableName)
}

// The server picks its dialect from the language ID, and handles `css`, `scss`, and `less`.
private val SUPPORTED_EXTENSIONS = setOf("css", "scss", "less")

private fun isCssLspFile(file: VirtualFile): Boolean = file.extension in SUPPORTED_EXTENSIONS
