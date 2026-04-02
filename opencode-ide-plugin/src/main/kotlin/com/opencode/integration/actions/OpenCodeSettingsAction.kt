package com.opencode.integration.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.opencode.integration.settings.OpenCodeConfigurable

/**
 * 打开 OpenCode 设置的 Action
 */
class OpenCodeSettingsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                OpenCodeConfigurable::class.java
            )
        }
    }
}
