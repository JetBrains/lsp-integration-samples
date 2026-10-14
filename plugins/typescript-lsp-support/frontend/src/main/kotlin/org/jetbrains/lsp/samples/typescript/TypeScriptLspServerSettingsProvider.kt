// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.typescript

import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettingsProvider
import com.intellij.platform.lsp.api.LspPluginServerConfiguration

class TypeScriptLspServerSettingsProvider : LspIntegrationSettingsProvider {
    override val serverId: String = SERVER_ID
    override val integrationProviderClass: Class<out LspIntegrationProvider> = TypeScriptLspIntegrationProvider::class.java
    override val defaultConfiguration: LspPluginServerConfiguration = DEFAULT_CONFIGURATION

    companion object {
        const val SERVER_ID: String = "org.jetbrains.typescript-lsp-support"
    }
}

val DEFAULT_CONFIGURATION: LspPluginServerConfiguration = LspPluginServerConfiguration(
    name = "TypeScript",
    arguments = listOf("--lsp", "--stdio"),
    filePatterns = listOf("*.ts", "*.tsx", "*.mts", "*.cts", "*.js", "*.jsx", "*.mjs", "*.cjs"),
)
