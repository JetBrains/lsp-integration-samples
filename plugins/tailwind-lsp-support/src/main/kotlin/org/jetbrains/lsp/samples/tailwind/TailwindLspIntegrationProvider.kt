// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.tailwind

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import com.intellij.platform.lsp.impl.nodeRuntime.LspNodeRuntimeManager
import com.intellij.platform.lsp.impl.nodeRuntime.withNodeRuntimeEnsured
import java.nio.file.Files


class TailwindLspIntegrationProvider : LspIntegrationProvider {
  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    clientStarter: LspIntegrationProvider.LspClientStarter,
  ) {
    if (isTailwindLspFile(file)) {
      withNodeRuntimeEnsured(project) {
        clientStarter.ensureClientStarted(TailwindLspServerDescriptor(project))
      }
    }
  }

  override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem {
    return LspClientWidgetItem(lspClient, currentFile, AllIcons.FileTypes.Css)
  }
}

class TailwindLspServerDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "Tailwind CSS") {
  override fun isSupportedFile(file: VirtualFile): Boolean = isTailwindLspFile(file)

  override fun createCommandLine(): GeneralCommandLine {
    val runtime = LspNodeRuntimeManager.getInstance().getRuntime()
                  ?: throwMissingLspExecutable(project, "Tailwind CSS", "tailwind.lsp.node.runtime.not.found")
    val serverPackage = findBundledTailwindLanguageServer()
                        ?: throwMissingLspExecutable(project, "Tailwind CSS", "tailwind.lsp.server.not.found")

    // `--no` forbids downloading anything, and `--prefix` makes npx take the bundled package as the
    // local one, so the executable comes from its `bin` entries. Without `--prefix` npx would
    // resolve against the working directory below and pick up the project's own install instead.
    val commandLine = GeneralCommandLine(
      runtime.npx.toString(),
      "--no",
      "--prefix",
      serverPackage,
      "tailwindcss-language-server",
      "--stdio",
    )
    return runtime.applyTo(commandLine).withWorkDirectory(project.basePath)
  }
}

private val SUPPORTED_EXTENSIONS = setOf("css", "scss", "less", "html", "js", "jsx", "ts", "tsx")

private fun isTailwindLspFile(file: VirtualFile): Boolean = file.extension in SUPPORTED_EXTENSIONS

private fun findBundledTailwindLanguageServer(): String? {
  val plugin = PluginManagerCore.getPlugin(
    PluginId.getId("org.jetbrains.tailwind-lsp-support")
  ) ?: return null

  return plugin.pluginPath
    .resolve("tailwindcss-language-server")
    .takeIf { Files.isDirectory(it) }
    ?.toString()
}
