package org.jetbrains.lsp.samples.bicep

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspFormattingCustomizer
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import com.intellij.platform.lsp.api.customization.LspInlayHintCustomizer
import com.intellij.platform.lsp.api.customization.LspInlayHintDisabled
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import org.jetbrains.lsp.samples.bicep.BicepBundle.BICEP_EXTENSION
import org.jetbrains.lsp.samples.bicep.infrastructure.DotnetRuntime
import org.jetbrains.lsp.samples.bicep.infrastructure.DotnetRuntimeService
import org.jetbrains.lsp.samples.bicep.infrastructure.DotnetRuntimeValidity

class BicepLspIntegrationProvider : LspIntegrationProvider {
  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    clientStarter: LspIntegrationProvider.LspClientStarter
  ) {
    if (isLsAvailableFor(file, project)) {
      clientStarter.ensureClientStarted(BicepLspDescriptor(project))
    }
  }

  override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem =
    LspClientWidgetItem(lspClient, currentFile, AllIcons.Providers.Azure)
}

class BicepLspDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "Bicep LSP") {
  override fun isSupportedFile(file: VirtualFile): Boolean {
    return file.extension == BICEP_EXTENSION
  }

  override fun createCommandLine(): GeneralCommandLine {
    return prepareBicepServerLaunchCommandLine()
  }

  override val lspCustomization: LspCustomization = object : LspCustomization() {
    override val inlayHintCustomizer: LspInlayHintCustomizer = LspInlayHintDisabled
    override val formattingCustomizer: LspFormattingCustomizer = object : LspFormattingSupport() {
      override fun shouldFormatThisFileExclusivelyByServer(file: VirtualFile,
                                                           ideCanFormatThisFileItself: Boolean,
                                                           serverExplicitlyWantsToFormatThisFile: Boolean): Boolean {
        return file.extension == BICEP_EXTENSION
      }
    }
  }
}

internal fun isLsAvailableFor(file: VirtualFile, project: Project): Boolean {
  if (file.extension != BICEP_EXTENSION) return false
  return getDotnetRuntimeValidityOrScheduleSetup(project) == DotnetRuntimeValidity.VALID
}

internal fun getDotnetRuntimeValidityOrScheduleSetup(project: Project): DotnetRuntimeValidity {
  val dotnetService = DotnetRuntimeService.getInstance(project)
  val currentValidity = dotnetService.currentValidity
  if (currentValidity != DotnetRuntimeValidity.VALID) {
    dotnetService.scheduleSetup()
  }
  return currentValidity
}

internal fun forceRunLanguageServer(project: Project) {
  LspClientManager.getInstance(project).startClientsIfNeeded(BicepLspIntegrationProvider::class.java)
}

internal fun prepareBicepServerLaunchCommandLine(): GeneralCommandLine {
  val serverPath = PluginPathManager.getPluginDistPath(
    BicepLspDescriptor::class.java, "bicep-langserver/Bicep.LangServer.dll"
  ) ?: throw ExecutionException("The bundled Bicep language server was not found")

  return GeneralCommandLine(
    DotnetRuntime.localExecutablePath,
    serverPath.toString()
  )
}
