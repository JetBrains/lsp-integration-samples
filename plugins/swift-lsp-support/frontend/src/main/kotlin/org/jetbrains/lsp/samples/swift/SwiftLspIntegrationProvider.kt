// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.swift

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettings
import com.intellij.platform.lsp.api.LspPluginServerConfiguration
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.impl.createInitializationOptions

class SwiftLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        val descriptor = SwiftLspServerDescriptor(project)
        if (descriptor.isSupportedFile(file)) {
            clientStarter.ensureClientStarted(descriptor)
        }
    }
}

class SwiftLspServerDescriptor(
    project: Project,
    private val configuration: LspPluginServerConfiguration =
        LspIntegrationSettings.getInstanceOrNull(project)
            ?.getPluginConfiguration(SwiftLspServerSettingsProvider.SERVER_ID)
            ?: DEFAULT_CONFIGURATION,
) : ProjectWideLspClientDescriptor(project, configuration.name) {
    override fun isSupportedFile(file: VirtualFile): Boolean = configuration.isSupportedFile(file)

    override fun createCommandLine(): GeneralCommandLine {
        val executable = SwiftToolchainResolver.resolve(project, configuration.environmentVariables)
        return GeneralCommandLine(executable.asEelPath().toString()).apply {
            addParameters(configuration.arguments)
            configuration.environmentVariables.configureCommandLine(this)
        }
    }

    override fun createInitializationOptions(): Any? = configuration.createInitializationOptions()
}
