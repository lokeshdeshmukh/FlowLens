package com.flowlens.ui

import com.flowlens.domain.FlowConnection
import com.flowlens.domain.FlowGraph
import com.flowlens.domain.NavigationKind
import com.flowlens.domain.NodeType
import com.flowlens.domain.ScreenNode
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.mxGraphComponent.mxGraphControl
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxEvent
import com.mxgraph.view.mxGraph
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

class FlowGraphCanvas(
    private val onSelectionChanged: (ScreenNode?) -> Unit,
) : JBPanel<FlowGraphCanvas>(BorderLayout()) {
    private val graph = object : mxGraph() {
        override fun isCellEditable(cell: Any?): Boolean = false
        override fun isCellMovable(cell: Any?): Boolean = false
        override fun isCellResizable(cell: Any?): Boolean = false
        override fun isCellDisconnectable(cell: Any?, terminal: Any?, source: Boolean): Boolean = false
        override fun isCellSelectable(cell: Any?): Boolean = cell is mxCell && cell.isVertex
    }

    private var flowGraph: FlowGraph? = null
    private val vertexById = linkedMapOf<String, mxCell>()
    private var selectedNodeId: String? = null

    private val graphComponent = FlowMxGraphComponent(graph) { currentNodes() }

    init {
        border = com.intellij.util.ui.JBUI.Borders.empty()
        background = JBColor.PanelBackground

        graph.isAllowDanglingEdges = false
        graph.isAutoSizeCells = false
        graphComponent.isPanning = true
        graphComponent.viewport.background = JBColor.PanelBackground
        graphComponent.setConnectable(false)
        graphComponent.preferredSize = Dimension(800, 600)
        graphComponent.graphHandler.isEnabled = false
        graphComponent.verticalScrollBarPolicy = javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        graphComponent.horizontalScrollBarPolicy = javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED

        graph.selectionModel.addListener(mxEvent.CHANGE) { _, _ ->
            val selected = graph.selectionCell as? mxCell
            selectedNodeId = selected?.id
            refreshVertexStyles()
            onSelectionChanged(flowGraph?.node(selectedNodeId.orEmpty()))
        }

        add(graphComponent, BorderLayout.CENTER)
    }

    fun setFlowGraph(graphModel: FlowGraph) {
        flowGraph = graphModel
        selectedNodeId = null
        vertexById.clear()

        val parent = graph.defaultParent
        graph.model.beginUpdate()
        try {
            graph.removeCells(graph.getChildCells(parent, true, true))

            graphModel.screens.forEach { node ->
                val width = max(180, node.name.length * 8 + 48).toDouble()
                val cell = graph.insertVertex(
                    parent,
                    node.id,
                    node.name,
                    0.0,
                    0.0,
                    width,
                    64.0,
                    vertexStyle(node, selected = false),
                ) as mxCell
                vertexById[node.id] = cell
            }

            graphModel.connections.forEach { connection ->
                val source = vertexById[connection.from] ?: return@forEach
                val target = vertexById[connection.to] ?: return@forEach
                graph.insertEdge(
                    parent,
                    null,
                    connection.label ?: "",
                    source,
                    target,
                    edgeStyle(connection),
                )
            }
        } finally {
            graph.model.endUpdate()
        }

        applyLayout()
        fitToGraph()
    }

    fun zoomIn() = graphComponent.zoomIn()

    fun zoomOut() = graphComponent.zoomOut()

    fun fitToGraph() {
        graphComponent.zoomActual()
        graphComponent.zoomAndCenter()
    }

    fun autoLayout() {
        applyLayout()
        fitToGraph()
    }

    fun focusScreen(query: String): ScreenNode? {
        val node = flowGraph?.findScreen(query) ?: return null
        selectNode(node.id)
        return node
    }

    fun selectNode(nodeId: String?) {
        selectedNodeId = nodeId
        val cell = nodeId?.let(vertexById::get)
        graph.setSelectionCell(cell)
        refreshVertexStyles()
        nodeId?.let { graphComponent.scrollCellToVisible(vertexById[it], true) }
    }

    fun exportPng(file: File) {
        graphComponent.exportPng(file)
    }

    private fun applyLayout() {
        graph.model.beginUpdate()
        try {
            val layout = mxHierarchicalLayout(graph).apply {
                intraCellSpacing = 40.0
                interRankCellSpacing = 80.0
                interHierarchySpacing = 90.0
                parallelEdgeSpacing = 12.0
            }
            layout.execute(graph.defaultParent)
        } finally {
            graph.model.endUpdate()
        }
        refreshVertexStyles()
    }

    private fun refreshVertexStyles() {
        val current = flowGraph ?: return
        graph.model.beginUpdate()
        try {
            current.screens.forEach { node ->
                vertexById[node.id]?.let { cell ->
                    graph.setCellStyle(
                        vertexStyle(node, selected = node.id == selectedNodeId),
                        arrayOf(cell),
                    )
                }
            }
        } finally {
            graph.model.endUpdate()
        }
        graph.refresh()
    }

    private fun currentNodes(): Map<String, ScreenNode> =
        flowGraph?.screens?.associateBy(ScreenNode::id).orEmpty()

    private fun vertexStyle(node: ScreenNode, selected: Boolean): String {
        val palette = when (node.type) {
            NodeType.ACTIVITY -> "#DBEAFE" to "#2563EB"
            NodeType.FRAGMENT -> "#E0F2FE" to "#0284C7"
            NodeType.COMPOSE_SCREEN -> "#DCFCE7" to "#16A34A"
            NodeType.BOTTOM_SHEET -> "#FEF3C7" to "#D97706"
            NodeType.DIALOG -> "#FCE7F3" to "#DB2777"
            NodeType.DEEP_LINK -> "#EDE9FE" to "#7C3AED"
        }
        val stroke = if (selected) "#111827" else if (node.isStartDestination) "#059669" else palette.second
        val width = if (selected) 3 else if (node.isStartDestination) 2 else 1
        return buildString {
            append("${mxConstants.STYLE_SHAPE}=${mxConstants.SHAPE_RECTANGLE};")
            append("${mxConstants.STYLE_ROUNDED}=1;")
            append("${mxConstants.STYLE_ARCSIZE}=18;")
            append("${mxConstants.STYLE_FILLCOLOR}=${palette.first};")
            append("${mxConstants.STYLE_STROKECOLOR}=$stroke;")
            append("${mxConstants.STYLE_STROKEWIDTH}=$width;")
            append("${mxConstants.STYLE_FONTCOLOR}=#111827;")
            append("${mxConstants.STYLE_FONTSTYLE}=${if (node.isStartDestination) mxConstants.FONT_BOLD else 0};")
            append("${mxConstants.STYLE_FONTSIZE}=12;")
            append("${mxConstants.STYLE_WHITE_SPACE}=wrap;")
            append("${mxConstants.STYLE_SPACING_LEFT}=28;")
            append("${mxConstants.STYLE_SPACING_RIGHT}=8;")
            append("${mxConstants.STYLE_SPACING_TOP}=10;")
            append("${mxConstants.STYLE_SPACING_BOTTOM}=10;")
        }
    }

    private fun edgeStyle(connection: FlowConnection): String {
        val stroke = when (connection.kind) {
            NavigationKind.NAVIGATION_XML -> "#2563EB"
            NavigationKind.NAV_CONTROLLER -> "#0284C7"
            NavigationKind.COMPOSE -> "#16A34A"
            NavigationKind.ACTIVITY_INTENT -> "#DC2626"
            NavigationKind.DEEP_LINK -> "#7C3AED"
            NavigationKind.HEURISTIC -> "#D97706"
        }
        val dashed = if (connection.kind == NavigationKind.HEURISTIC || connection.kind == NavigationKind.DEEP_LINK) 1 else 0
        return buildString {
            append("${mxConstants.STYLE_EDGE}=orthogonalEdgeStyle;")
            append("${mxConstants.STYLE_ROUNDED}=1;")
            append("${mxConstants.STYLE_STROKECOLOR}=$stroke;")
            append("${mxConstants.STYLE_STROKEWIDTH}=1.5;")
            append("${mxConstants.STYLE_DASHED}=$dashed;")
            append("${mxConstants.STYLE_ENDARROW}=${mxConstants.ARROW_BLOCK};")
            append("${mxConstants.STYLE_FONTCOLOR}=#475569;")
            append("${mxConstants.STYLE_LABEL_BACKGROUNDCOLOR}=#FFFFFF;")
        }
    }

    private class FlowMxGraphComponent(
        graph: mxGraph,
        private val nodesProvider: () -> Map<String, ScreenNode>,
    ) : mxGraphComponent(graph) {
        override fun createGraphControl(): mxGraphControl =
            object : mxGraphControl() {
                override fun paintComponent(graphics: Graphics) {
                    super.paintComponent(graphics)
                    val g2 = graphics.create() as Graphics2D
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        paintNodeIcons(g2)
                    } finally {
                        g2.dispose()
                    }
                }
            }

        fun exportPng(file: File) {
            val preferred = graphControl.preferredSize
            val width = max(preferred.width, 1)
            val height = max(preferred.height, 1)
            val previousSize = graphControl.size
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            try {
                g2.color = JBColor.WHITE
                g2.fillRect(0, 0, width, height)
                graphControl.setSize(width, height)
                graphControl.paint(g2)
            } finally {
                g2.dispose()
                graphControl.setSize(previousSize)
            }
            ImageIO.write(image, "png", file)
        }

        private fun paintNodeIcons(g2: Graphics2D) {
            val graph = graph
            val nodes = nodesProvider()
            graph.getChildVertices(graph.defaultParent)
                .filterIsInstance<mxCell>()
                .forEach { cell ->
                val node = nodes[cell.id] ?: return@forEach
                val state = graph.view.getState(cell) ?: return@forEach
                val icon = FlowIcons.forType(node.type)
                val x = state.x.toInt() + 8
                val y = (state.y + ((state.height - icon.iconHeight) / 2.0)).toInt()
                icon.paintIcon(this, g2, x, y)
                if (node.isStartDestination) {
                    g2.color = Color(5, 150, 105)
                    g2.stroke = BasicStroke(1.2f)
                    g2.drawOval(x + icon.iconWidth - 2, y - 2, 8, 8)
                }
            }
        }
    }
}
