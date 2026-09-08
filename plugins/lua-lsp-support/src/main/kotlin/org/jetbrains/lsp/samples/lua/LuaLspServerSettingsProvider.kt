// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.lua

import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.impl.LspPluginServerConfiguration
import com.intellij.platform.lsp.impl.LspServerSettingsProvider

class LuaLspServerSettingsProvider : LspServerSettingsProvider {
    override val serverId: String = SERVER_ID
    override val integrationProviderClass: Class<out LspIntegrationProvider> = LuaLspIntegrationProvider::class.java
    override val defaultConfiguration: LspPluginServerConfiguration = DEFAULT_CONFIGURATION

    companion object {
        const val SERVER_ID: String = "lua"
        val DEFAULT_CONFIGURATION: LspPluginServerConfiguration = LspPluginServerConfiguration(
            name = "Lua",
            filePatterns = listOf("*.lua"),
        )
    }
}
