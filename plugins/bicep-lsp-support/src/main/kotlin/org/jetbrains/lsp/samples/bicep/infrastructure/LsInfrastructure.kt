package org.jetbrains.lsp.samples.bicep.infrastructure

import org.jetbrains.lsp.samples.bicep.BicepBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

private const val DOTNET_VERSION = "10.0"

internal sealed class LsInfrastructure(protected val urlTemplate: String) {
  data object BicepLS : LsInfrastructure("https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip") {
    override val formattedUrl: String
      get() {
        val supportedVersion = "v0.48.1"
        return urlTemplate.format(supportedVersion)
      }

    override val extractedDirectoryName: String
      get() = "bicep-langserver"

    override val executableName: String
      get() = "Bicep.LangServer.dll"

    override val presentableName: String
      get() = BicepBundle.message("progress.title.load.ls")

    override val localExecutablePath: String
      get() = findExecutableInPluginTempDirectory()
  }

  data object DotnetRuntime : LsInfrastructure("https://aka.ms/dotnet/$DOTNET_VERSION/dotnet-sdk-%s-%s.%s") {
    override val formattedUrl: String?
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
        return urlTemplate.format(osClassifier, architectureClassifier, extension)
      }

    override val extractedDirectoryName: String
      get() = "dotnet-sdk"

    override val executableName: String
      get() = if (SystemInfo.isWindows) "dotnet.exe" else "dotnet"

    override val presentableName: String
      get() = BicepBundle.message("progress.title.load.dotnet")

    override val localExecutablePath: String
      get() = findExistingDotnetExecutable() ?: findExecutableInPluginTempDirectory()

    override fun isValid(): Boolean {
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
  }

  abstract val formattedUrl: @NlsSafe String?
  abstract val extractedDirectoryName: @NlsSafe String
  abstract val executableName: @NlsSafe String
  abstract val presentableName: @Nls String
  abstract val localExecutablePath: @NlsSafe String

  @RequiresBackgroundThread(generateAssertion = false /* IJPL-115548 */)
  @RequiresReadLockAbsence
  fun isPresent(): Boolean {
    return localExecutablePath.toNioPathOrNull()?.exists() ?: false
  }

  @RequiresBackgroundThread(generateAssertion = false /* IJPL-115548 */)
  @RequiresReadLockAbsence
  open fun isValid(): Boolean {
    return isPresent()
  }

  protected fun findExecutableInPluginTempDirectory(): String {
    val subPath = "$extractedDirectoryName/$executableName"
    return PLUGIN_TMP_PATH.resolve(subPath)
      .absolutePathString()
      .let(PathUtil::toSystemDependentName)
  }

  companion object {
    fun allKnown() = sequenceOf(BicepLS, DotnetRuntime)
  }
}
