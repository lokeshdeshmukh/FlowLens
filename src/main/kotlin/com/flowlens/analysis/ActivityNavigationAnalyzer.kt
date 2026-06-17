package com.flowlens.analysis

import com.flowlens.domain.AnalyzerOutput
import com.flowlens.domain.DiscoveredConnection
import com.flowlens.domain.DiscoveredNode
import com.flowlens.domain.NavigationKind
import com.flowlens.domain.NodeType
import com.flowlens.util.DisplayNames
import com.flowlens.util.NodeKeys
import com.flowlens.util.SourceTextParsers
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ActivityNavigationAnalyzer {
    fun analyze(context: SourceAnalysisContext): AnalyzerOutput {
        val uFile = context.uFile ?: return AnalyzerOutput()
        val nodes = linkedSetOf<DiscoveredNode>()
        val connections = linkedSetOf<DiscoveredConnection>()

        uFile.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val methodName = node.methodName.orEmpty()
                    val source = node.sourcePsi?.text ?: node.asSourceString()
                    val container = node.getContainingUClass()

                    if (methodName.startsWith("startActivity")) {
                        val fromKey = screenKey(container) ?: return super.visitCallExpression(node)
                        val targetClass = SourceTextParsers.extractIntentTarget(source)
                            ?: extractIntentTargetFromArgument(node.valueArguments.firstOrNull())
                        if (targetClass != null) {
                            nodes += DiscoveredNode(
                                key = NodeKeys.className(targetClass),
                                name = DisplayNames.simpleClassName(targetClass).ifBlank { targetClass },
                                type = NodeType.ACTIVITY,
                                filePath = context.filePath,
                                className = targetClass,
                            )
                            connections += DiscoveredConnection(
                                fromKey = fromKey,
                                toKey = NodeKeys.className(targetClass),
                                kind = NavigationKind.ACTIVITY_INTENT,
                                label = methodName,
                                sourceFilePath = context.filePath,
                            )
                        }
                    }

                    if (methodName == "navigate") {
                        val sourceKey = screenKey(container) ?: return super.visitCallExpression(node)
                        val receiverText = node.receiver?.sourcePsi?.text ?: node.receiver?.asSourceString().orEmpty()
                        if ("findNavController" !in receiverText && "navController" !in receiverText) {
                            return super.visitCallExpression(node)
                        }
                        val target = SourceTextParsers.extractNavigationTarget(source) ?: return super.visitCallExpression(node)
                        connections += DiscoveredConnection(
                            fromKey = sourceKey,
                            toKey = if (target.startsWith("R.id.") || source.contains("R.id.")) {
                                NodeKeys.xmlId(target)
                            } else {
                                NodeKeys.route(target)
                            },
                            kind = NavigationKind.NAV_CONTROLLER,
                            label = "navigate",
                            sourceFilePath = context.filePath,
                        )
                    }

                    return super.visitCallExpression(node)
                }
            },
        )

        return AnalyzerOutput(nodes = nodes.toList(), connections = connections.toList())
    }

    private fun screenKey(container: UClass?): String? {
        val qualifiedName = container?.qualifiedName ?: return null
        return NodeKeys.className(qualifiedName)
    }

    private fun extractIntentTargetFromArgument(argument: UElement?): String? {
        val source = argument?.sourcePsi?.text ?: argument?.asSourceString() ?: return null
        return SourceTextParsers.extractIntentTarget(source)
    }
}
