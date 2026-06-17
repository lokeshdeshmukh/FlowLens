package com.flowlens.ui

import com.flowlens.domain.NodeType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object FlowIcons {
    val activity: Icon = IconLoader.getIcon("/icons/nodeActivity.svg", FlowIcons::class.java)
    val fragment: Icon = IconLoader.getIcon("/icons/nodeFragment.svg", FlowIcons::class.java)
    val compose: Icon = IconLoader.getIcon("/icons/nodeCompose.svg", FlowIcons::class.java)
    val bottomSheet: Icon = IconLoader.getIcon("/icons/nodeBottomSheet.svg", FlowIcons::class.java)
    val dialog: Icon = IconLoader.getIcon("/icons/nodeDialog.svg", FlowIcons::class.java)
    val deepLink: Icon = IconLoader.getIcon("/icons/nodeDeepLink.svg", FlowIcons::class.java)

    fun forType(type: NodeType): Icon = when (type) {
        NodeType.ACTIVITY -> activity
        NodeType.FRAGMENT -> fragment
        NodeType.COMPOSE_SCREEN -> compose
        NodeType.BOTTOM_SHEET -> bottomSheet
        NodeType.DIALOG -> dialog
        NodeType.DEEP_LINK -> deepLink
    }
}

