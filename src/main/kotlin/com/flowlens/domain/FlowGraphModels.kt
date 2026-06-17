package com.flowlens.domain

import java.time.Instant

enum class NodeType {
    ACTIVITY,
    FRAGMENT,
    COMPOSE_SCREEN,
    BOTTOM_SHEET,
    DIALOG,
    DEEP_LINK,
}

enum class NavigationKind {
    NAVIGATION_XML,
    NAV_CONTROLLER,
    COMPOSE,
    ACTIVITY_INTENT,
    DEEP_LINK,
    HEURISTIC,
}

data class ScreenNode(
    val id: String,
    val name: String,
    val type: NodeType,
    val filePath: String? = null,
    val route: String? = null,
    val className: String? = null,
    val deepLinks: Set<String> = emptySet(),
    val incomingPaths: Set<String> = emptySet(),
    val outgoingPaths: Set<String> = emptySet(),
    val relatedViewModels: Set<String> = emptySet(),
    val relatedRepositories: Set<String> = emptySet(),
    val aliases: Set<String> = emptySet(),
    val isStartDestination: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
)

data class FlowConnection(
    val from: String,
    val to: String,
    val kind: NavigationKind,
    val label: String? = null,
    val sourceFilePath: String? = null,
)

data class FlowGraph(
    val screens: List<ScreenNode>,
    val connections: List<FlowConnection>,
    val startNodeIds: Set<String>,
    val generatedAt: Instant = Instant.now(),
) {
    private val byId: Map<String, ScreenNode> by lazy { screens.associateBy(ScreenNode::id) }

    fun node(id: String): ScreenNode? = byId[id]

    fun incoming(nodeId: String): List<FlowConnection> = connections.filter { it.to == nodeId }

    fun outgoing(nodeId: String): List<FlowConnection> = connections.filter { it.from == nodeId }

    fun findScreen(query: String): ScreenNode? {
        val normalized = query.trim().lowercase()
        return screens.firstOrNull { node ->
            node.name.lowercase().contains(normalized) ||
                node.route?.lowercase()?.contains(normalized) == true ||
                node.className?.lowercase()?.contains(normalized) == true
        }
    }
}

data class DiscoveredNode(
    val key: String,
    val name: String,
    val type: NodeType,
    val filePath: String? = null,
    val route: String? = null,
    val className: String? = null,
    val deepLinks: Set<String> = emptySet(),
    val relatedViewModels: Set<String> = emptySet(),
    val relatedRepositories: Set<String> = emptySet(),
    val aliases: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
)

data class DiscoveredConnection(
    val fromKey: String,
    val toKey: String,
    val kind: NavigationKind,
    val label: String? = null,
    val sourceFilePath: String? = null,
)

data class AnalyzerOutput(
    val nodes: List<DiscoveredNode> = emptyList(),
    val connections: List<DiscoveredConnection> = emptyList(),
    val startNodeKeys: Set<String> = emptySet(),
) {
    operator fun plus(other: AnalyzerOutput): AnalyzerOutput =
        AnalyzerOutput(
            nodes = nodes + other.nodes,
            connections = connections + other.connections,
            startNodeKeys = startNodeKeys + other.startNodeKeys,
        )
}

