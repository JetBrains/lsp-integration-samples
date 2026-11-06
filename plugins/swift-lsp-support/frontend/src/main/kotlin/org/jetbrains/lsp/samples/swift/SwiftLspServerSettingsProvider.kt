// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.swift

import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.LspIntegrationSettingsProvider
import com.intellij.platform.lsp.api.LspPluginServerConfiguration
import com.intellij.platform.lsp.api.LspServerEnvironmentData

class SwiftLspServerSettingsProvider : LspIntegrationSettingsProvider {
    override val serverId: String = SERVER_ID
    override val integrationProviderClass: Class<out LspIntegrationProvider> = SwiftLspIntegrationProvider::class.java
    override val defaultConfiguration: LspPluginServerConfiguration = DEFAULT_CONFIGURATION

    companion object {
        const val SERVER_ID: String = "swift-sourcekit-lsp"
    }
}

val DEFAULT_CONFIGURATION: LspPluginServerConfiguration = LspPluginServerConfiguration(
    name = "Swift (SourceKit-LSP)",
    filePatterns = listOf("*.swift"),
    // An empty value enables automatic discovery. Edit this in the platform's Language Servers settings.
    environmentVariables = LspServerEnvironmentData(variables = mapOf(SwiftToolchainResolver.TOOLCHAIN_VARIABLE to "")),
)
