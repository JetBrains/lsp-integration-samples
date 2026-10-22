// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.template

import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettingsProvider
import com.intellij.platform.lsp.api.LspPluginServerConfiguration

class TemplateLspServerSettingsProvider : LspIntegrationSettingsProvider {
    override val serverId: String = SERVER_ID
    override val integrationProviderClass: Class<out LspIntegrationProvider> = TemplateLspIntegrationProvider::class.java
    override val defaultConfiguration: LspPluginServerConfiguration = DEFAULT_CONFIGURATION

    companion object {
        // TODO: Choose a stable ID unique among the project's LSP servers.
        const val SERVER_ID: String = "template-language-server"
    }
}

val DEFAULT_CONFIGURATION: LspPluginServerConfiguration = LspPluginServerConfiguration(
    // TODO: Set the language server's display name.
    name = "Your language",
    // TODO: Add supported file patterns, for example listOf("*.your-extension").
    filePatterns = emptyList(),
    // TODO: Add arguments (such as --stdio), initializationOptions, or environmentVariables if your server needs them.
)
