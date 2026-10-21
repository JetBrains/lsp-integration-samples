package org.jetbrains.lsp.samples.bicep.infrastructure

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.lsp.samples.bicep.forceRunLanguageServer
import org.jetbrains.lsp.samples.bicep.prepareBicepServerLaunchCommandLine
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
internal class BicepSchedulingService(private val project: Project, val coroutineScope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): BicepSchedulingService {
      return project.service<BicepSchedulingService>()
    }
  }

  private val setupInProgress = MutableStateFlow(false)
  private val lsVerified = AtomicReference(LsValidity.YET_UNKNOWN)

  val currentLsInfrastructureValidity: LsValidity
    get() = lsVerified.get()

  fun scheduleLsSetup() {
    if (!setupInProgress.compareAndSet(false, true)) return

    lsVerified.set(LsValidity.YET_UNKNOWN)
    coroutineScope.launch(Dispatchers.Default + ModalityState.any().asContextElement()) {
      try {
        val infrastructureValidity = ensureInfrastructureValid()
        lsVerified.set(infrastructureValidity)
        if (infrastructureValidity == LsValidity.VALID) {
          forceRunLanguageServer(project)
        }
      }
      catch (e: Exception) {
        rethrowControlFlowException(e)
        lsVerified.set(LsValidity.INVALID)
        thisLogger().warn("Unable to set up Bicep language server", e)
      }
      finally {
        setupInProgress.compareAndSet(true, false)
      }
    }
  }

  private suspend fun ensureInfrastructureValid(): LsValidity {
    if (isInfrastructurePresent() && isInfrastructureValid() && tryRunServerAndCheckNoErrorsInOutput()) {
      return LsValidity.VALID
    }

    if (!cleanTmpDirectorySafe(LsInfrastructure.BicepLS) || !cleanTmpDirectorySafe(LsInfrastructure.DotnetRuntime)) {
      return LsValidity.INVALID
    }

    return if (downloadLsInfrastructure(project) && isInfrastructureValid() && tryRunServerAndCheckNoErrorsInOutput())
      LsValidity.VALID
    else
      LsValidity.INVALID
  }

  private fun isInfrastructureValid(): Boolean {
    return LsInfrastructure.allKnown().all(LsInfrastructure::isValid)
  }

  private suspend fun tryRunServerAndCheckNoErrorsInOutput(): Boolean {
    val testCommandLine = prepareBicepServerLaunchCommandLine()
    val testOutput = withContext(Dispatchers.IO) {
      CapturingProcessHandler.Silent(testCommandLine).runProcess(1000, true)
    }
    if (testOutput.stderr.isNotBlank()) {
      thisLogger().warn("Bicep language server output stderr: ${testOutput.stderr}")
      return false
    }

    return true
  }

  private fun isInfrastructurePresent(): Boolean {
    return LsInfrastructure.allKnown().all(LsInfrastructure::isPresent)
  }
}

internal enum class LsValidity {
  VALID, INVALID, YET_UNKNOWN
}
