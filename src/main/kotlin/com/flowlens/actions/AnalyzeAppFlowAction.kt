package com.flowlens.actions

import com.flowlens.ui.FlowGraphToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class AnalyzeAppFlowAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Flow Graph") ?: return
        toolWindow.show {
            toolWindow.activate {
                val panel = toolWindow.contentManager.contents.firstOrNull()?.component as? FlowGraphToolWindowPanel
                panel?.runAnalysis(forceRefresh = false)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
