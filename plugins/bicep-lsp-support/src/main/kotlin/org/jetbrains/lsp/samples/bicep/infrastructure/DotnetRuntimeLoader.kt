package org.jetbrains.lsp.samples.bicep.infrastructure

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import org.jetbrains.lsp.samples.bicep.BicepBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension


internal fun cleanDotnetDirectorySafe(): Boolean {
  return runCatching {
    cleanDotnetDirectory()
  }.onFailureNoPce { exception ->
    LOG.warn("Unable to clean download directory `${DotnetRuntime.extractedDirectoryName}`. Abort .NET installation", exception)
  }.isSuccess
}

@OptIn(ExperimentalPathApi::class)
private fun cleanDotnetDirectory() {
  PLUGIN_TMP_PATH.resolve(DotnetRuntime.extractedDirectoryName)
    .takeIf { it.exists() }
    ?.deleteRecursively()
}

internal suspend fun downloadDotnetRuntime(project: Project): Boolean {
  if (DotnetRuntime.isPresent()) return true

  return withBackgroundProgress(project, BicepBundle.message("progress.title.load.dotnet")) {
    reportSequentialProgress { reporter ->
      val archive = reporter.nextStep(endFraction = 80, text = BicepBundle.message("progress.title.load", DotnetRuntime.presentableName)) {
        downloadResourceSafe()
      } ?: return@reportSequentialProgress false

      val unpacked = reporter.nextStep(100, text = BicepBundle.message("progress.title.unpack", DotnetRuntime.presentableName)) {
        unzipResourceSafe(archive)
      }

      unpacked != null && DotnetRuntime.isPresent()
    }
  }
}

private fun downloadResourceSafe(): Path? {
  return runCatching {
    downloadResource()
  }.onFailureNoPce { exception ->
    LOG.warn("Unable to download ${DotnetRuntime.presentableName}. Abort .NET installation", exception)
  }.getOrNull()
}

private fun downloadResource(): Path? {
  val effectiveUrl = DotnetRuntime.formattedUrl
  if (effectiveUrl == null) {
    LOG.warn("Unable to prepare effective url for .NET and current OS")
    return null
  }
  val targetFile = PLUGIN_TMP_PATH.resolve(effectiveUrl.substringAfterLast("/"))
  if (targetFile.exists()) return targetFile

  return HttpRequests.request(effectiveUrl)
    .connect { request ->
      LOG.warn("Started loading of ${DotnetRuntime.presentableName}")
      val downloadedPath = request.saveToFile(targetFile, ProgressManager.getGlobalProgressIndicator())
      LOG.warn("Loaded ${DotnetRuntime.presentableName} to `$downloadedPath`")
      downloadedPath
    }
}

private fun unzipResourceSafe(resourcePath: Path): Path? {
  return runCatching {
    unzipResource(resourcePath)
  }.onFailureNoPce { exception ->
    LOG.warn("Unable to extract ${DotnetRuntime.presentableName} archive. Abort .NET installation", exception)
    Files.delete(resourcePath)
  }.getOrNull()
}

private fun unzipResource(resourcePath: Path): Path? {
  val decompressor = when (resourcePath.extension) {
    "zip" -> Decompressor.Zip(resourcePath)
    "gz", "tgz" -> Decompressor.Tar(resourcePath)
    else -> {
      LOG.warn("Downloaded resource '${DotnetRuntime.presentableName}' has unexpected extension '${resourcePath.extension}'. " +
               "Abort installation")
      return null
    }
  }
  val target = PLUGIN_TMP_PATH.resolve(DotnetRuntime.extractedDirectoryName)
  if (target.exists()) return target
  decompressor.extract(target)
  Files.delete(resourcePath)
  return target
}

private fun <T> Result<T>.onFailureNoPce(action: (exception: Throwable) -> Unit): Result<T> {
  val wrappedAction = { exception: Throwable ->
    if (exception is ProcessCanceledException)
      throw exception
    else
      action(exception)
  }
  return this.onFailure(wrappedAction)
}


internal val PLUGIN_TMP_PATH = Path.of(PathManager.getSystemPath()).resolve("bicep")

private val LOG
  get() = Logger.getInstance("dotnetRuntimeLoader")
