package com.flowlens.export

import com.flowlens.domain.FlowConnection
import com.flowlens.domain.FlowGraph
import com.flowlens.domain.NavigationKind
import com.flowlens.domain.NodeType
import com.flowlens.domain.ScreenNode
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportersTest {
    private val graph = FlowGraph(
        screens = listOf(
            ScreenNode(id = "route:splash", name = "Splash", type = NodeType.COMPOSE_SCREEN),
            ScreenNode(id = "route:login", name = "Login", type = NodeType.COMPOSE_SCREEN),
        ),
        connections = listOf(
            FlowConnection(
                from = "route:splash",
                to = "route:login",
                kind = NavigationKind.COMPOSE,
                label = "navigate",
            ),
        ),
        startNodeIds = setOf("route:splash"),
    )

    @Test
    fun `exports mermaid`() {
        val mermaid = MermaidExporter().export(graph)
        assertTrue(mermaid.contains("graph TD"))
        assertTrue(mermaid.contains("Splash"))
        assertTrue(mermaid.contains("-->"))
    }

    @Test
    fun `exports json`() {
        val json = JsonExporter().export(graph)
        assertTrue(json.contains("\"screens\""))
        assertTrue(json.contains("\"connections\""))
        assertTrue(json.contains("\"Splash\""))
    }
}
