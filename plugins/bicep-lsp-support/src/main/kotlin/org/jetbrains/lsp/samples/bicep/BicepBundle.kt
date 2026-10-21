package org.jetbrains.lsp.samples.bicep

import com.intellij.DynamicBundle
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

private const val PATH_TO_BUNDLE = "messages.BicepBundle"

object BicepBundle {
  private val instance = DynamicBundle(BicepBundle::class.java, PATH_TO_BUNDLE)
  const val BICEP_EXTENSION: @NlsSafe String = "bicep"

  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String = instance.getMessage(key, *params)

  @JvmStatic
  fun messagePointer(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): Supplier<@Nls String> {
    return instance.getLazyMessage(key, *params)
  }
}
