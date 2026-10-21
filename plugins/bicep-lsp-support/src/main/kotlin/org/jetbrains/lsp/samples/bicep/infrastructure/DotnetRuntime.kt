package org.jetbrains.lsp.samples.bicep.infrastructure

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.lsp.samples.bicep.BicepBundle
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

private const val DOTNET_VERSION = "10.0"

internal object DotnetRuntime {
  private const val URL_TEMPLATE = "https://aka.ms/dotnet/$DOTNET_VERSION/dotnet-sdk-%s-%s.%s"

  val formattedUrl: String?
    get() {
      val osClassifier = when {
        SystemInfo.isWindows -> "win"
        SystemInfo.isLinux -> "linux"
        SystemInfo.isMac -> "osx"
        else -> return null
      }
      val architectureClassifier = when (SystemInfo.OS_ARCH) {
        "x64", "amd64", "ia64", "x86_64" -> "x64"
        "x86" -> "x86"
        "arm64", "aarch64" -> "arm64"
        "arm32" -> "arm32"
        else -> {
          thisLogger().warn("Unsupported os arch ${SystemInfo.OS_ARCH} - dotnet runtime could not be installed")
          return null
        }
      }
      val extension = when {
        SystemInfo.isWindows -> "zip"
        else -> "tar.gz"
      }
      return URL_TEMPLATE.format(osClassifier, architectureClassifier, extension)
    }

  val extractedDirectoryName: String
    get() = "dotnet-sdk"

  val executableName: String
    get() = if (SystemInfo.isWindows) "dotnet.exe" else "dotnet"

  val presentableName: String
    get() = BicepBundle.message("progress.title.load.dotnet")

  val localExecutablePath: String
    get() = findExistingDotnetExecutable() ?: findExecutableInPluginTempDirectory()

  fun isValid(): Boolean {
    val executablePath = localExecutablePath.toNioPathOrNull()?.takeIf(Path::exists) ?: return false
    return isAcceptableDotnetSdkVersion(executablePath.absolutePathString())
  }

  private fun findExistingDotnetExecutable(): String? {
    val osSpecificWhich = if (SystemInfo.isWindows) "where" else "which"
    val existingDotnetSdk = ExecUtil.execAndReadLine(GeneralCommandLine(osSpecificWhich, executableName))?.let(PathUtil::toSystemDependentName)

    return if (!existingDotnetSdk.isNullOrBlank() && isAcceptableDotnetSdkVersion(existingDotnetSdk)) {
      existingDotnetSdk
    }
    else {
      null
    }
  }

  private fun isAcceptableDotnetSdkVersion(executablePath: String): Boolean {
    val availableRuntimes = ExecUtil.execAndGetOutput(
      GeneralCommandLine(executablePath, "--list-runtimes")
    ).stdoutLines

    val isAcceptableSdkVersion = listOf(
      "Microsoft.NETCore.App $DOTNET_VERSION."
    ).any { requiredRuntime ->
      availableRuntimes.any { availableRuntime ->
        availableRuntime.startsWith(requiredRuntime)
      }
    }
    thisLogger().warn("Available runtimes for Bicep language server: $availableRuntimes. Is acceptable version: $isAcceptableSdkVersion")
    return isAcceptableSdkVersion
  }

  @RequiresBackgroundThread(generateAssertion = false /* IJPL-115548 */)
  @RequiresReadLockAbsence
  fun isPresent(): Boolean {
    return localExecutablePath.toNioPathOrNull()?.exists() ?: false
  }

  private fun findExecutableInPluginTempDirectory(): String {
    return PLUGIN_TMP_PATH.resolve("$extractedDirectoryName/$executableName")
      .absolutePathString()
      .let(PathUtil::toSystemDependentName)
  }
}
