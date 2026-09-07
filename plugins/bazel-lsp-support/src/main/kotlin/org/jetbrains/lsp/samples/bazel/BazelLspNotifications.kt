// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.lsp.samples.bazel

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.PropertyKey

private const val NOTIFICATION_GROUP_ID = "LSP window/showMessage"
private const val BUNDLE = "messages.BazelLspBundle"

internal fun throwMissingLspExecutable(
  project: Project,
  languageName: String,
  @PropertyKey(resourceBundle = BUNDLE) messageKey: String,
): Nothing {
  val message = BazelLspBundle.message(messageKey)
  NotificationGroupManager.getInstance()
    .getNotificationGroup(NOTIFICATION_GROUP_ID)
    .createNotification(
      BazelLspBundle.message("lsp.executable.not.found.notification.title", languageName),
      message,
      NotificationType.ERROR,
    )
    .notify(project)
  throw ExecutionException(message)
}
