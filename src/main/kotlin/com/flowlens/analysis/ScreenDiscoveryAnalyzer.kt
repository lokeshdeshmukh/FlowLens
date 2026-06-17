package com.flowlens.analysis

import com.flowlens.domain.AnalyzerOutput
import com.flowlens.domain.DiscoveredNode
import com.flowlens.domain.NodeType
import com.flowlens.util.DisplayNames
import com.flowlens.util.NodeKeys
import org.jetbrains.uast.UClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ScreenDiscoveryAnalyzer {
    fun analyze(context: SourceAnalysisContext): AnalyzerOutput {
        val uFile = context.uFile ?: return AnalyzerOutput()
        val nodes = linkedSetOf<DiscoveredNode>()

        uFile.accept(
            object : AbstractUastVisitor() {
                override fun visitClass(node: UClass): Boolean {
                    val superTypes = node.uastSuperTypes.map { it.type.canonicalText }
                    val nodeType = when {
                        superTypes.any { it.endsWith("BottomSheetDialogFragment") } -> NodeType.BOTTOM_SHEET
                        superTypes.any { it.endsWith("DialogFragment") } -> NodeType.DIALOG
                        superTypes.any { it.endsWith("Fragment") } -> NodeType.FRAGMENT
                        superTypes.any { it.endsWith("Activity") } -> NodeType.ACTIVITY
                        else -> null
                    } ?: return super.visitClass(node)

                    val qualifiedName = node.qualifiedName ?: node.name ?: return super.visitClass(node)
                    nodes += DiscoveredNode(
                        key = NodeKeys.className(qualifiedName),
                        name = DisplayNames.simpleClassName(qualifiedName).ifBlank { qualifiedName },
                        type = nodeType,
                        filePath = context.filePath,
                        className = qualifiedName,
                        relatedViewModels = context.relatedViewModels,
                        relatedRepositories = context.relatedRepositories,
                    )
                    return super.visitClass(node)
                }
            },
        )

        return AnalyzerOutput(nodes = nodes.toList())
    }
}
