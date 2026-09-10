// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.tailwind

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.impl.nodeRuntime.LspNodeRuntimeManager
import java.nio.file.Files

private const val PLUGIN_ID = "org.jetbrains.tailwind-lsp-support"

// Where `layoutTailwindServer` puts the unpacked npm package inside the plugin.
private const val SERVER_PACKAGE_DIRECTORY = "tailwindcss-language-server"

/**
 * The bundled `@tailwindcss/language-server` package ships two servers, `tailwindcss-language-server`
 * and `css-language-server`, so both are started the same way, with the [executable] naming the one
 * to run.
 */
internal fun createBundledServerCommandLine(
  project: Project,
  executable: String,
  languageName: String,
): GeneralCommandLine {
  val runtime = LspNodeRuntimeManager.getInstance().getRuntime()
                ?: throwMissingLspExecutable(project, languageName, "tailwind.lsp.node.runtime.not.found")
  val serverPackage = findBundledServerPackage()
                      ?: throwMissingLspExecutable(project, languageName, "tailwind.lsp.server.not.found")

  // `--no` forbids downloading anything, and `--prefix` makes npx take the bundled package as the
  // local one, so the executable comes from its `bin` entries. Without `--prefix` npx would resolve
  // against the working directory below and pick up the project's own install instead.
  val commandLine = GeneralCommandLine(
    runtime.npx.toString(),
    "--no",
    "--prefix",
    serverPackage,
    executable,
    "--stdio",
  )
  return runtime.applyTo(commandLine).withWorkDirectory(project.basePath)
}

private fun findBundledServerPackage(): String? {
  val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)) ?: return null

  return plugin.pluginPath
    .resolve(SERVER_PACKAGE_DIRECTORY)
    .takeIf { Files.isDirectory(it) }
    ?.toString()
}
