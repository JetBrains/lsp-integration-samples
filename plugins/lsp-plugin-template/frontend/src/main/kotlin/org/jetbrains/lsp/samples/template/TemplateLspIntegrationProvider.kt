// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.template

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettings
import com.intellij.platform.lsp.api.LspPluginServerConfiguration
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.impl.createInitializationOptions

// TODO: Rename the package and classes for your language, then update the XML registrations.
class TemplateLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        // TODO: Check whether the opened file is supported, then call
        // clientStarter.ensureClientStarted with your server descriptor.
    }
}

class TemplateLspServerDescriptor(
    project: Project,
    private val configuration: LspPluginServerConfiguration =
        LspIntegrationSettings.getInstanceOrNull(project)
            ?.getPluginConfiguration(TemplateLspServerSettingsProvider.SERVER_ID)
            ?: DEFAULT_CONFIGURATION,
) : ProjectWideLspClientDescriptor(project, configuration.name) {
    override fun isSupportedFile(file: VirtualFile): Boolean = configuration.isSupportedFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        // TODO: Resolve your language server executable on the project execution host.
        return GeneralCommandLine("path/to/language-server").apply {
            addParameters(configuration.arguments)
            configuration.environmentVariables.configureCommandLine(this)
        }
    }

    override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}
