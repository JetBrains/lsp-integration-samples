package org.jetbrains.lsp.samples.bicep

import com.intellij.ide.FileIconProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

internal class BicepFileIconProvider : FileIconProvider {
  override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
    return if (file.extension == BicepBundle.BICEP_EXTENSION) AllIcons.Providers.Azure else null
  }
}
