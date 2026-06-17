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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ComposeNavigationAnalyzer {
    private val routeBuilders = setOf("composable", "dialog", "bottomSheet")
    private val sheetHeuristics = setOf("ModalBottomSheet", "BottomSheetScaffold")

    fun analyze(context: SourceAnalysisContext): AnalyzerOutput {
        val uFile = context.uFile ?: return AnalyzerOutput()
        val nodes = linkedSetOf<DiscoveredNode>()
        val connections = linkedSetOf<DiscoveredConnection>()
        val startNodeKeys = linkedSetOf<String>()

        uFile.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val methodName = node.methodName.orEmpty()
                    val source = node.sourcePsi?.text ?: node.asSourceString()

                    if (methodName == "NavHost") {
                        SourceTextParsers.extractStartDestination(source)?.let { start ->
                            startNodeKeys += NodeKeys.route(start)
                        }
                    }

                    if (methodName in routeBuilders) {
                        val route = SourceTextParsers.extractRouteDeclaration(source) ?: return super.visitCallExpression(node)
                        val type = when (methodName) {
                            "dialog" -> NodeType.DIALOG
                            "bottomSheet" -> NodeType.BOTTOM_SHEET
                            else -> NodeType.COMPOSE_SCREEN
                        }
                        nodes += DiscoveredNode(
                            key = NodeKeys.route(route),
                            name = DisplayNames.humanizeRoute(route),
                            type = type,
                            filePath = context.filePath,
                            route = route,
                            deepLinks = SourceTextParsers.extractDeepLinks(source),
                            relatedViewModels = context.relatedViewModels,
                            relatedRepositories = context.relatedRepositories,
                        )
                    }

                    if (methodName == "navigate") {
                        val sourceRoute = findEnclosingRoute(node)
                        val target = SourceTextParsers.extractNavigationTarget(source)
                        if (sourceRoute != null && target != null) {
                            connections += DiscoveredConnection(
                                fromKey = NodeKeys.route(sourceRoute),
                                toKey = resolveTargetKey(target),
                                kind = NavigationKind.COMPOSE,
                                label = "navigate",
                                sourceFilePath = context.filePath,
                            )
                        }
                    }

                    if (methodName in sheetHeuristics) {
                        val ownerRoute = findEnclosingRoute(node)
                        if (ownerRoute != null) {
                            val ownerKey = NodeKeys.route(ownerRoute)
                            val sheetKey = NodeKeys.bottomSheet(ownerKey, methodName)
                            nodes += DiscoveredNode(
                                key = sheetKey,
                                name = "${DisplayNames.humanizeRoute(ownerRoute)} Sheet",
                                type = NodeType.BOTTOM_SHEET,
                                filePath = context.filePath,
                                relatedViewModels = context.relatedViewModels,
                                relatedRepositories = context.relatedRepositories,
                                metadata = mapOf("heuristic" to methodName),
                            )
                            connections += DiscoveredConnection(
                                fromKey = ownerKey,
                                toKey = sheetKey,
                                kind = NavigationKind.HEURISTIC,
                                label = methodName,
                                sourceFilePath = context.filePath,
                            )
                        }
                    }

                    return super.visitCallExpression(node)
                }
            },
        )

        return AnalyzerOutput(
            nodes = nodes.toList(),
            connections = connections.toList(),
            startNodeKeys = startNodeKeys,
        )
    }

    private fun findEnclosingRoute(element: UElement): String? {
        var current: UElement? = element
        while (current != null) {
            if (current is UCallExpression && current.methodName in routeBuilders) {
                val source = current.sourcePsi?.text ?: current.asSourceString()
                return SourceTextParsers.extractRouteDeclaration(source)
            }
            current = current.uastParent
        }
        return null
    }

    private fun resolveTargetKey(target: String): String =
        if (target.contains('.')) NodeKeys.route(target) else NodeKeys.route(target)
}
