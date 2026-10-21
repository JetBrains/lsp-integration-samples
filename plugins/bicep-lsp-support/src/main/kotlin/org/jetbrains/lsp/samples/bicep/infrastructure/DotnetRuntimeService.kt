package org.jetbrains.lsp.samples.bicep.infrastructure

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.lsp.samples.bicep.forceRunLanguageServer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
internal class DotnetRuntimeService(private val project: Project, private val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): DotnetRuntimeService {
      return project.service<DotnetRuntimeService>()
    }
  }

  private val setupInProgress = AtomicBoolean(false)
  private val runtimeValidity = AtomicReference(DotnetRuntimeValidity.YET_UNKNOWN)

  val currentValidity: DotnetRuntimeValidity
    get() = runtimeValidity.get()

  fun scheduleSetup() {
    if (!setupInProgress.compareAndSet(false, true)) return

    runtimeValidity.set(DotnetRuntimeValidity.YET_UNKNOWN)
    coroutineScope.launch(Dispatchers.Default + ModalityState.any().asContextElement()) {
      try {
        val validity = ensureDotnetRuntimeValid()
        runtimeValidity.set(validity)
        if (validity == DotnetRuntimeValidity.VALID) {
          forceRunLanguageServer(project)
        }
      }
      catch (e: Exception) {
        rethrowControlFlowException(e)
        runtimeValidity.set(DotnetRuntimeValidity.INVALID)
        thisLogger().warn("Unable to set up the .NET runtime for Bicep", e)
      }
      finally {
        setupInProgress.compareAndSet(true, false)
      }
    }
  }

  private suspend fun ensureDotnetRuntimeValid(): DotnetRuntimeValidity {
    if (DotnetRuntime.isValid()) {
      return DotnetRuntimeValidity.VALID
    }

    if (!cleanDotnetDirectorySafe()) {
      return DotnetRuntimeValidity.INVALID
    }

    return if (downloadDotnetRuntime(project) && DotnetRuntime.isValid())
      DotnetRuntimeValidity.VALID
    else
      DotnetRuntimeValidity.INVALID
  }
}

internal enum class DotnetRuntimeValidity {
  VALID, INVALID, YET_UNKNOWN
}
